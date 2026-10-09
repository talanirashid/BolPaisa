package com.bolpaisa.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.audio.NumberToWordsConverter
import com.bolpaisa.app.data.AppDatabase
import com.bolpaisa.app.data.TransactionEntity
import com.bolpaisa.app.parser.PaymentDetails
import com.bolpaisa.app.parser.PaymentParser
import com.bolpaisa.app.sensor.ShakeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SoundboxService : Service() {

    companion object {
        private const val TAG = "SoundboxService"
        private const val CHANNEL_ID = "BolPaisaServiceChannel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_PROCESS_NOTIFICATION = "com.bolpaisa.app.action.PROCESS_NOTIFICATION"
        const val ACTION_PROCESS_SMS = "com.bolpaisa.app.action.PROCESS_SMS"

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_NOTIFICATION_TEXT = "extra_notification_text"
        const val EXTRA_SMS_SENDER = "extra_sms_sender"
        const val EXTRA_SMS_BODY = "extra_sms_body"
    }

    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var paymentParser: PaymentParser
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null

    private var keepAliveJob: Job? = null
    private var lastPlaybackTime = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        audioPlayerManager = AudioPlayerManager(this)
        paymentParser = PaymentParser()
        database = AppDatabase.getDatabase(this)
        startForegroundService()

        setupShakeDetector()
        startBluetoothKeepAlive()
    }

    private fun setupShakeDetector() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            shakeDetector = ShakeDetector {
                Log.d(TAG, "Shake gesture detected - Replaying last payment announcement")
                replayLastPayment()
            }
            sensorManager?.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun replayLastPayment() {
        serviceScope.launch {
            try {
                val latest = database.transactionDao().getLatestTransaction()
                if (latest != null) {
                    playAnnouncement(PaymentDetails(latest.provider, latest.amount, latest.senderName))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching latest transaction for replay", e)
            }
        }
    }

    private fun startBluetoothKeepAlive() {
        keepAliveJob = serviceScope.launch {
            while (isActive) {
                delay(25_000L) // 25 seconds
                if (System.currentTimeMillis() - lastPlaybackTime >= 25_000L) {
                    playNearSilentPing()
                }
            }
        }
    }

    /**
     * Plays an ultra-short (50ms) near-silent audio pulse to keep Bluetooth DAC/speaker active.
     */
    private fun playNearSilentPing() {
        try {
            val sampleRate = 8000
            val numSamples = sampleRate / 20 // 50 ms
            val buffer = ShortArray(numSamples) // Silent PCM buffer

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            serviceScope.launch {
                delay(100L)
                audioTrack.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Near-silent Bluetooth ping failed: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PROCESS_NOTIFICATION -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val text = intent.getStringExtra(EXTRA_NOTIFICATION_TEXT)
                if (!text.isNullOrEmpty()) {
                    processPaymentText(text, packageName)
                }
            }
            ACTION_PROCESS_SMS -> {
                val sender = intent.getStringExtra(EXTRA_SMS_SENDER)
                val body = intent.getStringExtra(EXTRA_SMS_BODY)
                if (!body.isNullOrEmpty()) {
                    processPaymentText(body, sender)
                }
            }
        }
        return START_STICKY
    }

    private fun processPaymentText(text: String, source: String?) {
        val details = paymentParser.parse(text, source)
        if (details != null) {
            Log.d(TAG, "Valid payment detected: Provider=${details.provider}, Amount=${details.amount}")
            
            serviceScope.launch {
                try {
                    database.transactionDao().insert(
                        TransactionEntity(
                            provider = details.provider,
                            amount = details.amount,
                            senderName = details.senderName
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save transaction to DB", e)
                }
            }

            playAnnouncement(details)
        }
    }

    private fun playAnnouncement(details: PaymentDetails) {
        lastPlaybackTime = System.currentTimeMillis()
        val tokens = mutableListOf<Int>()

        val dingRes = resources.getIdentifier("ding", "raw", packageName)
        if (dingRes != 0) tokens.add(dingRes)

        val providerResName = if (details.provider.equals("JazzCash", true)) "jazzcash_par" else "easypaisa_par"
        val providerRes = resources.getIdentifier(providerResName, "raw", packageName)
        if (providerRes != 0) tokens.add(providerRes)

        tokens.addAll(NumberToWordsConverter.getUrduResIds(this, details.amount))

        val rupayRes = resources.getIdentifier("rupay", "raw", packageName)
        if (rupayRes != 0) tokens.add(rupayRes)

        val wasoolRes = resources.getIdentifier("wasool_huay", "raw", packageName)
        if (wasoolRes != 0) tokens.add(wasoolRes)

        if (tokens.isNotEmpty()) {
            audioPlayerManager.playSequence(tokens)
        }
    }

    private fun startForegroundService() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BolPaisa Soundbox Active Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps BolPaisa running in background to announce payments."
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BolPaisa Active")
            .setContentText("Listening for incoming payments...")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector?.let { sensorManager?.unregisterListener(it) }
        keepAliveJob?.cancel()
        audioPlayerManager.release()
    }
}
