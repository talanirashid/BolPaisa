package com.bolpaisa.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.bolpaisa.app.licensing.SafeEncryptedPreferences

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val profileManager = MerchantProfileManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val isOnboardingCompleted = checkOnboardingCompleted()
            val intent = if (!isOnboardingCompleted) {
                Intent(this, OnboardingActivity::class.java)
            } else if (!profileManager.hasProfile()) {
                Intent(this, MerchantSetupActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun checkOnboardingCompleted(): Boolean {
        return try {
            val prefs = SafeEncryptedPreferences.get(this, "secure_onboarding_prefs")
            prefs.getBoolean(OnboardingActivity.KEY_ONBOARDING_COMPLETED, false)
        } catch (e: Exception) {
            false
        }
    }
}
