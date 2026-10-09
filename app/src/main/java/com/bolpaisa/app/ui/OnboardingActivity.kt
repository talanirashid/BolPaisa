package com.bolpaisa.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.viewpager2.widget.ViewPager2
import com.bolpaisa.app.R

class OnboardingActivity : AppCompatActivity() {

    companion object {
        const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var btnSkip: Button
    private lateinit var btnNextContinue: Button
    private lateinit var tvDotsIndicator: TextView

    private val dots = arrayOf("● ○ ○ ○", "○ ● ○ ○", "○ ○ ● ○", "○ ○ ○ ●")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPagerOnboarding)
        btnSkip = findViewById(R.id.btnSkip)
        btnNextContinue = findViewById(R.id.btnNextContinue)
        tvDotsIndicator = findViewById(R.id.tvDotsIndicator)

        val slides = listOf(
            OnboardingSlide(
                title = "Instant Voice Alerts\n(آواز سے تصدیق)",
                subtitle = "Receive hands-free payment voice announcements for Easypaisa, JazzCash, and Raast right on your shop counter.",
                iconResId = R.drawable.ic_speaker
            ),
            OnboardingSlide(
                title = "Select Your Wallets",
                subtitle = "Supports Easypaisa (3737), JazzCash (8558), and Raast dynamic Till & account payment notifications.",
                iconResId = R.drawable.ic_qr
            ),
            OnboardingSlide(
                title = "Enable Notification Access",
                subtitle = "BolPaisa requires Notification Access to listen for incoming payment notifications and announce amounts.",
                iconResId = R.drawable.ic_analytics,
                actionText = "Enable Permission",
                onActionClick = {
                    try {
                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            ),
            OnboardingSlide(
                title = "100% Offline & Private\n(محفوظ اور پرائیویٹ)",
                subtitle = "Your transaction history and activation keys stay 100% locally on your device with zero cloud syncing.",
                iconResId = R.drawable.logo_mark
            )
        )

        val adapter = OnboardingAdapter(slides)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tvDotsIndicator.text = dots[position]
                if (position == slides.size - 1) {
                    btnNextContinue.text = "Continue"
                } else {
                    btnNextContinue.text = "Next"
                }
            }
        })

        btnSkip.setOnClickListener {
            completeOnboarding()
        }

        btnNextContinue.setOnClickListener {
            if (viewPager.currentItem < slides.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                completeOnboarding()
            }
        }
    }

    private fun completeOnboarding() {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                this,
                "secure_onboarding_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
