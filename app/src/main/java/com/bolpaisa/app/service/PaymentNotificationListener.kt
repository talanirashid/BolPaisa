package com.bolpaisa.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.bolpaisa.app.audio.VoiceAlertEngine
import com.bolpaisa.app.data.AppDatabase
import com.bolpaisa.app.data.PakistaniPaymentParser
import com.bolpaisa.app.data.PaymentRepository
import com.bolpaisa.app.data.TransactionEntity
import com.bolpaisa.app.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var voiceAlertEngine: VoiceAlertEngine

    override fun onCreate() {
        super.onCreate()
        voiceAlertEngine = VoiceAlertEngine(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        val payment = PakistaniPaymentParser.parse(title, text) ?: return

        scope.launch(Dispatchers.IO) {
            // 1. Save to Room database
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.transactionDao().insert(
                    TransactionEntity(
                        provider = payment.rail.displayName,
                        amount = payment.amount.toLong(),
                        senderName = payment.sender ?: "Customer",
                        timestamp = payment.timestamp
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Emit to reactive UI flow
            PaymentRepository.emitPayment(payment)

            // 3. Trigger voice alert announcement immediately
            val voiceLang = LocaleHelper.getVoiceLanguage(applicationContext)
            voiceAlertEngine.speakPaymentAlert(
                walletName = payment.rail.displayName,
                amount = payment.amount,
                langCode = voiceLang
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceAlertEngine.release()
    }
}
