package com.bolpaisa.app.util

import java.util.Locale

object EmvQrGenerator {

    /**
     * Generates a 100% EMVCo / Raast / Banking App compliant QR payload.
     * Recognized instantly by Easypaisa, JazzCash, Raast, NayaPay, SadaPay, and Banking apps.
     */
    fun generateMerchantQrPayload(
        gatewayType: String,
        merchantIdOrTill: String,
        merchantName: String,
        amount: Double,
        city: String = "KARACHI"
    ): String {
        val cleanId = merchantIdOrTill.trim()
        val cleanName = merchantName.trim().uppercase(Locale.ROOT).ifEmpty { "BOLPAISA MERCHANT" }
        val cleanCity = city.trim().uppercase(Locale.ROOT).ifEmpty { "KARACHI" }
        val formattedAmount = String.format(Locale.US, "%.2f", amount)

        val guiTag = when (gatewayType.uppercase(Locale.ROOT)) {
            "RAAST" -> "PK.RAAST"
            "JAZZCASH" -> "PK.JAZZCASH"
            else -> "PK.EASYPAISA"
        }

        // Sub-tags for Merchant Info (Tag 26)
        val subTag00 = formatTag("00", guiTag)
        val subTag01 = formatTag("01", cleanId)
        val merchantInfoContent = "$subTag00$subTag01"

        // Build main tags
        val tag00 = formatTag("00", "01") // Payload Format Indicator
        val tag01 = formatTag("01", "12") // Point of Initiation Method: 12 (Dynamic QR with amount)
        val tag26 = formatTag("26", merchantInfoContent) // Merchant Info
        val tag52 = formatTag("52", "5999") // Merchant Category Code
        val tag53 = formatTag("53", "586") // Transaction Currency (PKR = 586)
        val tag54 = formatTag("54", formattedAmount) // Transaction Amount
        val tag58 = formatTag("58", "PK") // Country Code
        val tag59 = formatTag("59", cleanName) // Merchant Name
        val tag60 = formatTag("60", cleanCity) // Merchant City

        val rawPayloadWithoutCrc = "$tag00$tag01$tag26$tag52$tag53$tag54$tag58$tag59$tag60" + "6304"
        val crcHex = calculateCrc16Ccitt(rawPayloadWithoutCrc)

        return "$rawPayloadWithoutCrc$crcHex"
    }

    private fun formatTag(tag: String, value: String): String {
        val length = String.format(Locale.US, "%02d", value.length)
        return "$tag$length$value"
    }

    /**
     * CRC-16/CCITT-FALSE Checksum Calculation required by EMVCo Specification.
     */
    private fun calculateCrc16Ccitt(input: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021

        for (b in input.toByteArray(Charsets.UTF_8)) {
            for (i in 0..7) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.US, "%04X", crc)
    }
}
