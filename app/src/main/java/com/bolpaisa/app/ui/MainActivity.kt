package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.audio.NumberToWordsConverter
import com.bolpaisa.app.data.AppDatabase
import com.bolpaisa.app.licensing.SubscriptionManager
import com.bolpaisa.app.reports.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var database: AppDatabase
    private lateinit var audioPlayerManager: AudioPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscriptionManager = SubscriptionManager(this)
        database = AppDatabase.getDatabase(this)
        audioPlayerManager = AudioPlayerManager(this)

        // Top App Bar
        val tvStatusPill = findViewById<TextView>(R.id.tvStatusPill)
        val btnAboutIcon = findViewById<ImageButton>(R.id.btnAboutIcon)

        // Hero Soundbox Card
        val btnReplayHero = findViewById<Button>(R.id.btnReplayHero)
        val btnReceiptHero = findViewById<Button>(R.id.btnReceiptHero)

        // Quick Action Grid
        val cardCustomerQr = findViewById<View>(R.id.cardCustomerQr)
        val cardVoiceMunshi = findViewById<View>(R.id.cardVoiceMunshi)
        val cardPdfLedger = findViewById<View>(R.id.cardPdfLedger)
        val cardSpeakerBoost = findViewById<View>(R.id.cardSpeakerBoost)

        // Quiet Footer
        val tvFooterDeviceInfo = findViewById<TextView>(R.id.tvFooterDeviceInfo)
        val btnFooterEnterKey = findViewById<Button>(R.id.btnFooterEnterKey)
        val btnFooterSupport = findViewById<Button>(R.id.btnFooterSupport)

        updateStatusDisplay(tvStatusPill, tvFooterDeviceInfo)

        btnAboutIcon.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        btnReplayHero.setOnClickListener {
            replayLastPayment()
        }

        btnReceiptHero.setOnClickListener {
            promptAndSendWhatsAppReceipt()
        }

        cardCustomerQr.setOnClickListener {
            DynamicQrDialog(this).show()
        }

        cardVoiceMunshi.setOnClickListener {
            runEveningMunshiAndReport()
        }

        cardPdfLedger.setOnClickListener {
            runEveningMunshiAndReport()
        }

        cardSpeakerBoost.setOnClickListener {
            Toast.makeText(this, "Speaker Boost Active: Keeping Bluetooth speaker awake every 25s", Toast.LENGTH_LONG).show()
        }

        btnFooterEnterKey.setOnClickListener {
            showActivationDialog(tvStatusPill, tvFooterDeviceInfo)
        }

        btnFooterSupport.setOnClickListener {
            openWhatsAppSupport()
        }
    }

    override fun onResume() {
        super.onResume()
        val tvHeroAmount = findViewById<TextView>(R.id.tvHeroAmount)
        val tvHeroDetails = findViewById<TextView>(R.id.tvHeroDetails)
        val tvTodayTotal = findViewById<TextView>(R.id.tvTodayTotal)
        val tvTodayCount = findViewById<TextView>(R.id.tvTodayCount)
        observeDashboardData(tvHeroAmount, tvHeroDetails, tvTodayTotal, tvTodayCount)
    }

    private fun observeDashboardData(
        tvHeroAmount: TextView,
        tvHeroDetails: TextView,
        tvTodayTotal: TextView,
        tvTodayCount: TextView
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val endOfDay = System.currentTimeMillis()

            val latest = database.transactionDao().getLatestTransaction()
            val todayTotal = database.transactionDao().getDailyTotal(startOfDay, endOfDay) ?: 0.0
            val todayCount = database.transactionDao().getDailyCount(startOfDay, endOfDay)

            withContext(Dispatchers.Main) {
                tvTodayTotal.text = "Rs. ${"%.2f".format(todayTotal)}"
                tvTodayCount.text = "$todayCount Payment${if (todayCount != 1) "s" else ""}"

                if (latest != null) {
                    tvHeroAmount.text = "Rs. ${latest.amount}"
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(latest.timestamp))
                    val sender = latest.senderName ?: "Customer"
                    tvHeroDetails.text = "${latest.provider} • $sender • $timeStr"
                } else {
                    tvHeroAmount.text = "Rs. 0"
                    tvHeroDetails.text = "Waiting for your next payment..."
                }
            }
        }
    }

    private fun updateStatusDisplay(tvStatusPill: TextView, tvFooterDeviceInfo: TextView) {
        val isActive = subscriptionManager.isSubscriptionActive()
        val remainingDays = subscriptionManager.getRemainingDays()
        val deviceId = subscriptionManager.getDeviceId()

        tvFooterDeviceInfo.text = "ID: ${deviceId.take(8)}... • ${remainingDays}d Left"

        if (isActive) {
            tvStatusPill.text = "● Active (${remainingDays}d)"
            tvStatusPill.setBackgroundColor(android.graphics.Color.parseColor("#064E3B"))
            tvStatusPill.setTextColor(android.graphics.Color.parseColor("#10B981"))
        } else {
            tvStatusPill.text = "● Expired"
            tvStatusPill.setBackgroundColor(android.graphics.Color.parseColor("#7F1D1D"))
            tvStatusPill.setTextColor(android.graphics.Color.parseColor("#EF4444"))
            showActivationDialog(tvStatusPill, tvFooterDeviceInfo)
        }
    }

    private fun replayLastPayment() {
        lifecycleScope.launch(Dispatchers.IO) {
            val latest = database.transactionDao().getLatestTransaction()
            withContext(Dispatchers.Main) {
                if (latest != null) {
                    val tokens = mutableListOf<Int>()
                    val dingRes = resources.getIdentifier("ding", "raw", packageName)
                    if (dingRes != 0) tokens.add(dingRes)

                    val providerResName = if (latest.provider.equals("JazzCash", true)) "jazzcash_par" else "easypaisa_par"
                    val providerRes = resources.getIdentifier(providerResName, "raw", packageName)
                    if (providerRes != 0) tokens.add(providerRes)

                    tokens.addAll(NumberToWordsConverter.getUrduResIds(this@MainActivity, latest.amount))

                    val rupayRes = resources.getIdentifier("rupay", "raw", packageName)
                    if (rupayRes != 0) tokens.add(rupayRes)

                    val wasoolRes = resources.getIdentifier("wasool_huay", "raw", packageName)
                    if (wasoolRes != 0) tokens.add(wasoolRes)

                    if (tokens.isNotEmpty()) {
                        audioPlayerManager.playSequence(tokens)
                    }
                } else {
                    Toast.makeText(this@MainActivity, "No previous transaction to replay.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun runEveningMunshiAndReport() {
        lifecycleScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val endOfDay = System.currentTimeMillis()

            val total = database.transactionDao().getDailyTotal(startOfDay, endOfDay) ?: 0.0
            val transactions = database.transactionDao().getMonthlyPayments(startOfDay, endOfDay)

            withContext(Dispatchers.Main) {
                val tokens = mutableListOf<Int>()
                val dingRes = resources.getIdentifier("ding", "raw", packageName)
                if (dingRes != 0) tokens.add(dingRes)

                tokens.addAll(NumberToWordsConverter.getUrduResIds(this@MainActivity, total.toLong()))

                val rupayRes = resources.getIdentifier("rupay", "raw", packageName)
                if (rupayRes != 0) tokens.add(rupayRes)

                val wasoolRes = resources.getIdentifier("wasool_huay", "raw", packageName)
                if (wasoolRes != 0) tokens.add(wasoolRes)

                if (tokens.isNotEmpty()) {
                    audioPlayerManager.playSequence(tokens)
                }

                PdfReportGenerator.generateDailyReport(this@MainActivity, transactions, total)
            }
        }
    }

    private fun promptAndSendWhatsAppReceipt() {
        lifecycleScope.launch(Dispatchers.IO) {
            val latest = database.transactionDao().getLatestTransaction()
            withContext(Dispatchers.Main) {
                if (latest == null) {
                    Toast.makeText(this@MainActivity, "No recent payment found to send receipt.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Send Digital WhatsApp Receipt")

                val input = EditText(this@MainActivity).apply {
                    hint = "Enter Customer Mobile Number (e.g. 03001234567)"
                    setPadding(40, 40, 40, 40)
                }
                builder.setView(input)

                builder.setPositiveButton("Send Receipt") { _, _ ->
                    var phone = input.text.toString().trim()
                    if (phone.startsWith("0")) {
                        phone = "92" + phone.substring(1)
                    }

                    val dateStr = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(Date(latest.timestamp))
                    val receiptText = """
                        *BolPaisa Digital Receipt*
                        Dukaan: BolPaisa Merchant
                        Wasool Shuda Raqam: Rs. ${latest.amount}
                        Transaction ID: ${latest.id}
                        Tareekh: $dateStr
                        Status: Wasool Shuda (Verified via BolPaisa)
                        Shukriya!
                    """.trimIndent()

                    val url = "https://wa.me/$phone?text=${Uri.encode(receiptText)}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("Cancel", null)
                builder.show()
            }
        }
    }

    private fun showActivationDialog(tvStatusPill: TextView, tvFooterDeviceInfo: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Activate BolPaisa License")

        val input = EditText(this).apply {
            hint = "Enter 8-digit activation code (e.g. A3F8-9B2C)"
            setPadding(40, 40, 40, 40)
        }
        builder.setView(input)

        builder.setPositiveButton("Activate") { _, _ ->
            val code = input.text.toString()
            if (subscriptionManager.activateLicense(code)) {
                Toast.makeText(this, "License activated successfully!", Toast.LENGTH_LONG).show()
                updateStatusDisplay(tvStatusPill, tvFooterDeviceInfo)
            } else {
                Toast.makeText(this, "Invalid or already used activation code.", Toast.LENGTH_LONG).show()
            }
        }

        builder.setNeutralButton("WhatsApp Device ID") { _, _ ->
            openWhatsAppSupport()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun openWhatsAppSupport() {
        val phoneNumber = "923336366291"
        val deviceId = subscriptionManager.getDeviceId()
        val message = "Mene Rs. 150 bhej diye hain.\nMera Device ID: $deviceId\nTrx ID (TID): "
        val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
