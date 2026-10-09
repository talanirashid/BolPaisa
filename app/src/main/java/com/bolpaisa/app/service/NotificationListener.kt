package com.bolpaisa.app.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val fullText = "$title $text"
        Log.d(TAG, "Notification received from $packageName: $fullText")

        // Forward to SoundboxService
        val intent = Intent(this, SoundboxService::class.java).apply {
            action = SoundboxService.ACTION_PROCESS_NOTIFICATION
            putExtra(SoundboxService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(SoundboxService.EXTRA_NOTIFICATION_TEXT, fullText)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SoundboxService from notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
