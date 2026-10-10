package com.bolpaisa.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.reports.PdfReportGenerator
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.LocaleHelper

class SafeDemoActivity : AppCompatActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_demo)

        audioPlayerManager = AudioPlayerManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnPlayDemoAudio = findViewById<Button>(R.id.btnPlayDemoAudio)
        val btnPreviewDemoPdf = findViewById<Button>(R.id.btnPreviewDemoPdf)

        btnBack.setOnClickListener { finish() }

        btnPlayDemoAudio.setOnClickListener {
            val activeLang = LocaleHelper.getLanguage(this)
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Playing demo payment announcement...")
            audioPlayerManager.playSampleAlert(activeLang)
        }

        btnPreviewDemoPdf.setOnClickListener {
            PdfReportGenerator.generateDemoReport(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
