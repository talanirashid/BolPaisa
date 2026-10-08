package com.bolpaisa.app.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RawRes
import java.util.LinkedList
import java.util.Queue

class AudioPlayerManager(private val context: Context) {

    private val tag = "AudioPlayerManager"

    // Primary and secondary players for the ping-pong pipeline
    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null

    // Track state of current sequence
    private val sequenceQueue: Queue<Int> = LinkedList()
    private val paymentJobsQueue: Queue<List<Int>> = LinkedList()

    private var isPlaying = false
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BolPaisa:AudioPlaybackWakeLock"
        )
    }

    /**
     * Enqueues an entire sequence of resource IDs (e.g. [ding, easypaisa, char, sau, rupay])
     * and begins playback if the engine is currently idle.
     */
    @Synchronized
    fun playSequence(rawResIds: List<Int>) {
        if (rawResIds.isEmpty()) return

        paymentJobsQueue.offer(rawResIds)

        if (!isPlaying) {
            processNextJob()
        }
    }

    @Synchronized
    private fun processNextJob() {
        val nextJob = paymentJobsQueue.poll()
        if (nextJob == null) {
            isPlaying = false
            releaseWakeLock()
            return
        }

        isPlaying = true
        acquireWakeLock()

        sequenceQueue.clear()
        sequenceQueue.addAll(nextJob)

        startPlaybackPipeline()
    }

    /**
     * Bootstraps the primary and secondary players for gapless chaining.
     */
    private fun startPlaybackPipeline() {
        val firstResId = sequenceQueue.poll() ?: run {
            processNextJob()
            return
        }

        currentPlayer = createConfiguredPlayer(firstResId) ?: run {
            processNextJob()
            return
        }

        // Prepare the secondary player if more clips exist in this sequence
        prepareNextInSequence()

        currentPlayer?.setOnCompletionListener { mp ->
            mp.release()
            currentPlayer = nextPlayer
            nextPlayer = null

            if (currentPlayer != null) {
                // Secondary player automatically transitioned via setNextMediaPlayer;
                // Hook completion to continue the chain
                attachCompletionHandler(currentPlayer)
                prepareNextInSequence()
            } else {
                // Current payment sequence finished
                processNextJob()
            }
        }

        currentPlayer?.start()
    }

    private fun prepareNextInSequence() {
        val nextResId = sequenceQueue.poll() ?: return

        nextPlayer = createConfiguredPlayer(nextResId)
        if (nextPlayer != null && currentPlayer != null) {
            try {
                // Hardware-level gapless transition
                currentPlayer?.setNextMediaPlayer(nextPlayer)
            } catch (e: Exception) {
                Log.w(tag, "Gapless chaining failed, falling back to sequential listener: ${e.message}")
            }
        }
    }

    private fun attachCompletionHandler(player: MediaPlayer?) {
        player?.setOnCompletionListener { mp ->
            mp.release()
            currentPlayer = nextPlayer
            nextPlayer = null

            if (currentPlayer != null) {
                attachCompletionHandler(currentPlayer)
                prepareNextInSequence()
            } else {
                processNextJob()
            }
        }
    }

    /**
     * Instantiates and configures a MediaPlayer pointing directly to an uncompressed/Ogg raw resource.
     */
    private fun createConfiguredPlayer(@RawRes rawResId: Int): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            val afd: AssetFileDescriptor = context.resources.openRawResourceFd(rawResId) ?: return null

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION) // Cut through background noise
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

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(15_000) // 15-second safety limit per payment announcement
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

    /**
     * Emergency reset: stops all audio, clears queued payments, and frees native audio memory.
     */
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
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaPlayers", e)
        }

        isPlaying = false
        releaseWakeLock()
    }
}
