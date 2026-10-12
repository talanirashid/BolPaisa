package com.bolpaisa.app.data

import com.bolpaisa.app.util.PaymentRail
import java.util.regex.Pattern

object PakistaniPaymentParser {
    // Matches amounts: Rs. 500, Rs 1,500.00, PKR 2500, etc.
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:Rs\.?|PKR|received)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(title: String?, text: String?): PaymentEvent? {
        val content = "${title.orEmpty()} ${text.orEmpty()}".lowercase()

        // Filter: Must be an incoming credit/received message
        val isCredit = content.contains("received") || 
                       content.contains("credited") || 
                       content.contains("wasool") || 
                       content.contains("mosool") ||
                       content.contains("has received")

        // Exclude outgoing debits
        val isDebit = content.contains("sent") ||
                      content.contains("paid") ||
                      content.contains("debited") ||
                      content.contains("deducted") ||
                      content.contains("bhej diye")

        if (!isCredit || isDebit) return null

        val matcher = AMOUNT_PATTERN.matcher("${title.orEmpty()} ${text.orEmpty()}")
        if (!matcher.find()) return null

        val amountCleaned = matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        if (amountCleaned <= 0.0) return null

        val rail = when {
            content.contains("raast") -> PaymentRail.RAAST
            content.contains("easypaisa") || content.contains("3737") -> PaymentRail.EASYPAISA
            content.contains("jazzcash") || content.contains("8558") -> PaymentRail.JAZZCASH
            else -> PaymentRail.EASYPAISA
        }

        return PaymentEvent(
            amount = amountCleaned,
            rail = rail,
            sender = title ?: "Customer"
        )
    }
}
