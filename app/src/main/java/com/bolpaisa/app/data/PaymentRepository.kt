package com.bolpaisa.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PaymentRepository {
    private val _incomingPayments = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 10)
    val incomingPayments = _incomingPayments.asSharedFlow()

    fun emitPayment(event: PaymentEvent) {
        _incomingPayments.tryEmit(event)
    }
}
