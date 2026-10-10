package com.bolpaisa.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import com.bolpaisa.app.R
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.LocaleHelper

class LanguageSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rgLanguage = findViewById<RadioGroup>(R.id.rgAppLanguage)
        val rbEnglish = findViewById<RadioButton>(R.id.rbLangEnglish)
        val rbUrdu = findViewById<RadioButton>(R.id.rbLangUrdu)
        val rbSindhi = findViewById<RadioButton>(R.id.rbLangSindhi)
        val btnApply = findViewById<Button>(R.id.btnApplyLanguage)

        btnBack.setOnClickListener { finish() }

        val activeLang = LocaleHelper.getAppUiLanguage(this)
        when (activeLang) {
            "ur" -> rbUrdu.isChecked = true
            "sd" -> rbSindhi.isChecked = true
            else -> rbEnglish.isChecked = true
        }

        btnApply.setOnClickListener {
            val selectedLangCode = when (rgLanguage.checkedRadioButtonId) {
                R.id.rbLangUrdu -> "ur"
                R.id.rbLangSindhi -> "sd"
                else -> "en"
            }

            LocaleHelper.setAppUiLanguage(this, selectedLangCode)
            FeedbackHelper.showSuccess(findViewById(android.R.id.content), "App language updated!")

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
