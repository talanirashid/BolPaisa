package com.bolpaisa.app.util

enum class PaymentRail(val code: String, val displayName: String, val defaultGui: String, val brandColorHex: String) {
    EASYPAISA("EASYPAISA", "Easypaisa", "PK.EASYPAISA", "#10B981"),
    JAZZCASH("JAZZCASH", "JazzCash", "PK.JAZZCASH", "#EF4444"),
    RAAST("RAAST", "Raast (SBP)", "PK.RAAST", "#38BDF8");

    companion object {
        fun fromCode(code: String): PaymentRail {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: EASYPAISA
        }
    }
}
