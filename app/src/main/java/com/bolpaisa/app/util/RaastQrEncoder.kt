package com.bolpaisa.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

data class RaastQrParams(
    val merchantIbanOrAlias: String,     // e.g. "PK70SCBL0000001123456701", Till ID, or mobile alias
    val merchantName: String,            // Store Name
    val merchantCity: String = "KARACHI",// City Name
    val amount: Double? = null,          // If provided > 0, generates a Dynamic QR (tag 01 = "12")
    val mcc: String = "5999",            // ISO 18245 Merchant Category Code
    val billNumber: String? = null,      // Optional order/invoice reference
    val gatewayType: String = "EASYPAISA"
)

object RaastQrEncoder {

    /**
     * Formats a raw key-value string into EMV TLV format: ID (2 digits) + LEN (2 digits) + VAL
     */
    fun tlv(tag: String, value: String): String {
        if (value.isEmpty()) return ""
        val length = String.format(Locale.US, "%02d", value.toByteArray(Charsets.UTF_8).size)
        return "$tag$length$value"
    }

    /**
     * Constructs the full EMVCo & SBP Raast compliant payload string.
     */
    fun buildPayload(params: RaastQrParams): String {
        val sb = StringBuilder()

        // 00: Payload Format Indicator
        sb.append(tlv("00", "01"))

        // 01: Point of Initiation Method (11: Static, 12: Dynamic)
        val isDynamic = params.amount != null && params.amount > 0
        sb.append(tlv("01", if (isDynamic) "12" else "11"))

        // 26: Merchant Account Information (Raast / Gateway Scheme)
        val aid = when (params.gatewayType.uppercase(Locale.ROOT)) {
            "JAZZCASH" -> "PK.JAZZCASH"
            "RAAST" -> "PK.RAAST"
            else -> "PK.EASYPAISA"
        }
        val raastSubPayload = StringBuilder()
            .append(tlv("00", aid))
            .append(tlv("01", params.merchantIbanOrAlias.trim()))
            .toString()
        sb.append(tlv("26", raastSubPayload))

        // 52: Merchant Category Code
        sb.append(tlv("52", params.mcc.take(4).padStart(4, '0')))

        // 53: Transaction Currency (586 = PKR)
        sb.append(tlv("53", "586"))

        // 54: Transaction Amount (dynamic only)
        if (isDynamic) {
            val formattedAmount = String.format(Locale.US, "%.2f", params.amount)
            sb.append(tlv("54", formattedAmount))
        }

        // 58: Country Code
        sb.append(tlv("58", "PK"))

        // 59: Merchant Name (Up to 25 chars)
        sb.append(tlv("59", params.merchantName.trim().uppercase(Locale.ROOT).take(25)))

        // 60: Merchant City (Up to 15 chars)
        sb.append(tlv("60", params.merchantCity.trim().uppercase(Locale.ROOT).take(15)))

        // 62: Additional Data (Optional bill/invoice ref)
        if (!params.billNumber.isNullOrBlank()) {
            val addDataSub = tlv("01", params.billNumber.take(25))
            sb.append(tlv("62", addDataSub))
        }

        // Append 6304 header for CRC calculation
        val payloadWithoutCrc = sb.toString() + "6304"
        val crc = calculateCrc16Ccitt(payloadWithoutCrc)

        return "$payloadWithoutCrc$crc"
    }

    /**
     * CRC-16/CCITT-FALSE (Polynomial: 0x1021, Initial: 0xFFFF, No reflection, No final XOR)
     */
    fun calculateCrc16Ccitt(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = data.toByteArray(Charsets.UTF_8)

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = ((b.toInt() shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.US, "%04X", crc)
    }

    /**
     * Renders payload string to a clean, high-contrast QR Bitmap.
     */
    fun renderQrBitmap(content: String, sizePx: Int = 600): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)

        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
