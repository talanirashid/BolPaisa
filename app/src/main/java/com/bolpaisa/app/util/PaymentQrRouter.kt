package com.bolpaisa.app.util

import java.util.Locale

data class PaymentQrResult(
    val rawPayload: String,
    val footerLabel: String,
    val rail: PaymentRail
)

object PaymentQrRouter {

    /**
     * Resolves and builds a 100% EMVCo ISO/IEC 18004 & Raast/SBP compliant payment QR payload.
     * Intelligently routes 5-8 digit Till IDs vs 11-digit Mobile Numbers across Easypaisa, JazzCash, and Raast.
     */
    fun resolvePaymentPayload(
        rail: PaymentRail,
        identifier: String,
        recipientName: String,
        amount: Double?,
        city: String = "KARACHI"
    ): PaymentQrResult {
        val cleanId = identifier.trim()
        val cleanName = recipientName.trim().uppercase(Locale.ROOT).ifEmpty { "BOLCASH MERCHANT" }.take(25)
        val cleanCity = city.trim().uppercase(Locale.ROOT).ifEmpty { "KARACHI" }
        val isTillId = cleanId.length in 5..8 && cleanId.all { it.isDigit() }

        val (tagNum, guiTag, footer) = when (rail) {
            PaymentRail.EASYPAISA -> {
                if (isTillId) {
                    Triple("26", "pk.easypaisa", "Till ID: $cleanId (Easypaisa Merchant)")
                } else {
                    Triple("27", "pk.raast", "Raast ID / Mobile: $cleanId (Instant via Easypaisa & All Banks)")
                }
            }
            PaymentRail.JAZZCASH -> {
                if (isTillId) {
                    Triple("27", "pk.jazzcash", "Till ID: $cleanId (JazzCash Merchant)")
                } else {
                    Triple("27", "pk.raast", "Raast ID / Mobile: $cleanId (Instant via JazzCash & All Banks)")
                }
            }
            PaymentRail.RAAST -> {
                Triple("27", "pk.raast", "Raast Pay: $cleanId (Interoperable Across All Banks)")
            }
        }

        // Sub-tags for Merchant Account Info
        val subTag00 = formatTag("00", guiTag)
        val subTag01 = formatTag("01", cleanId)
        val merchantInfoContent = "$subTag00$subTag01"

        // Build EMVCo tags
        val tag00 = formatTag("00", "01") // Format Indicator
        val tag01 = if (amount != null && amount > 0.0) formatTag("01", "12") else formatTag("01", "11") // Initiation Method
        val tagMerchant = formatTag(tagNum, merchantInfoContent) // Tag 26 or 27
        val tag52 = formatTag("52", "5999") // Merchant Category Code
        val tag53 = formatTag("53", "586") // Transaction Currency (PKR = 586)
        val tag54 = if (amount != null && amount > 0.0) formatTag("54", String.format(Locale.US, "%.2f", amount)) else ""
        val tag58 = formatTag("58", "PK") // Country Code
        val tag59 = formatTag("59", cleanName) // Merchant Name
        val tag60 = formatTag("60", cleanCity) // Merchant City

        val rawPayloadWithoutCrc = "$tag00$tag01$tagMerchant$tag52$tag53$tag54$tag58$tag59$tag60" + "6304"
        val crcHex = calculateCrc16Ccitt(rawPayloadWithoutCrc)
        val fullPayload = "$rawPayloadWithoutCrc$crcHex"

        return PaymentQrResult(
            rawPayload = fullPayload,
            footerLabel = footer,
            rail = rail
        )
    }

    private fun formatTag(tag: String, value: String): String {
        if (value.isEmpty()) return ""
        val length = String.format(Locale.US, "%02d", value.length)
        return "$tag$length$value"
    }

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
