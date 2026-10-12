package com.bolpaisa.app.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.ContextCompat
import com.bolpaisa.app.R
import java.io.FileOutputStream
import java.io.IOException

enum class StandeeSize(val widthMm: Float, val heightMm: Float, val displayName: String) {
    A5(148f, 210f, "A5 Acrylic Standee (148 x 210 mm)"),
    PHOTO_4X6(101.6f, 152.4f, "4x6 Inch Counter Frame (102 x 152 mm)"),
    A4(210f, 297f, "A4 Counter Poster (210 x 297 mm)")
}

class StandeePrintHelper(private val context: Context) {

    fun printStandee(
        merchantName: String,
        merchantIbanOrTill: String,
        qrBitmap: Bitmap,
        standeeSize: StandeeSize = StandeeSize.A5
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "BolPaisa_Standee_${merchantName.replace(" ", "_")}"

        val adapter = StandeePrintDocumentAdapter(
            context = context,
            merchantName = merchantName,
            merchantIdentifier = merchantIbanOrTill,
            qrBitmap = qrBitmap,
            size = standeeSize
        )

        val printAttributes = PrintAttributes.Builder()
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMediaSize(
                when (standeeSize) {
                    StandeeSize.A5 -> PrintAttributes.MediaSize.ISO_A5
                    StandeeSize.PHOTO_4X6 -> PrintAttributes.MediaSize.NA_INDEX_4X6
                    StandeeSize.A4 -> PrintAttributes.MediaSize.ISO_A4
                }
            )
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, printAttributes)
    }
}

class StandeePrintDocumentAdapter(
    private val context: Context,
    private val merchantName: String,
    private val merchantIdentifier: String,
    private val qrBitmap: Bitmap,
    private val size: StandeeSize
) : PrintDocumentAdapter() {

    private var pdfDocument: PdfDocument? = null

    // 72 points per inch (Android Print Standard)
    private val widthPoints = (size.widthMm / 25.4f * 72f).toInt()
    private val heightPoints = (size.heightMm / 25.4f * 72f).toInt()

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("BolPaisa_Standee.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }

        pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(widthPoints, heightPoints, 1).create()
        val page = pdfDocument!!.startPage(pageInfo)
        val canvas = page.canvas

        drawStandeeCanvas(canvas, widthPoints.toFloat(), heightPoints.toFloat())

        pdfDocument!!.finishPage(page)

        try {
            destination?.let {
                pdfDocument!!.writeTo(FileOutputStream(it.fileDescriptor))
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback?.onWriteFailed(e.message)
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }
    }

    private fun drawStandeeCanvas(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Background (Pure White for clean prints)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, w, h, paint)

        // 2. Top Header (Pakistan Green #006A4E)
        val headerHeight = h * 0.16f
        paint.color = Color.parseColor("#006A4E")
        canvas.drawRect(0f, 0f, w, headerHeight, paint)

        // Accent gold divider line
        paint.color = Color.parseColor("#E2A93B")
        paint.strokeWidth = 3f
        canvas.drawLine(0f, headerHeight, w, headerHeight, paint)

        // Header Title (National Payment Standee)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = headerHeight * 0.28f
        canvas.drawText("NATIONAL DIGITAL PAYMENT", w / 2, headerHeight * 0.45f, paint)

        paint.textSize = headerHeight * 0.18f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("SBP RAAST INTEROPERABLE P2M QR", w / 2, headerHeight * 0.72f, paint)

        // 3. Merchant Details
        var currentY = headerHeight + (h * 0.06f)
        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = h * 0.038f
        canvas.drawText(merchantName.uppercase(), w / 2, currentY, paint)

        currentY += h * 0.024f
        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.DEFAULT
        paint.textSize = h * 0.018f
        canvas.drawText("RAAST / TILL: $merchantIdentifier", w / 2, currentY, paint)

        // 4. Center QR Code Canvas with rounded frame
        currentY += h * 0.04f
        val qrBoxSize = w * 0.65f
        val qrLeft = (w - qrBoxSize) / 2
        val qrTop = currentY
        val qrRect = RectF(qrLeft, qrTop, qrLeft + qrBoxSize, qrTop + qrBoxSize)

        // Outer Border
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(qrRect, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        // Draw High-Res QR Bitmap
        val destRect = Rect((qrLeft + 12).toInt(), (qrTop + 12).toInt(), (qrLeft + qrBoxSize - 12).toInt(), (qrLeft + qrBoxSize - 12).toInt())
        canvas.drawBitmap(qrBitmap, null, destRect, paint)

        // 5. Urdu & English Callout Instructions
        currentY = qrTop + qrBoxSize + (h * 0.045f)
        paint.color = Color.parseColor("#006A4E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = h * 0.034f
        canvas.drawText("یہاں سے رقم ادا کریں", w / 2, currentY, paint)

        currentY += h * 0.024f
        paint.color = Color.parseColor("#334155")
        paint.typeface = Typeface.DEFAULT
        paint.textSize = h * 0.019f
        canvas.drawText("Scan & Pay with any Banking or Wallet App", w / 2, currentY, paint)

        // 6. Footer Payment Rails (Easypaisa, JazzCash, Raast / 1LINK)
        val footerTop = h - (h * 0.13f)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(0f, footerTop, w, h, paint)

        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(0f, footerTop, w, footerTop, paint)

        // Supported Brands Text
        paint.color = Color.parseColor("#64748B")
        paint.textSize = h * 0.016f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ACCEPTED DIGITAL CHANNELS", w / 2, footerTop + (h * 0.032f), paint)

        // Draw Brand Badges across the footer
        drawBrandBadges(canvas, w, footerTop + (h * 0.045f), h * 0.055f)
    }

    private fun drawBrandBadges(canvas: Canvas, totalWidth: Float, yPos: Float, badgeHeight: Float) {
        val badgeWidth = totalWidth * 0.24f
        val spacing = totalWidth * 0.05f
        val startX = (totalWidth - (3 * badgeWidth + 2 * spacing)) / 2

        val logos = listOf(
            R.drawable.ic_logo_easypaisa,
            R.drawable.ic_logo_jazzcash,
            R.drawable.ic_logo_raast
        )

        logos.forEachIndexed { index, resId ->
            val left = startX + index * (badgeWidth + spacing)
            val rect = RectF(left, yPos, left + badgeWidth, yPos + badgeHeight)

            // Card container
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(rect, 8f, 8f, strokePaint)

            // Draw drawable inside
            val drawable = ContextCompat.getDrawable(context, resId)
            drawable?.let {
                val padding = (badgeHeight * 0.15f).toInt()
                it.setBounds(
                    (rect.left + padding).toInt(),
                    (rect.top + padding).toInt(),
                    (rect.right - padding).toInt(),
                    (rect.bottom - padding).toInt()
                )
                it.draw(canvas)
            }
        }
    }
}
