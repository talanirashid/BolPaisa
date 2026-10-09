package com.bolpaisa.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.MerchantProfileManager

class ShopSetupDialog(
    context: Context,
    private val onProfileSaved: () -> Unit,
    private val onSelectLogoRequested: () -> Unit
) : Dialog(context) {

    private val profileManager = MerchantProfileManager(context)
    private lateinit var ivLogoPreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_shop_setup)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

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
            when (profileManager.getGatewayType()) {
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
            onSelectLogoRequested()
        }

        btnSaveProfile.setOnClickListener {
            val shopName = etShopName.text.toString().trim()
            val accountOrTill = etAccountOrTill.text.toString().trim()

            if (shopName.isEmpty()) {
                Toast.makeText(context, "Please enter your Shop / Business Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (accountOrTill.isEmpty()) {
                Toast.makeText(context, "Please enter your Till ID or Account Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedGateway = when (rgGateway.checkedRadioButtonId) {
                R.id.rbJazzCash -> "JAZZCASH"
                R.id.rbRaast -> "RAAST"
                else -> "EASYPAISA"
            }

            profileManager.saveProfile(shopName, selectedGateway, accountOrTill)
            Toast.makeText(context, "Shop profile saved successfully!", Toast.LENGTH_SHORT).show()
            onProfileSaved()
            dismiss()
        }
    }

    fun updateLogoPreview(imageUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                profileManager.saveShopLogo(bitmap)
                ivLogoPreview.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
