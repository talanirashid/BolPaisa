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
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class DynamicQrDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_dynamic_qr)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnGenerate = findViewById<Button>(R.id.btnGenerateQr)
        val ivQrCode = findViewById<ImageView>(R.id.ivQrCode)
        val tvQrPayload = findViewById<TextView>(R.id.tvQrPayload)

        btnGenerate.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qrPayload = "raast://pay?pa=03336366291@raast&pn=BolPaisa&am=$amountStr&cu=PKR"
            val bitmap = generateQrBitmap(qrPayload, 600, 600)
            if (bitmap != null) {
                ivQrCode.setImageBitmap(bitmap)
                ivQrCode.visibility = View.VISIBLE
                tvQrPayload.text = "Raast QR Payload: Rs. $amountStr"
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
