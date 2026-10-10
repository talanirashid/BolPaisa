package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.KeyGenerationEngine

class AdminPortalActivity : BaseActivity() {

    private var generatedKey: String = ""
    private var selectedDays: Int = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_portal)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etDeviceId = findViewById<EditText>(R.id.etDeviceId)
        val btnPaste = findViewById<Button>(R.id.btnPasteClipboard)
        val rgDuration = findViewById<RadioGroup>(R.id.rgDuration)
        val rb30Days = findViewById<RadioButton>(R.id.rb30Days)
        val rb90Days = findViewById<RadioButton>(R.id.rb90Days)
        val rb365Days = findViewById<RadioButton>(R.id.rb365Days)
        val btnGenerate = findViewById<Button>(R.id.btnGenerateKey)
        val layoutResult = findViewById<View>(R.id.layoutKeyResult)
        val tvGeneratedKey = findViewById<TextView>(R.id.tvGeneratedKey)
        val btnCopyKey = findViewById<Button>(R.id.btnCopyKey)
        val btnShareWhatsApp = findViewById<Button>(R.id.btnShareWhatsApp)

        btnBack.setOnClickListener { finish() }

        btnPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pastedText = clip.getItemAt(0).text.toString().trim()
                etDeviceId.setText(pastedText)
                Toast.makeText(this, "Pasted from clipboard!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
            }
        }

        rgDuration.setOnCheckedChangeListener { _, checkedId ->
            selectedDays = when (checkedId) {
                R.id.rb90Days -> 90
                R.id.rb365Days -> 365
                else -> 30
            }
        }

        btnGenerate.setOnClickListener {
            val deviceId = etDeviceId.text.toString().trim()
            if (deviceId.isEmpty()) {
                Toast.makeText(this, "Please enter or paste the target Device ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            generatedKey = KeyGenerationEngine.generateActivationKey(deviceId, selectedDays)
            tvGeneratedKey.text = generatedKey
            layoutResult.visibility = View.VISIBLE
            Toast.makeText(this, "Key generated successfully!", Toast.LENGTH_SHORT).show()
        }

        btnCopyKey.setOnClickListener {
            if (generatedKey.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Activation Key", generatedKey)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Key copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }

        btnShareWhatsApp.setOnClickListener {
            if (generatedKey.isNotEmpty()) {
                val replyText = """
                    Assalam-o-Alaikum!
                    Aapka BolPaisa activation code yeh hai:
                    Key: $generatedKey
                    Validity: $selectedDays Days

                    Isko Subscription screen par 'Enter Activation Code' mein darj karein. Shukriya!
                """.trimIndent()

                val url = "https://wa.me/?text=${Uri.encode(replyText)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, replyText)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, "Share Activation Key"))
                }
            }
        }
    }
}
