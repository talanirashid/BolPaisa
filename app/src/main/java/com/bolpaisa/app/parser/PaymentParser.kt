package com.bolpaisa.app.parser

data class PaymentDetails(
    val provider: String, // "Easypaisa" or "JazzCash"
    val amount: Long,
    val senderName: String? = null,
    val transactionId: String? = null
)

class PaymentParser {

    companion object {
        // Easypaisa SMS & Notification Regex patterns
        private val EASYPAISA_REGEX = Regex(
            "(?:Rs\\.?\\s*([0-9,]+\\.?[0-9]*)|([0-9,]+\\.?[0-9*]*)\\s*Rs).*?(?:received from|transfer from|by)\\s+([a-zA-Z\\s]+)",
            RegexOption.IGNORE_CASE
        )

        // JazzCash SMS & Notification Regex patterns
        private val JAZZCASH_REGEX = Regex(
            "(?:Rs\\.?\\s*([0-9,]+\\.?[0-9]*)|([0-9,]+\\.?[0-9*]*)\\s*Rs).*?(?:from|by)\\s+([a-zA-Z\\s]+)",
            RegexOption.IGNORE_CASE
        )

        private val DEBIT_KEYWORDS = listOf("sent", "paid", "deducted", "transferred to", "debited", "bhej diye")
    }

    fun parse(text: String, packageName: String? = null): PaymentDetails? {
        val lowerText = text.lowercase()

        // Check for debit exclusions (we only want credit/incoming payments)
        for (keyword in DEBIT_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return null
            }
        }

        // Determine provider based on package name or text content
        val isEasypaisa = packageName?.contains("easypaisa", true) == true || lowerText.contains("easypaisa") || text.contains("3737")
        val isJazzCash = packageName?.contains("jazzcash", true) == true || lowerText.contains("jazzcash") || text.contains("8558")

        if (isEasypaisa) {
            val match = EASYPAISA_REGEX.find(text)
            if (match != null) {
                val amountStr = (match.groupValues[1].ifEmpty { match.groupValues[2] }).replace(",", "")
                val amount = amountStr.toDoubleOrNull()?.toLong() ?: return null
                val sender = match.groupValues.getOrNull(3)?.trim()
                return PaymentDetails("Easypaisa", amount, sender)
            }
        } else if (isJazzCash) {
            val match = JAZZCASH_REGEX.find(text)
            if (match != null) {
                val amountStr = (match.groupValues[1].ifEmpty { match.groupValues[2] }).replace(",", "")
                val amount = amountStr.toDoubleOrNull()?.toLong() ?: return null
                val sender = match.groupValues.getOrNull(3)?.trim()
                return PaymentDetails("JazzCash", amount, sender)
            }
        }

        // Generic fallback parser for numeric amount extraction
        val genericRegex = Regex("(?:Rs\\.?\\s*([0-9,]+(?:\\.[0-9]+)?))", RegexOption.IGNORE_CASE)
        val genericMatch = genericRegex.find(text)
        if (genericMatch != null) {
            val amountStr = genericMatch.groupValues[1].replace(",", "")
            val amount = amountStr.toDoubleOrNull()?.toLong() ?: return null
            val provider = if (isJazzCash) "JazzCash" else "Easypaisa"
            return PaymentDetails(provider, amount)
        }

        return null
    }
}
