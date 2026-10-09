package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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

        // Collect Payment
        val btnReceivePayment = findViewById<Button>(R.id.btnReceivePayment)
        val btnReplayHero = findViewById<Button>(R.id.btnReplayHero)
        val btnReceiptHero = findViewById<Button>(R.id.btnReceiptHero)

        // Merchant Tools
        val cardCustomerQr = findViewById<View>(R.id.cardCustomerQr)
        val cardVoiceMunshi = findViewById<View>(R.id.cardVoiceMunshi)
        val cardPdfLedger = findViewById<View>(R.id.cardPdfLedger)
        val cardSpeakerBoost = findViewById<View>(R.id.cardSpeakerBoost)

        // Subscription Card
        val cardSubscription = findViewById<View>(R.id.cardSubscription)

        updateStatusDisplay(tvStatusPill)

        btnReceivePayment.setOnClickListener {
            DynamicQrDialog(this).show()
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

        cardSubscription.setOnClickListener {
            showActivationDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        observeDashboardData()
    }

    private fun observeDashboardData() {
        val tvTodayTotal = findViewById<TextView>(R.id.tvTodayTotal)
        val tvTodayStatus = findViewById<TextView>(R.id.tvTodayStatus)
        val tvTodayCount = findViewById<TextView>(R.id.tvTodayCount)
        val tvLastPaymentValue = findViewById<TextView>(R.id.tvLastPaymentValue)

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
                // Today's Collections
                tvTodayTotal.text = "Rs. ${"%.2f".format(todayTotal)}"
                tvTodayCount.text = "$todayCount"

                if (todayCount > 0) {
                    tvTodayStatus.text = "✓ $todayCount payment${if (todayCount != 1) "s" else ""} received today"
                } else {
                    tvTodayStatus.text = "🕒 No payments received yet today"
                }

                // Last Payment Value
                if (latest != null) {
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(latest.timestamp))
                    tvLastPaymentValue.text = "Rs. ${latest.amount} (${latest.provider} • $timeStr)"
                } else {
                    tvLastPaymentValue.text = "—"
                }
            }
        }
    }

    private fun updateStatusDisplay(tvStatusPill: TextView) {
        val tvSubscriptionDays = findViewById<TextView>(R.id.tvSubscriptionDays)
        val isActive = subscriptionManager.isSubscriptionActive()
        val remainingDays = subscriptionManager.getRemainingDays()

        tvSubscriptionDays?.text = "$remainingDays days remaining"

        if (isActive) {
            tvStatusPill.text = "✓ Active"
            tvStatusPill.setBackgroundColor(android.graphics.Color.parseColor("#064E3B"))
            tvStatusPill.setTextColor(android.graphics.Color.parseColor("#19C878"))
        } else {
            tvStatusPill.text = "● Expired"
            tvStatusPill.setBackgroundColor(android.graphics.Color.parseColor("#7F1D1D"))
            tvStatusPill.setTextColor(android.graphics.Color.parseColor("#EF4444"))
            showActivationDialog()
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

    private fun showActivationDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_activation)

        val etCode = dialog.findViewById<EditText>(R.id.etActivationCode)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitActivation)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelActivation)
        val btnWhatsApp = dialog.findViewById<Button>(R.id.btnWhatsAppDeviceId)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnWhatsApp.setOnClickListener {
            openWhatsAppSupport()
        }

        btnSubmit.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (subscriptionManager.activateLicense(code)) {
                Toast.makeText(this, "License activated successfully!", Toast.LENGTH_LONG).show()
                val tvStatusPill = findViewById<TextView>(R.id.tvStatusPill)
                updateStatusDisplay(tvStatusPill)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Invalid or already used activation code.", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
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
