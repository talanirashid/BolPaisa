package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val btnWhatsApp = findViewById<Button>(R.id.btnWhatsApp)
        val btnEmail = findViewById<Button>(R.id.btnEmail)

        btnWhatsApp.setOnClickListener {
            val phoneNumber = "923336366291"
            val message = "Assalam-o-Alaikum Mehrzaad Technologies, I need help with BolPaisa."
            val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
            }
        }

        btnEmail.setOnClickListener {
            val email = "mehrzaadtechnologies@gmail.com"
            val subject = "BolPaisa Support & Feedback"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No email app found on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
