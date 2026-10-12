package com.bolpaisa.app.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.bolpaisa.app.R
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.bolpaisa.app.util.FeedbackHelper
import com.bolpaisa.app.util.PaymentQrRouter
import com.bolpaisa.app.util.PaymentRail
import com.bolpaisa.app.util.RaastQrEncoder
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class QrGeneratorActivity : BaseActivity() {

    private lateinit var profileManager: MerchantProfileManager
    private var selectedRail = PaymentRail.EASYPAISA
    private var activeGeneratedBitmap: Bitmap? = null
    private var expiryTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_qr_generator)

        profileManager = MerchantProfileManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rgGateway = findViewById<RadioGroup>(R.id.rgWalletSelector)
        val rbEasypaisa = findViewById<RadioButton>(R.id.rbEasypaisa)
        val rbJazzCash = findViewById<RadioButton>(R.id.rbJazzCash)
        val rbRaast = findViewById<RadioButton>(R.id.rbRaast)
        val tvBanner = findViewById<TextView>(R.id.tvReceivingAccountBanner)
        val etAmount = findViewById<EditText>(R.id.etAmountInput)
        val btnChip500 = findViewById<Button>(R.id.btnChip500)
        val btnChip1000 = findViewById<Button>(R.id.btnChip1000)
        val btnChip5000 = findViewById<Button>(R.id.btnChip5000)
        val btnChipClear = findViewById<Button>(R.id.btnChipClear)
        val btnGenerate = findViewById<Button>(R.id.btnGenerateQr)
        val layoutResult = findViewById<View>(R.id.layoutQrResult)
        val ivQrRailLogo = findViewById<ImageView>(R.id.ivQrRailLogo)
        val tvQrShopName = findViewById<TextView>(R.id.tvQrShopName)
        val tvQrAmount = findViewById<TextView>(R.id.tvQrAmount)
        val tvQrExpiryTimer = findViewById<TextView>(R.id.tvQrExpiryTimer)
        val ivQrImage = findViewById<ImageView>(R.id.ivQrImage)
        val tvQrTillId = findViewById<TextView>(R.id.tvQrTillId)
        val tvQrInstruction = findViewById<TextView>(R.id.tvQrInstruction)
        val btnShareQr = findViewById<Button>(R.id.btnShareQr)
        val btnDownloadQr = findViewById<Button>(R.id.btnDownloadQr)
        val btnDone = findViewById<Button>(R.id.btnDoneQr)

        btnBack.setOnClickListener { finish() }

        val addQuickAmount: (Double) -> Unit = { added ->
            val current = etAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
            val total = current + added
            etAmount.setText(if (total % 1.0 == 0.0) total.toLong().toString() else total.toString())
        }

        btnChip500?.setOnClickListener { addQuickAmount(500.0) }
        btnChip1000?.setOnClickListener { addQuickAmount(1000.0) }
        btnChip5000?.setOnClickListener { addQuickAmount(5000.0) }
        btnChipClear?.setOnClickListener { etAmount.setText("") }

        selectedRail = PaymentRail.fromCode(profileManager.getGatewayType())
        when (selectedRail) {
            PaymentRail.JAZZCASH -> rbJazzCash.isChecked = true
            PaymentRail.RAAST -> rbRaast.isChecked = true
            else -> rbEasypaisa.isChecked = true
        }
        updateGatewayUi(selectedRail, rbEasypaisa, rbJazzCash, rbRaast, tvBanner)

        rgGateway.setOnCheckedChangeListener { _, checkedId ->
            selectedRail = when (checkedId) {
                R.id.rbJazzCash -> PaymentRail.JAZZCASH
                R.id.rbRaast -> PaymentRail.RAAST
                else -> PaymentRail.EASYPAISA
            }
            updateGatewayUi(selectedRail, rbEasypaisa, rbJazzCash, rbRaast, tvBanner)
        }

        tvBanner.setOnClickListener {
            promptSetGatewayId(selectedRail.code) {
                updateGatewayUi(selectedRail, rbEasypaisa, rbJazzCash, rbRaast, tvBanner)
            }
        }

        btnGenerate.setOnClickListener {
            var amountStr = etAmount.text.toString().trim()
            if (amountStr.startsWith("Rs.", true)) {
                amountStr = amountStr.substring(3).trim()
            }
            val amount = amountStr.toDoubleOrNull()

            val gatewayId = profileManager.getGatewayId(selectedRail.code)
            if (gatewayId.isEmpty()) {
                promptSetGatewayId(selectedRail.code) {
                    updateGatewayUi(selectedRail, rbEasypaisa, rbJazzCash, rbRaast, tvBanner)
                }
                return@setOnClickListener
            }

            if (!PaymentQrRouter.isValidIdentifier(gatewayId)) {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Invalid $selectedRail ID length. Must be 5-8 digit Till or 11 digit Mobile.")
                return@setOnClickListener
            }

            val shopName = profileManager.getShopName()

            // Resolve multi-rail EMVCo ISO/IEC 18004 & Raast/SBP compliant QR code payload
            val qrResult = PaymentQrRouter.resolvePaymentPayload(
                rail = selectedRail,
                identifier = gatewayId,
                recipientName = shopName,
                amount = amount
            )

            val bitmap = RaastQrEncoder.renderQrBitmap(qrResult.rawPayload, 600)
            if (bitmap != null) {
                activeGeneratedBitmap = bitmap
                ivQrImage.setImageBitmap(bitmap)

                // Set active payment rail logo badge
                val logoRes = when (selectedRail) {
                    PaymentRail.JAZZCASH -> R.drawable.ic_logo_jazzcash
                    PaymentRail.RAAST -> R.drawable.ic_logo_raast
                    else -> R.drawable.ic_logo_easypaisa
                }
                ivQrRailLogo?.setImageResource(logoRes)

                // Render Shop Name on TOP above QR
                tvQrShopName.text = shopName.uppercase(Locale.ROOT)
                tvQrAmount.text = if (amount != null && amount > 0.0) "Payable Amount: Rs. ${"%.2f".format(amount)}" else "Static: Customer Enters Amount"

                // Start 3-minute dynamic QR expiry timer
                expiryTimer?.cancel()
                if (amount != null && amount > 0.0) {
                    tvQrExpiryTimer?.visibility = View.VISIBLE
                    expiryTimer = object : CountDownTimer(180_000L, 1000L) {
                        override fun onTick(millisUntilFinished: Long) {
                            val seconds = millisUntilFinished / 1000
                            tvQrExpiryTimer?.text = String.format(Locale.US, "Expires in %02d:%02d", seconds / 60, seconds % 60)
                        }

                        override fun onFinish() {
                            tvQrExpiryTimer?.visibility = View.GONE
                            FeedbackHelper.showInfo(findViewById(android.R.id.content), "Dynamic QR expired. Reset to Static QR.")
                            btnGenerate.performClick()
                        }
                    }.start()
                } else {
                    tvQrExpiryTimer?.visibility = View.GONE
                }

                // Render Till ID / Account Number BELOW QR
                tvQrTillId.text = qrResult.footerLabel
                tvQrInstruction.text = "Scan & Pay using any Mobile Banking or Wallet App"

                layoutResult.visibility = View.VISIBLE
                FeedbackHelper.showSuccess(findViewById(android.R.id.content), "Customer Payment QR generated!")
            } else {
                FeedbackHelper.showError(findViewById(android.R.id.content), "Failed to generate QR code")
            }
        }

        btnShareQr?.setOnClickListener {
            activeGeneratedBitmap?.let { bmp ->
                shareQrBitmap(bmp)
            }
        }

        btnDownloadQr?.setOnClickListener {
            activeGeneratedBitmap?.let { bmp ->
                saveQrToGallery(bmp)
            }
        }

        btnDone.setOnClickListener {
            expiryTimer?.cancel()
            layoutResult.visibility = View.GONE
            etAmount.setText("")
        }
    }

    private fun shareQrBitmap(bitmap: Bitmap) {
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream(File(cachePath, "BolPaisa_QR.png"))
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(cacheDir, "images")
            val newFile = File(imagePath, "BolPaisa_QR.png")
            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(shareIntent, "Share Payment QR"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FeedbackHelper.showError(findViewById(android.R.id.content), "Failed to share QR image")
        }
    }

    private fun saveQrToGallery(bitmap: Bitmap) {
        try {
            val fileName = "BolPaisa_QR_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BolPaisa")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    val out = contentResolver.openOutputStream(uri)
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.close()
                        FeedbackHelper.showSuccess(findViewById(android.R.id.content), "QR saved to Pictures/BolPaisa")
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BolPaisa")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                FeedbackHelper.showSuccess(findViewById(android.R.id.content), "QR saved to Pictures/BolPaisa")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FeedbackHelper.showError(findViewById(android.R.id.content), "Failed to save QR to Gallery")
        }
    }

    private fun updateGatewayUi(
        rail: PaymentRail,
        rbEasypaisa: RadioButton,
        rbJazzCash: RadioButton,
        rbRaast: RadioButton,
        tvBanner: TextView
    ) {
        val activeBg = Color.parseColor(rail.brandColorHex)
        val inactiveBg = Color.parseColor("#1E293B")

        rbEasypaisa.setBackgroundColor(if (rail == PaymentRail.EASYPAISA) activeBg else inactiveBg)
        rbJazzCash.setBackgroundColor(if (rail == PaymentRail.JAZZCASH) activeBg else inactiveBg)
        rbRaast.setBackgroundColor(if (rail == PaymentRail.RAAST) activeBg else inactiveBg)

        val id = profileManager.getGatewayId(rail.code)
        if (id.isNotEmpty()) {
            tvBanner.text = "Receiving on: ${rail.displayName} ($id)"
            tvBanner.setTextColor(Color.parseColor("#10B981"))
        } else {
            tvBanner.text = "Tap to set ${rail.displayName} Till ID / Number"
            tvBanner.setTextColor(Color.parseColor("#EF4444"))
        }
    }

    private fun promptSetGatewayId(gatewayCode: String, onSaved: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set $gatewayCode Till/Account ID")

        val input = EditText(this).apply {
            hint = "Enter Till ID or Mobile Account Number"
            setPadding(40, 40, 40, 40)
            setText(profileManager.getGatewayId(gatewayCode))
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val enteredId = input.text.toString().trim()
            if (enteredId.isNotEmpty()) {
                profileManager.saveGatewayId(gatewayCode, enteredId)
                FeedbackHelper.showSuccess(findViewById(android.R.id.content), "$gatewayCode ID saved!")
                onSaved()
            } else {
                FeedbackHelper.showError(findViewById(android.R.id.content), "ID cannot be empty")
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        expiryTimer?.cancel()
    }
}
