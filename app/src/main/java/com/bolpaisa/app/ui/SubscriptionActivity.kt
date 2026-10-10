package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.SubscriptionManager
import com.bolpaisa.app.util.FeedbackHelper

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var subscriptionManager: SubscriptionManager
    private var selectedPlanName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        subscriptionManager = SubscriptionManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvStatusChip = findViewById<TextView>(R.id.tvStatusChip)
        val tvDaysRemaining = findViewById<TextView>(R.id.tvDaysRemaining)
        val tvDeviceId = findViewById<TextView>(R.id.tvDeviceId)
        val btnCopyDeviceId = findViewById<Button>(R.id.btnCopyDeviceId)
        val cardMonthly = findViewById<View>(R.id.cardMonthlyPlan)
        val cardAnnual = findViewById<View>(R.id.cardAnnualPlan)
        val tvMonthlyCheck = findViewById<TextView>(R.id.tvMonthlyCheck)
        val tvAnnualCheck = findViewById<TextView>(R.id.tvAnnualCheck)
        val btnSendWhatsAppProof = findViewById<Button>(R.id.btnSendWhatsAppProof)
        val etKeyInput = findViewById<EditText>(R.id.etKeyInput)
        val btnRedeemKey = findViewById<Button>(R.id.btnRedeemKey)

        btnBack.setOnClickListener { finish() }

        val deviceId = subscriptionManager.getDeviceId()
        tvDeviceId.text = "Device ID: $deviceId"

        val isActive = subscriptionManager.isSubscriptionActive()
        val remainingDays = subscriptionManager.getRemainingDays()

        if (isActive) {
            tvStatusChip.text = "✓ Active License"
            tvDaysRemaining.text = "$remainingDays Days Remaining"
        } else {
            tvStatusChip.text = "● Expired"
            tvStatusChip.setBackgroundColor(Color.parseColor("#7F1D1D"))
            tvStatusChip.setTextColor(Color.parseColor("#EF4444"))
            tvDaysRemaining.text = "0 Days Remaining"
        }

        tvMonthlyCheck.visibility = View.GONE
        tvAnnualCheck.visibility = View.GONE

        btnCopyDeviceId.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device ID", deviceId)
            clipboard.setPrimaryClip(clip)
            FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Device ID copied to clipboard!")
        }

        cardMonthly.setOnClickListener {
            selectedPlanName = "Monthly Pass - Rs. 150"
            tvMonthlyCheck.text = "✓ Selected"
            tvMonthlyCheck.visibility = View.VISIBLE
            tvAnnualCheck.visibility = View.GONE
            cardMonthly.setBackgroundColor(Color.parseColor("#1B382B"))
            cardAnnual.setBackgroundColor(Color.parseColor("#1E293B"))
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Selected: Monthly Pass - Rs. 150")
        }

        cardAnnual.setOnClickListener {
            selectedPlanName = "Annual Pass - Rs. 1,200"
            tvAnnualCheck.text = "✓ Selected"
            tvAnnualCheck.visibility = View.VISIBLE
            tvMonthlyCheck.visibility = View.GONE
            cardAnnual.setBackgroundColor(Color.parseColor("#1B382B"))
            cardMonthly.setBackgroundColor(Color.parseColor("#1E293B"))
            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Selected: Annual Pass - Rs. 1,200")
        }

        btnSendWhatsAppProof.setOnClickListener {
            if (selectedPlanName == null) {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Please select a license plan (Monthly or Annual) first!")
                return@setOnClickListener
            }

            val message = """
                Assalam-o-Alaikum Mehrzaad Technologies!
                I want to activate BolPaisa:
                - Selected Plan: $selectedPlanName
                - My Device ID: $deviceId
                - Payment Trx ID (TID): [PLEASE ENTER TID]

                (I have attached the payment receipt screenshot with this message. Please send my 8-digit activation key.)
            """.trimIndent()

            val url = "https://wa.me/923336366291?text=${Uri.encode(message)}"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                FeedbackHelper.showError(findViewById(android.R.id.content), "WhatsApp is not installed on this device.")
            }
        }

        btnRedeemKey.setOnClickListener {
            val key = etKeyInput.text.toString().trim()
            if (key.isEmpty()) {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Please enter your 8-digit key")
                return@setOnClickListener
            }

            if (subscriptionManager.activateLicense(key)) {
                FeedbackHelper.showSuccess(findViewById(android.R.id.content), "License activated successfully!")
                recreate()
            } else {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Invalid or already used activation key.")
            }
        }
    }
}
