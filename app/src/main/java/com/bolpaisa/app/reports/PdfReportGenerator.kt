package com.bolpaisa.app.reports

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.bolpaisa.app.data.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateDailyReport(context: Context, transactions: List<TransactionEntity>, totalSum: Double) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
        }

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault())
        val todayStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())

        canvas.drawText("BolPaisa - Daily Transaction Summary", 40f, 50f, titlePaint)
        canvas.drawText("Developed by Mehrzaad Technologies | Date: $todayStr", 40f, 70f, subTitlePaint)

        paint.color = Color.GRAY
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 85f, 555f, 85f, paint)

        var y = 110f
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

        val fileName = "BolPaisa_Report_$todayStr.pdf"
        savePdfFile(context, pdfDocument, fileName)
    }

    private fun savePdfFile(context: Context, pdfDocument: PdfDocument, fileName: String) {
        try {
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
                        Toast.makeText(context, "PDF Report saved to Documents/BolPaisa", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "BolPaisa")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                val fos = FileOutputStream(file)
                pdfDocument.writeTo(fos)
                fos.close()
                Toast.makeText(context, "PDF saved to Documents/BolPaisa/$fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
