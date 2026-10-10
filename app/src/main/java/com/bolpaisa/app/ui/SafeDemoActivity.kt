package com.bolpaisa.app.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.audio.VoiceAlertEngine
import com.bolpaisa.app.reports.PdfReportGenerator
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.LocaleHelper

class SafeDemoActivity : AppCompatActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var voiceAlertEngine: VoiceAlertEngine

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_demo)

        audioPlayerManager = AudioPlayerManager(this)
        voiceAlertEngine = VoiceAlertEngine(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnSimulateEasypaisa = findViewById<Button>(R.id.btnSimulateEasypaisa)
        val btnSimulateJazzCash = findViewById<Button>(R.id.btnSimulateJazzCash)
        val btnPreviewDemoPdf = findViewById<Button>(R.id.btnPreviewDemoPdf)

        btnBack.setOnClickListener { finish() }

        btnSimulateEasypaisa.setOnClickListener {
            val activeLang = LocaleHelper.getLanguage(this)
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Simulating Easypaisa Rs. 150 voice alert (DEMO)...")
            voiceAlertEngine.speakPaymentAlert("Easypaisa (DEMO)", 150, activeLang)
        }

        btnSimulateJazzCash.setOnClickListener {
            val activeLang = LocaleHelper.getLanguage(this)
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Simulating JazzCash Rs. 500 voice alert (DEMO)...")
            voiceAlertEngine.speakPaymentAlert("JazzCash (DEMO)", 500, activeLang)
        }

        btnPreviewDemoPdf.setOnClickListener {
            PdfReportGenerator.generateDemoReport(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
        voiceAlertEngine.release()
    }
}
