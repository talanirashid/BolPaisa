package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.SubscriptionManager

class MainActivity : AppCompatActivity() {

    private lateinit var subscriptionManager: SubscriptionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscriptionManager = SubscriptionManager(this)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnActivate = findViewById<Button>(R.id.btnActivate)
        val btnWhatsAppSupport = findViewById<Button>(R.id.btnWhatsAppSupport)
        val btnAbout = findViewById<Button>(R.id.btnAbout)

        updateStatusDisplay(tvStatus)

        btnActivate.setOnClickListener {
            showActivationDialog(tvStatus)
        }

        btnWhatsAppSupport.setOnClickListener {
            openWhatsAppSupport()
        }

        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun updateStatusDisplay(tvStatus: TextView) {
        val isActive = subscriptionManager.isSubscriptionActive()
        val remainingDays = subscriptionManager.getRemainingDays()
        val deviceId = subscriptionManager.getDeviceId()

        if (isActive) {
            tvStatus.text = "Status: ACTIVE\nRemaining Trial / License: $remainingDays days\nDevice ID: $deviceId"
            tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))
        } else {
            tvStatus.text = "Status: EXPIRED\nPlease activate your license to continue voice alerts.\nDevice ID: $deviceId"
            tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"))
            // Automatically prompt activation dialog if expired
            showActivationDialog(tvStatus)
        }
    }

    private fun showActivationDialog(tvStatus: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Activate BolPaisa License")

        val input = EditText(this).apply {
            hint = "Enter 8-digit activation code (e.g. A3F8-9B2C)"
            setPadding(40, 40, 40, 40)
        }
        builder.setView(input)

        builder.setPositiveButton("Activate") { _, _ ->
            val code = input.text.toString()
            if (subscriptionManager.activateLicense(code)) {
                Toast.makeText(this, "License activated successfully!", Toast.LENGTH_LONG).show()
                updateStatusDisplay(tvStatus)
            } else {
                Toast.makeText(this, "Invalid or already used activation code.", Toast.LENGTH_LONG).show()
            }
        }

        builder.setNeutralButton("WhatsApp Device ID") { _, _ ->
            openWhatsAppSupport()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun openWhatsAppSupport() {
        val phoneNumber = "923336366291"
        val deviceId = subscriptionManager.getDeviceId()
        val message = "Mene Rs. 150 bhej diye hain.\nMera Device ID: $deviceId\nTrx ID (TID): "
        val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
        }
    }
}
