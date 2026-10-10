package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.bolpaisa.app.licensing.SubscriptionManager

class AboutActivity : AppCompatActivity() {

    private var tapCount = 0
    private var lastTapTime = 0L

    companion object {
        private const val ADMIN_MASTER_PIN = "336366"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val ivLogo = findViewById<ImageView>(R.id.ivAboutLogo)
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        val btnShareDiagnostic = findViewById<Button>(R.id.btnShareDiagnostic)
        val btnWhatsApp = findViewById<Button>(R.id.btnWhatsApp)
        val btnEmail = findViewById<Button>(R.id.btnEmail)

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            tvVersion.text = "Version 1.0.0"
        }

        ivLogo?.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime > 3000) {
                tapCount = 1
            } else {
                tapCount++
            }
            lastTapTime = currentTime

            if (tapCount >= 5) {
                tapCount = 0
                showAdminPinDialog()
            }
        }

        btnShareDiagnostic?.setOnClickListener {
            shareDiagnosticReport()
        }

        btnWhatsApp.setOnClickListener {
            val url = "https://wa.me/923336366291?text=Assalam-o-Alaikum%20Mehrzaad%20Technologies,%20I%20need%20help%20with%20BolPaisa"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
            }
        }

        btnEmail.setOnClickListener {
            val email = "mehrzaadtechnologies@gmail.com"
            val subject = "BolPaisa Support"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No email client found on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareDiagnosticReport() {
        val subscriptionManager = SubscriptionManager(this)
        val profileManager = MerchantProfileManager(this)

        var versionStr = "1.0.1"
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionStr = pInfo.versionName ?: "1.0.1"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dbFile = getDatabasePath("bolpaisa_database")
        val dbSizeKb = if (dbFile.exists()) dbFile.length() / 1024 else 0

        val reportText = """
            *BolPaisa Diagnostic Support Report*
            App Version: $versionStr
            Android SDK: ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})
            Device Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
            Shop Name: ${profileManager.getShopName()}
            Gateway: ${profileManager.getGatewayType()}
            Device ID: ${subscriptionManager.getDeviceId()}
            Subscription Active: ${subscriptionManager.isSubscriptionActive()} (${subscriptionManager.getRemainingDays()} days left)
            Database Size: ${dbSizeKb} KB
            (Privacy Note: Secrets, keys, and private customer payments excluded)
        """.trimIndent()

        val url = "https://wa.me/923336366291?text=${Uri.encode(reportText)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, reportText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Share Diagnostic Report"))
        }
    }

    private fun showAdminPinDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Admin Verification")

        val input = EditText(this).apply {
            hint = "Enter Master PIN (e.g. 336366)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(40, 40, 40, 40)
        }
        builder.setView(input)

        builder.setPositiveButton("Verify & Access Console") { _, _ ->
            val pin = input.text.toString().trim()
            if (pin == ADMIN_MASTER_PIN) {
                Toast.makeText(this, "Master PIN Verified!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminPortalActivity::class.java))
            } else {
                Toast.makeText(this, "Unauthorized access: Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}
