package com.bolpaisa.app.data

import com.bolpaisa.app.util.PaymentRail
import java.util.Calendar

data class DailySummary(
    val totalAmount: Double,
    val transactionCount: Int,
    val easypaisaTotal: Double,
    val jazzcashTotal: Double,
    val raastTotal: Double
)

object DailySettlementManager {

    private val transactions = mutableListOf<PaymentEvent>()

    fun recordPayment(event: PaymentEvent) {
        transactions.add(event)
    }

    fun getTodaySummary(): DailySummary {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        val todayTx = transactions.filter { it.timestamp >= startOfDay }

        val total = todayTx.sumOf { it.amount }
        val epTotal = todayTx.filter { it.rail == PaymentRail.EASYPAISA }.sumOf { it.amount }
        val jcTotal = todayTx.filter { it.rail == PaymentRail.JAZZCASH }.sumOf { it.amount }
        val raastTotal = todayTx.filter { it.rail == PaymentRail.RAAST }.sumOf { it.amount }

        return DailySummary(
            totalAmount = total,
            transactionCount = todayTx.size,
            easypaisaTotal = epTotal,
            jazzcashTotal = jcTotal,
            raastTotal = raastTotal
        )
    }

    fun getLastPayment(): PaymentEvent? = transactions.lastOrNull()
}
