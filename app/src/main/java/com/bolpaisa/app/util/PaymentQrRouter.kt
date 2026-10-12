package com.bolpaisa.app.util

data class PaymentQrResult(
    val rawPayload: String,
    val footerLabel: String,
    val rail: PaymentRail
)

object PaymentQrRouter {

    fun isValidIdentifier(identifier: String): Boolean {
        val cleanId = identifier.trim()
        if (cleanId.isEmpty()) return false
        val isDigits = cleanId.all { it.isDigit() }
        if (isDigits) {
            return (cleanId.length in 5..8) || (cleanId.length == 11)
        }
        return cleanId.length == 24 && cleanId.startsWith("PK", ignoreCase = true)
    }

    /**
     * Resolves and builds a 100% SBP & EMVCo ISO/IEC 18004 compliant payment QR payload via RaastQrEncoder.
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
        val isTillId = cleanId.length in 5..8 && cleanId.all { it.isDigit() }

        val footer = when (rail) {
            PaymentRail.EASYPAISA -> {
                if (isTillId) "Till ID: $cleanId (Easypaisa Merchant)"
                else "Raast ID / Mobile: $cleanId (Instant via Easypaisa & All Banks)"
            }
            PaymentRail.JAZZCASH -> {
                if (isTillId) "Till ID: $cleanId (JazzCash Merchant)"
                else "Raast ID / Mobile: $cleanId (Instant via JazzCash & All Banks)"
            }
            PaymentRail.RAAST -> "Raast Pay: $cleanId (Interoperable Across All Banks)"
        }

        val params = RaastQrParams(
            merchantIbanOrAlias = cleanId,
            merchantName = recipientName,
            merchantCity = city,
            amount = amount,
            gatewayType = rail.code
        )

        val fullPayload = RaastQrEncoder.buildPayload(params)

        return PaymentQrResult(
            rawPayload = fullPayload,
            footerLabel = footer,
            rail = rail
        )
    }
}
