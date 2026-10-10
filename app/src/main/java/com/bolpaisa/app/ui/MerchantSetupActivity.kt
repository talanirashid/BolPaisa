package com.bolpaisa.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.LocaleHelper

class MerchantSetupActivity : AppCompatActivity() {

    private lateinit var profileManager: MerchantProfileManager
    private lateinit var ivLogoPreview: ImageView

    private val selectLogoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    profileManager.saveShopLogo(bitmap)
                    ivLogoPreview.setImageBitmap(bitmap)
                    FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Shop logo saved!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merchant_setup)

        profileManager = MerchantProfileManager(this)

        ivLogoPreview = findViewById(R.id.ivShopLogoPreview)
        val btnSelectLogo = findViewById<Button>(R.id.btnSelectLogo)
        val etShopName = findViewById<EditText>(R.id.etShopName)
        val rgGateway = findViewById<RadioGroup>(R.id.rgGateway)
        val rbEasypaisa = findViewById<RadioButton>(R.id.rbEasypaisa)
        val rbJazzCash = findViewById<RadioButton>(R.id.rbJazzCash)
        val rbRaast = findViewById<RadioButton>(R.id.rbRaast)
        val etAccountOrTill = findViewById<EditText>(R.id.etAccountOrTill)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)

        if (profileManager.hasProfile()) {
            etShopName.setText(profileManager.getShopName())
            etAccountOrTill.setText(profileManager.getAccountOrTill())
            when (profileManager.getGatewayType().uppercase()) {
                "JAZZCASH" -> rbJazzCash.isChecked = true
                "RAAST" -> rbRaast.isChecked = true
                else -> rbEasypaisa.isChecked = true
            }
        }

        val logoBitmap = profileManager.getShopLogo()
        if (logoBitmap != null) {
            ivLogoPreview.setImageBitmap(logoBitmap)
        }

        btnSelectLogo.setOnClickListener {
            selectLogoLauncher.launch("image/*")
        }

        btnSaveProfile.setOnClickListener {
            val shopName = etShopName.text.toString().trim()
            val accountOrTill = etAccountOrTill.text.toString().trim()

            if (shopName.isEmpty()) {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Please enter your Shop / Business Name")
                return@setOnClickListener
            }

            if (accountOrTill.isEmpty()) {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Please enter your Till ID or Account Number")
                return@setOnClickListener
            }

            val selectedGateway = when (rgGateway.checkedRadioButtonId) {
                R.id.rbJazzCash -> "JAZZCASH"
                R.id.rbRaast -> "RAAST"
                else -> "EASYPAISA"
            }

            profileManager.saveProfile(shopName, selectedGateway, accountOrTill)
            FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Business profile saved successfully!")

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
