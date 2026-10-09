package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        val btnWhatsApp = findViewById<Button>(R.id.btnWhatsApp)
        val btnEmail = findViewById<Button>(R.id.btnEmail)

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            tvVersion.text = "Version 1.0.0"
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
}
