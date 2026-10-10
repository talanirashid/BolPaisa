package com.bolpaisa.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.audio.VoiceAlertEngine
import com.bolpaisa.app.reports.PdfReportGenerator
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.LocaleHelper

class SafeDemoActivity : BaseActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var voiceAlertEngine: VoiceAlertEngine

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
            val activeVoiceLang = LocaleHelper.getVoiceLanguage(this)
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Simulating Easypaisa Rs. 150 voice alert (DEMO)...")
            voiceAlertEngine.speakPaymentAlert("Easypaisa (DEMO)", 150, activeVoiceLang)
        }

        btnSimulateJazzCash.setOnClickListener {
            val activeVoiceLang = LocaleHelper.getVoiceLanguage(this)
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Simulating JazzCash Rs. 500 voice alert (DEMO)...")
            voiceAlertEngine.speakPaymentAlert("JazzCash (DEMO)", 500, activeVoiceLang)
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
