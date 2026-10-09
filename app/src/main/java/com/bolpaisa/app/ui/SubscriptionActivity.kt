package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.SubscriptionManager

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var subscriptionManager: SubscriptionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        subscriptionManager = SubscriptionManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvStatusChip = findViewById<TextView>(R.id.tvStatusChip)
        val tvDaysRemaining = findViewById<TextView>(R.id.tvDaysRemaining)
        val tvDeviceId = findViewById<TextView>(R.id.tvDeviceId)
        val btnCopyDeviceId = findViewById<Button>(R.id.btnCopyDeviceId)
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
            tvStatusChip.setBackgroundColor(android.graphics.Color.parseColor("#7F1D1D"))
            tvStatusChip.setTextColor(android.graphics.Color.parseColor("#EF4444"))
            tvDaysRemaining.text = "0 Days Remaining"
        }

        btnCopyDeviceId.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device ID", deviceId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Device ID copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnSendWhatsAppProof.setOnClickListener {
            val message = "Assalam-o-Alaikum, mene Rs. 150/1200 bhej diye hain.\nMera Device ID: $deviceId\nTrx ID (TID): "
            val url = "https://wa.me/923336366291?text=${Uri.encode(message)}"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
            }
        }

        btnRedeemKey.setOnClickListener {
            val key = etKeyInput.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter your 8-digit key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (subscriptionManager.activateLicense(key)) {
                Toast.makeText(this, "License activated successfully!", Toast.LENGTH_LONG).show()
                recreate()
            } else {
                Toast.makeText(this, "Invalid or already used key.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
