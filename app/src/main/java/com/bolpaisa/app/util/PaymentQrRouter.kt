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
     * Resolves and builds a hybrid multi-rail EMVCo payload containing Tag 26 (Raast), Tag 27 (Easypaisa Till), Tag 28 (JazzCash Till).
     */
    fun resolveHybridPayload(
        profile: MerchantTillProfile,
        activeRail: PaymentRail
    ): PaymentQrResult {
        val fullPayload = BolPaisaMerchantQrEncoder.generateHybridPayload(profile)

        val footer = when (activeRail) {
            PaymentRail.EASYPAISA -> {
                val epTill = profile.easypaisaTillId
                if (!epTill.isNullOrBlank()) "Till ID: $epTill (Easypaisa Merchant)"
                else "Raast ID: ${profile.raastIban} (Instant via Easypaisa & All Banks)"
            }
            PaymentRail.JAZZCASH -> {
                val jcTill = profile.jazzcashTillId
                if (!jcTill.isNullOrBlank()) "Till ID: $jcTill (JazzCash Merchant)"
                else "Raast ID: ${profile.raastIban} (Instant via JazzCash & All Banks)"
            }
            PaymentRail.RAAST -> "Raast IBAN: ${profile.raastIban} (Interoperable Across All Banks)"
        }

        return PaymentQrResult(
            rawPayload = fullPayload,
            footerLabel = footer,
            rail = activeRail
        )
    }

    fun resolvePaymentPayload(
        rail: PaymentRail,
        identifier: String,
        recipientName: String,
        amount: Double?,
        city: String = "KARACHI"
    ): PaymentQrResult {
        val cleanId = identifier.trim()
        val epTill = if (rail == PaymentRail.EASYPAISA && cleanId.length in 5..8) cleanId else null
        val jcTill = if (rail == PaymentRail.JAZZCASH && cleanId.length in 5..8) cleanId else null

        val profile = MerchantTillProfile(
            merchantName = recipientName,
            merchantCity = city,
            raastIban = cleanId,
            easypaisaTillId = epTill,
            jazzcashTillId = jcTill,
            dynamicAmount = amount
        )

        return resolveHybridPayload(profile, rail)
    }
}
