package com.bolpaisa.app.data

import com.bolpaisa.app.util.PaymentRail

data class PaymentEvent(
    val amount: Double,
    val rail: PaymentRail,
    val sender: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
