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
import android.widget.TextView
import android.widget.Toast
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class DynamicQrDialog(context: Context) : Dialog(context) {

    private val profileManager = MerchantProfileManager(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_dynamic_qr)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnGenerate = findViewById<Button>(R.id.btnGenerateQr)
        val btnCancel = findViewById<Button>(R.id.btnCancelQr)
        val ivQrCode = findViewById<ImageView>(R.id.ivQrCode)
        val tvQrPayload = findViewById<TextView>(R.id.tvQrPayload)

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

            val shopName = profileManager.getShopName()
            val gatewayType = profileManager.getGatewayType()
            val accountOrTill = profileManager.getAccountOrTill()

            val isTillId = accountOrTill.length <= 8 && accountOrTill.all { it.isDigit() }

            val qrPayload = when (gatewayType.uppercase()) {
                "RAAST" -> "raast://pay?receiver=$accountOrTill&amount=$amountStr&ref=BolPaisa"
                "JAZZCASH" -> if (isTillId) {
                    "jazzcash://merchant?merchant_id=$accountOrTill&amount=$amountStr"
                } else {
                    "jazzcash://pay?receiver=$accountOrTill&amount=$amountStr"
                }
                else -> if (isTillId) { // EASYPAISA
                    "easypaisa://till?till_id=$accountOrTill&amount=$amountStr"
                } else {
                    "easypaisa://pay?receiver=$accountOrTill&amount=$amountStr"
                }
            }

            val bitmap = generateQrBitmap(qrPayload, 600, 600)
            if (bitmap != null) {
                ivQrCode.setImageBitmap(bitmap)
                ivQrCode.visibility = View.VISIBLE
                tvQrPayload.text = "Scan to Pay $shopName\n$gatewayType ($accountOrTill) • Rs. $amountStr"
                tvQrPayload.visibility = View.VISIBLE
            } else {
                Toast.makeText(context, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
            }
        }
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
