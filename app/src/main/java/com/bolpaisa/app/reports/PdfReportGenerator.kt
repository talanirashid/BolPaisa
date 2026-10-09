package com.bolpaisa.app.reports

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import com.bolpaisa.app.R
import com.bolpaisa.app.data.TransactionEntity
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.bolpaisa.app.util.FeedbackHelper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateDailyReport(context: Context, transactions: List<TransactionEntity>, totalSum: Double) {
        val profileManager = MerchantProfileManager(context)
        val shopName = profileManager.getShopName()
        val customLogo = profileManager.getShopLogo()

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault())
        val todayStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())

        val logoBitmap: Bitmap? = customLogo ?: try {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_mark)
        } catch (e: Exception) {
            null
        }

        var textStartX = 40f
        if (logoBitmap != null) {
            val destRect = Rect(40, 35, 88, 83)
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
            textStartX = 100f
        }

        canvas.drawText("$shopName - Daily Summary", textStartX, 52f, titlePaint)
        canvas.drawText("Verified via BolPaisa | Date: $todayStr", textStartX, 70f, subTitlePaint)

        paint.color = Color.GRAY
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 95f, 555f, 95f, paint)

        var y = 120f
        canvas.drawText("Time", 40f, y, boldPaint)
        canvas.drawText("Gateway / Provider", 180f, y, boldPaint)
        canvas.drawText("Sender Name", 340f, y, boldPaint)
        canvas.drawText("Amount (Rs.)", 460f, y, boldPaint)

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)

        y += 20f
        for (trx in transactions) {
            if (y > 780f) break

            val timeStr = dateFormat.format(Date(trx.timestamp))
            val sender = trx.senderName ?: "-"

            canvas.drawText(timeStr, 40f, y, textPaint)
            canvas.drawText(trx.provider, 180f, y, textPaint)
            canvas.drawText(sender, 340f, y, textPaint)
            canvas.drawText("Rs. ${trx.amount}", 460f, y, textPaint)

            y += 20f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 25f
        canvas.drawText("Total Transactions: ${transactions.size}", 40f, y, boldPaint)
        canvas.drawText("Total Wasool Shuda: Rs. ${totalSum.toLong()}", 340f, y, boldPaint)

        pdfDocument.finishPage(page)

        val fileName = "BolPaisa_${shopName.replace(" ", "_")}_$todayStr.pdf"
        savePdfFile(context, pdfDocument, fileName)
    }

    private fun savePdfFile(context: Context, pdfDocument: PdfDocument, fileName: String) {
        try {
            val successMessage = "PDF Report saved to Documents/BolPaisa"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/BolPaisa")
                }
                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
                if (uri != null) {
                    val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        pdfDocument.writeTo(outputStream)
                        outputStream.close()
                        showNotification(context, successMessage)
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "BolPaisa")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                val fos = FileOutputStream(file)
                pdfDocument.writeTo(fos)
                fos.close()
                showNotification(context, successMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showNotification(context, "Failed to save PDF: ${e.message}", isError = true)
        } finally {
            pdfDocument.close()
        }
    }

    private fun showNotification(context: Context, message: String, isError: Boolean = false) {
        if (context is android.app.Activity) {
            val view = context.findViewById<View>(android.R.id.content)
            if (view != null) {
                if (isError) {
                    FeedbackHelper.showError(view, message)
                } else {
                    FeedbackHelper.showSuccess(view, message)
                }
                return
            }
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
