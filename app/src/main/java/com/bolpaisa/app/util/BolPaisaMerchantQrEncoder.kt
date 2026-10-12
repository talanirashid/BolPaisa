package com.bolpaisa.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

data class MerchantTillProfile(
    val merchantName: String,             // e.g. "Talani General Store"
    val merchantCity: String = "KARACHI",
    val raastIban: String,                // 24-character IBAN or mobile account (e.g. "PK73TMBP0000000312365636")
    val easypaisaTillId: String? = null,  // 6-8 digit official Till Number
    val jazzcashTillId: String? = null,   // 6-8 digit official Till Number
    val dynamicAmount: Double? = null,
    val billNumber: String? = null
)

object BolPaisaMerchantQrEncoder {

    private fun tlv(tag: String, value: String): String {
        if (value.isEmpty()) return ""
        val byteCount = value.toByteArray(Charsets.UTF_8).size
        val len = String.format(Locale.US, "%02d", byteCount)
        return "$tag$len$value"
    }

    /**
     * Constructs a hybrid multi-rail EMVCo payload string containing:
     * - Tag 26: Raast Universal Interoperable Network (pk.raast + IBAN)
     * - Tag 27: Easypaisa Official Merchant Till Rail (pk.easypaisa + Till ID)
     * - Tag 28: JazzCash Official Merchant Till Rail (pk.jazzcash + Till ID)
     */
    fun generateHybridPayload(profile: MerchantTillProfile): String {
        val sb = StringBuilder()

        // 00: Format Indicator
        sb.append(tlv("00", "01"))

        // 01: Initiation (11 = Static, 12 = Dynamic)
        val isDynamic = profile.dynamicAmount != null && profile.dynamicAmount > 0
        sb.append(tlv("01", if (isDynamic) "12" else "11"))

        // 26: Raast Interoperable Network (Mandatory fallback)
        val raastSub00 = tlv("00", "pk.raast")
        val raastSub01 = tlv("01", profile.raastIban.trim().uppercase(Locale.US))
        sb.append(tlv("26", raastSub00 + raastSub01))

        // 27: Easypaisa Official Merchant Till Rail
        if (!profile.easypaisaTillId.isNullOrBlank()) {
            val epSub00 = tlv("00", "pk.easypaisa")
            val epSub01 = tlv("01", profile.easypaisaTillId.trim())
            sb.append(tlv("27", epSub00 + epSub01))
        }

        // 28: JazzCash Official Merchant Till Rail
        if (!profile.jazzcashTillId.isNullOrBlank()) {
            val jcSub00 = tlv("00", "pk.jazzcash")
            val jcSub01 = tlv("01", profile.jazzcashTillId.trim())
            sb.append(tlv("28", jcSub00 + jcSub01))
        }

        // 52: Merchant Category Code
        sb.append(tlv("52", "5411"))

        // 53: Transaction Currency (586 = PKR)
        sb.append(tlv("53", "586"))

        // 54: Transaction Amount
        if (isDynamic) {
            sb.append(tlv("54", String.format(Locale.US, "%.2f", profile.dynamicAmount)))
        }

        // 58: Country Code
        sb.append(tlv("58", "PK"))

        // 59: Merchant Name
        val cleanName = profile.merchantName.filter { it.isLetterOrDigit() || it.isWhitespace() }.take(25)
        sb.append(tlv("59", cleanName.ifBlank { "MERCHANT" }))

        // 60: City
        val cleanCity = profile.merchantCity.filter { it.isLetterOrDigit() || it.isWhitespace() }.take(15)
        sb.append(tlv("60", cleanCity.ifBlank { "KARACHI" }))

        // 62: Bill / Reference (Optional)
        if (!profile.billNumber.isNullOrBlank()) {
            sb.append(tlv("62", tlv("01", profile.billNumber.take(25))))
        }

        // 63: CRC-16 Checksum
        val payloadWithCrcTag = sb.toString() + "6304"
        val crc = calculateCrc16(payloadWithCrcTag)

        return "$payloadWithCrcTag$crc"
    }

    private fun calculateCrc16(data: String): String {
        var crc = 0xFFFF
        val bytes = data.toByteArray(Charsets.UTF_8)
        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = ((b.toInt() shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor 0x1021
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.US, "%04X", crc)
    }

    fun render(payload: String, sizePx: Int = 600): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
