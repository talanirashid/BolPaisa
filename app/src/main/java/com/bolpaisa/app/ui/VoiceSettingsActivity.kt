package com.bolpaisa.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.audio.NumberToWordsConverter
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.LocaleHelper

class VoiceSettingsActivity : AppCompatActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager

    companion object {
        const val PREFS_NAME = "secure_voice_prefs"
        const val KEY_VOICE_LANGUAGE = "key_voice_language" // "URDU", "ENGLISH", "SINDHI"
        const val KEY_BT_KEEP_ALIVE = "key_bt_keep_alive"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)))
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
        val switchBtKeepAlive = findViewById<Switch>(R.id.switchBtKeepAlive)

        btnBack.setOnClickListener { finish() }

        val activeLangCode = LocaleHelper.getLanguage(this)
        when (activeLangCode) {
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

            if (LocaleHelper.getLanguage(this) != langCode) {
                LocaleHelper.setLocale(this, langCode)
                prefs.edit().putString(KEY_VOICE_LANGUAGE, langName).apply()
                FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Voice language set to $langLabel")

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }

        switchBtKeepAlive.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BT_KEEP_ALIVE, isChecked).apply()
            val status = if (isChecked) "enabled" else "disabled"
            FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Speaker keep-alive boost $status")
        }

        btnTestAlert.setOnClickListener {
            playSampleAlert()
        }
    }

    private fun playSampleAlert() {
        val tokens = mutableListOf<Int>()
        val dingRes = resources.getIdentifier("ding", "raw", packageName)
        if (dingRes != 0) tokens.add(dingRes)

        val providerRes = resources.getIdentifier("easypaisa_par", "raw", packageName)
        if (providerRes != 0) tokens.add(providerRes)

        tokens.addAll(NumberToWordsConverter.getUrduResIds(this, 150L))

        val rupayRes = resources.getIdentifier("rupay", "raw", packageName)
        if (rupayRes != 0) tokens.add(rupayRes)

        val wasoolRes = resources.getIdentifier("wasool_huay", "raw", packageName)
        if (wasoolRes != 0) tokens.add(wasoolRes)

        if (tokens.isNotEmpty()) {
            audioPlayerManager.playSequence(tokens)
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Playing sample payment announcement...")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
