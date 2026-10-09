package com.bolpaisa.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.bolpaisa.app.service.SoundboxService

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: ""
                val body = message.messageBody ?: ""
                Log.d(TAG, "SMS received from $sender: $body")

                if (sender.contains("3737", true) || sender.contains("8558", true) || 
                    sender.contains("Easypaisa", true) || sender.contains("JazzCash", true) ||
                    body.contains("Rs.", true) || body.contains("received", true)) {
                    
                    val serviceIntent = Intent(context, SoundboxService::class.java).apply {
                        action = SoundboxService.ACTION_PROCESS_SMS
                        putExtra(SoundboxService.EXTRA_SMS_SENDER, sender)
                        putExtra(SoundboxService.EXTRA_SMS_BODY, body)
                    }
                    try {
                        context.startService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start SoundboxService from SMS", e)
                    }
                }
            }
        }
    }
}
