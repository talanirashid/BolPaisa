package com.bolpaisa.app.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.RawRes
import com.bolpaisa.app.util.LocaleHelper
import java.util.LinkedList
import java.util.Locale
import java.util.Queue

class AudioPlayerManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "BolPaisaAudio"

    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null

    private val sequenceQueue: Queue<Int> = LinkedList()
    private val paymentJobsQueue: Queue<List<Int>> = LinkedList()

    private var isPlaying = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val voiceAlertEngine = VoiceAlertEngine(context)

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    var onPlaybackStateChangeListener: ((Boolean) -> Unit)? = null

    init {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BolPaisa:AudioPlaybackWakeLock"
        )
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TTS engine: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ur", "PK"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
            isTtsReady = true
        }
    }

    fun ensureAudibleVolume() {
        voiceAlertEngine.ensureAudibleVolume()
    }

    fun playSampleAlert(langCode: String) {
        val activeLang = if (langCode.isNotEmpty()) langCode else LocaleHelper.getVoiceLanguage(context)
        voiceAlertEngine.speakPaymentAlert("Easypaisa", 150, activeLang)
    }

    @Synchronized
    fun playSequence(rawResIds: List<Int>, fallbackText: String = "Rs. 150 received via Easypaisa") {
        ensureAudibleVolume()

        if (rawResIds.isEmpty()) {
            playFallbackSpeech(fallbackText)
            return
        }

        paymentJobsQueue.offer(rawResIds)

        if (!isPlaying) {
            processNextJob(fallbackText)
        }
    }

    @Synchronized
    private fun processNextJob(fallbackText: String = "Payment received") {
        val nextJob = paymentJobsQueue.poll()
        if (nextJob == null) {
            isPlaying = false
            releaseWakeLock()
            notifyState(false)
            return
        }

        isPlaying = true
        notifyState(true)
        acquireWakeLock()

        sequenceQueue.clear()
        sequenceQueue.addAll(nextJob)

        startPlaybackPipeline(fallbackText)
    }

    private fun notifyState(playing: Boolean) {
        onPlaybackStateChangeListener?.invoke(playing)
    }

    private fun startPlaybackPipeline(fallbackText: String) {
        val firstResId = sequenceQueue.poll()
        if (firstResId == null) {
            processNextJob(fallbackText)
            return
        }

        currentPlayer = createConfiguredPlayer(firstResId)
        if (currentPlayer == null) {
            Log.w(tag, "Raw audio res $firstResId missing. Falling back to Neural TTS speech.")
            voiceAlertEngine.speakPaymentAlert("Easypaisa", 150, LocaleHelper.getVoiceLanguage(context))
            processNextJob(fallbackText)
            return
        }

        prepareNextInSequence()

        currentPlayer?.setOnCompletionListener { mp ->
            mp.release()
            currentPlayer = nextPlayer
            nextPlayer = null

            if (currentPlayer != null) {
                attachCompletionHandler(currentPlayer, fallbackText)
                prepareNextInSequence()
            } else {
                processNextJob(fallbackText)
            }
        }

        currentPlayer?.start()
    }

    private fun prepareNextInSequence() {
        val nextResId = sequenceQueue.poll() ?: return

        nextPlayer = createConfiguredPlayer(nextResId)
        if (nextPlayer != null && currentPlayer != null) {
            try {
                currentPlayer?.setNextMediaPlayer(nextPlayer)
            } catch (e: Exception) {
                Log.w(tag, "Gapless chaining failed: ${e.message}")
            }
        }
    }

    private fun attachCompletionHandler(player: MediaPlayer?, fallbackText: String) {
        player?.setOnCompletionListener { mp ->
            mp.release()
            currentPlayer = nextPlayer
            nextPlayer = null

            if (currentPlayer != null) {
                attachCompletionHandler(currentPlayer, fallbackText)
                prepareNextInSequence()
            } else {
                processNextJob(fallbackText)
            }
        }
    }

    private fun createConfiguredPlayer(@RawRes rawResId: Int): MediaPlayer? {
        if (rawResId == 0) return null
        val player = MediaPlayer()
        return try {
            val afd: AssetFileDescriptor = context.resources.openRawResourceFd(rawResId) ?: return null

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.prepare()
            player
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize player for res ID: $rawResId", e)
            player.release()
            null
        }
    }

    fun playFallbackSpeech(text: String) {
        voiceAlertEngine.speakPaymentAlert("Easypaisa", text, LocaleHelper.getVoiceLanguage(context))
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

    @Synchronized
    fun release() {
        sequenceQueue.clear()
        paymentJobsQueue.clear()

        try {
            currentPlayer?.stop()
            currentPlayer?.release()
            currentPlayer = null

            nextPlayer?.stop()
            nextPlayer?.release()
            nextPlayer = null

            tts?.stop()
            tts?.shutdown()
            voiceAlertEngine.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaPlayers", e)
        }

        isPlaying = false
        releaseWakeLock()
        notifyState(false)
    }
}
