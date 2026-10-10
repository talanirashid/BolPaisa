package com.bolpaisa.app.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceAlertEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "BolPaisaVoice"

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var wakeLock: PowerManager.WakeLock? = null

    var onPlaybackStateChangeListener: ((Boolean) -> Unit)? = null

    init {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BolPaisa:VoiceEngineWakeLock"
        )
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TTS engine", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setSpeechRate(0.92f)
            tts?.setPitch(1.0f)
            isTtsReady = true
            Log.i(tag, "Neural TTS engine initialized successfully.")
        } else {
            Log.e(tag, "TTS engine initialization failed with status: $status")
        }
    }

    fun ensureAudibleVolume() {
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val targetVol = (maxVol * 0.75f).toInt()
            if (currentVol < targetVol) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                Log.i(tag, "Boosted STREAM_MUSIC volume from $currentVol to $targetVol")
            }
        } catch (e: Exception) {
            Log.w(tag, "Volume guard warning: ${e.message}")
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (e: Exception) {
            Log.w(tag, "Audio focus request warning: ${e.message}")
        }
    }

    fun speakPaymentAlert(walletName: String, amount: Any, langCode: String = "ur") {
        ensureAudibleVolume()
        requestAudioFocus()
        acquireWakeLock()
        onPlaybackStateChangeListener?.invoke(true)

        val amountStr = amount.toString()
        val targetLocale = when (langCode.lowercase()) {
            "en" -> Locale("en", "PK")
            "sd" -> Locale("sd", "PK")
            else -> Locale("ur", "PK")
        }

        val formattedSentence = when (langCode.lowercase()) {
            "en" -> "$walletName: Payment received. $amountStr Rupees."
            "sd" -> "$walletName تي $amountStr رپيا وصول ٿيا."
            else -> "$walletName پر $amountStr روپے موصول ہوئے۔"
        }

        if (isTtsReady && tts != null) {
            try {
                tts?.language = targetLocale
                tts?.speak(formattedSentence, TextToSpeech.QUEUE_FLUSH, null, "BolPaisaAlert_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e(tag, "TTS speak failed, falling back to default locale", e)
                tts?.language = Locale.ENGLISH
                tts?.speak(formattedSentence, TextToSpeech.QUEUE_FLUSH, null, "BolPaisaAlertFallback")
            }
        } else {
            Log.w(tag, "TTS engine not ready. Playing notification chime fallback.")
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone.play()
            } catch (e: Exception) {
                Log.e(tag, "Ringtone chime fallback failed", e)
            }
        }

        onPlaybackStateChangeListener?.invoke(false)
        releaseWakeLock()
    }

    fun getInstallTtsDataIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(15_000)
            }
        } catch (e: Exception) {
            Log.w(tag, "WakeLock acquisition warning: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "WakeLock release warning: ${e.message}")
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS engine", e)
        }
        releaseWakeLock()
    }
}
