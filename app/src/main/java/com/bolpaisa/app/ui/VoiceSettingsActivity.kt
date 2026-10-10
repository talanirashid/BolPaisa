package com.bolpaisa.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.reports.PdfReportGenerator
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.LocaleHelper

class VoiceSettingsActivity : BaseActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager

    companion object {
        const val PREFS_NAME = "secure_voice_prefs"
        const val KEY_VOICE_LANGUAGE = "key_voice_language" // "URDU", "ENGLISH", "SINDHI"
        const val KEY_BT_KEEP_ALIVE = "key_bt_keep_alive"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_settings)

        audioPlayerManager = AudioPlayerManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rgLanguage = findViewById<RadioGroup>(R.id.rgLanguage)
        val rbUrdu = findViewById<RadioButton>(R.id.rbUrdu)
        val rbEnglish = findViewById<RadioButton>(R.id.rbEnglish)
        val rbSindhi = findViewById<RadioButton>(R.id.rbSindhi)
        val btnTestAlert = findViewById<Button>(R.id.btnTestAlert)
        val btnPlayDemoAudio = findViewById<Button>(R.id.btnPlayDemoAudio)
        val btnPreviewDemoPdf = findViewById<Button>(R.id.btnPreviewDemoPdf)
        val switchBtKeepAlive = findViewById<Switch>(R.id.switchBtKeepAlive)
        val btnPairBluetooth = findViewById<Button>(R.id.btnPairBluetooth)

        btnBack.setOnClickListener { finish() }

        val activeVoiceLang = LocaleHelper.getVoiceLanguage(this)
        when (activeVoiceLang) {
            "en" -> rbEnglish.isChecked = true
            "sd" -> rbSindhi.isChecked = true
            else -> rbUrdu.isChecked = true
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        switchBtKeepAlive.isChecked = prefs.getBoolean(KEY_BT_KEEP_ALIVE, true)

        rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val (langCode, langName, langLabel) = when (checkedId) {
                R.id.rbEnglish -> Triple("en", "ENGLISH", "English")
                R.id.rbSindhi -> Triple("sd", "SINDHI", "Sindhi")
                else -> Triple("ur", "URDU", "Urdu")
            }

            if (LocaleHelper.getVoiceLanguage(this) != langCode) {
                LocaleHelper.setVoiceLanguage(this, langCode)
                prefs.edit().putString(KEY_VOICE_LANGUAGE, langName).apply()
                FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Voice alert language set to $langLabel")
            }
        }

        switchBtKeepAlive.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BT_KEEP_ALIVE, isChecked).apply()
            val status = if (isChecked) "enabled" else "disabled"
            FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Speaker keep-alive boost $status")
        }

        audioPlayerManager.onPlaybackStateChangeListener = { isPlaying ->
            runOnUiThread {
                if (isPlaying) {
                    btnTestAlert.text = "Playing Announcement..."
                    btnTestAlert.isEnabled = false
                } else {
                    btnTestAlert.text = "Play Sample Voice Alert"
                    btnTestAlert.isEnabled = true
                }
            }
        }

        btnTestAlert.setOnClickListener {
            playSampleAlert()
        }

        btnPlayDemoAudio?.setOnClickListener {
            playSampleAlert()
        }

        btnPreviewDemoPdf?.setOnClickListener {
            PdfReportGenerator.generateDemoReport(this)
        }

        btnPairBluetooth.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            } catch (e: Exception) {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Cannot open Bluetooth settings")
            }
        }

        updateBluetoothStatus()
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothStatus()
    }

    private fun updateBluetoothStatus() {
        val tvBtConnectionStatus = findViewById<TextView>(R.id.tvBtConnectionStatus) ?: return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var connectedBtName: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in outputs) {
                if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        device.productName?.toString()
                    } else null
                    connectedBtName = name ?: "Bluetooth Counter Speaker"
                    break
                }
            }
        }

        if (connectedBtName != null) {
            tvBtConnectionStatus.text = "● Connected: $connectedBtName"
            tvBtConnectionStatus.setTextColor(Color.parseColor("#10B981"))
        } else {
            tvBtConnectionStatus.text = "● Phone Speaker (No Bluetooth Connected)"
            tvBtConnectionStatus.setTextColor(Color.parseColor("#94A3B8"))
        }
    }

    private fun playSampleAlert() {
        val voiceLang = LocaleHelper.getVoiceLanguage(this)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (currentVol == 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.75f).toInt(), AudioManager.FLAG_SHOW_UI)
            }
        } catch (e: Exception) {
            android.util.Log.w("BolPaisaAudio", "Volume check warning: ${e.message}")
        }

        FeedbackHelper.showInfo(findViewById(android.R.id.content), "Playing sample payment announcement...")
        audioPlayerManager.playSampleAlert(voiceLang)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
