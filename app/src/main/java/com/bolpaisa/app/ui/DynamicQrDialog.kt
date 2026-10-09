package com.bolpaisa.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class DynamicQrDialog(context: Context) : Dialog(context) {

    private val profileManager = MerchantProfileManager(context)
    private var selectedGateway = "EASYPAISA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_dynamic_qr)

        val rgGateway = findViewById<RadioGroup>(R.id.rgQrGateway)
        val rbEasypaisa = findViewById<RadioButton>(R.id.rbChipEasypaisa)
        val rbJazzCash = findViewById<RadioButton>(R.id.rbChipJazzCash)
        val rbRaast = findViewById<RadioButton>(R.id.rbChipRaast)
        val tvReceiverInfoBar = findViewById<TextView>(R.id.tvReceiverInfoBar)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnGenerate = findViewById<Button>(R.id.btnGenerateQr)
        val btnCancel = findViewById<Button>(R.id.btnCancelQr)
        val ivQrCode = findViewById<ImageView>(R.id.ivQrCode)
        val tvQrPayload = findViewById<TextView>(R.id.tvQrPayload)

        selectedGateway = profileManager.getGatewayType()
        when (selectedGateway.uppercase()) {
            "JAZZCASH" -> rbJazzCash.isChecked = true
            "RAAST" -> rbRaast.isChecked = true
            else -> rbEasypaisa.isChecked = true
        }
        updateGatewayUi(selectedGateway, rbEasypaisa, rbJazzCash, rbRaast, tvReceiverInfoBar)

        rgGateway.setOnCheckedChangeListener { _, checkedId ->
            selectedGateway = when (checkedId) {
                R.id.rbChipJazzCash -> "JAZZCASH"
                R.id.rbChipRaast -> "RAAST"
                else -> "EASYPAISA"
            }
            updateGatewayUi(selectedGateway, rbEasypaisa, rbJazzCash, rbRaast, tvReceiverInfoBar)
        }

        tvReceiverInfoBar.setOnClickListener {
            promptSetGatewayId(selectedGateway) {
                updateGatewayUi(selectedGateway, rbEasypaisa, rbJazzCash, rbRaast, tvReceiverInfoBar)
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnGenerate.setOnClickListener {
            var amountStr = etAmount.text.toString().trim()
            if (amountStr.startsWith("Rs.", true)) {
                amountStr = amountStr.substring(3).trim()
            }
            val amount = amountStr.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                Toast.makeText(context, "Please enter a valid payment amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gatewayId = profileManager.getGatewayId(selectedGateway)
            if (gatewayId.isEmpty()) {
                promptSetGatewayId(selectedGateway) {
                    updateGatewayUi(selectedGateway, rbEasypaisa, rbJazzCash, rbRaast, tvReceiverInfoBar)
                }
                return@setOnClickListener
            }

            val shopName = profileManager.getShopName()
            val isTillId = gatewayId.length <= 8 && gatewayId.all { it.isDigit() }

            val qrPayload = when (selectedGateway.uppercase()) {
                "RAAST" -> "raast://pay?receiver=$gatewayId&amount=$amountStr&ref=BolPaisa"
                "JAZZCASH" -> if (isTillId) {
                    "jazzcash://merchant?merchant_id=$gatewayId&amount=$amountStr"
                } else {
                    "jazzcash://pay?receiver=$gatewayId&amount=$amountStr"
                }
                else -> if (isTillId) { // EASYPAISA
                    "easypaisa://till?till_id=$gatewayId&amount=$amountStr"
                } else {
                    "easypaisa://pay?receiver=$gatewayId&amount=$amountStr"
                }
            }

            val bitmap = generateQrBitmap(qrPayload, 600, 600)
            if (bitmap != null) {
                ivQrCode.setImageBitmap(bitmap)
                ivQrCode.visibility = View.VISIBLE
                tvQrPayload.text = "Scan with $selectedGateway App\n$shopName ($gatewayId) • Rs. $amountStr"
                tvQrPayload.visibility = View.VISIBLE
            } else {
                Toast.makeText(context, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateGatewayUi(
        gateway: String,
        rbEasypaisa: RadioButton,
        rbJazzCash: RadioButton,
        rbRaast: RadioButton,
        tvReceiverInfoBar: TextView
    ) {
        val activeBg = Color.parseColor("#10B981")
        val inactiveBg = Color.parseColor("#1E293B")

        rbEasypaisa.setBackgroundColor(if (gateway == "EASYPAISA") activeBg else inactiveBg)
        rbJazzCash.setBackgroundColor(if (gateway == "JAZZCASH") activeBg else inactiveBg)
        rbRaast.setBackgroundColor(if (gateway == "RAAST") activeBg else inactiveBg)

        val id = profileManager.getGatewayId(gateway)
        if (id.isNotEmpty()) {
            tvReceiverInfoBar.text = "Receiving on: $gateway ($id)"
            tvReceiverInfoBar.setTextColor(Color.parseColor("#10B981"))
        } else {
            tvReceiverInfoBar.text = "Tap to set $gateway Till ID / Number"
            tvReceiverInfoBar.setTextColor(Color.parseColor("#EF4444"))
        }
    }

    private fun promptSetGatewayId(gateway: String, onSaved: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Set $gateway Till/Account ID")

        val input = EditText(context).apply {
            hint = "Enter Till ID or Mobile Account Number"
            setPadding(40, 40, 40, 40)
            setText(profileManager.getGatewayId(gateway))
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val enteredId = input.text.toString().trim()
            if (enteredId.isNotEmpty()) {
                profileManager.saveGatewayId(gateway, enteredId)
                Toast.makeText(context, "$gateway ID saved!", Toast.LENGTH_SHORT).show()
                onSaved()
            } else {
                Toast.makeText(context, "ID cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun generateQrBitmap(content: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
