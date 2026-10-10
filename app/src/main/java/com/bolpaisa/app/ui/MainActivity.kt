package com.bolpaisa.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.audio.NumberToWordsConverter
import com.bolpaisa.app.data.AppDatabase
import com.bolpaisa.app.data.TransactionEntity
import com.bolpaisa.app.licensing.MerchantProfileManager
import com.bolpaisa.app.licensing.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import com.bolpaisa.app.util.LocaleHelper
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var profileManager: MerchantProfileManager
    private lateinit var database: AppDatabase
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var transactionAdapter: TransactionAdapter
    private var activeShopSetupDialog: ShopSetupDialog? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)))
    }

    private val selectLogoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            activeShopSetupDialog?.updateLogoPreview(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscriptionManager = SubscriptionManager(this)
        profileManager = MerchantProfileManager(this)
        database = AppDatabase.getDatabase(this)
        audioPlayerManager = AudioPlayerManager(this)

        // Top App Bar
        val tvStatusPill = findViewById<TextView>(R.id.tvStatusPill)
        val btnAboutIcon = findViewById<ImageButton>(R.id.btnAboutIcon)
        val tvDashboardSubtitle = findViewById<TextView>(R.id.tvDashboardSubtitle)

        // Collect Payment
        val btnReceivePayment = findViewById<Button>(R.id.btnReceivePayment)
        val btnReplayHero = findViewById<Button>(R.id.btnReplayHero)
        val btnReceiptHero = findViewById<Button>(R.id.btnReceiptHero)

        // Merchant Tools Grid (8 Cards)
        val cardSummary = findViewById<View>(R.id.cardSummary)
        val cardHistory = findViewById<View>(R.id.cardHistory)
        val cardCustomerQr = findViewById<View>(R.id.cardCustomerQr)
        val cardVoiceSettings = findViewById<View>(R.id.cardVoiceSettings)
        val cardAppLanguage = findViewById<View>(R.id.cardAppLanguage)
        val cardSafeDemo = findViewById<View>(R.id.cardSafeDemo)
        val cardDiagnostic = findViewById<View>(R.id.cardDiagnostic)
        val cardShopProfile = findViewById<View>(R.id.cardShopProfile)

        // Subscription Card
        val cardSubscription = findViewById<View>(R.id.cardSubscription)

        // RecyclerView Today's Payment History
        val rvTodayTransactions = findViewById<RecyclerView>(R.id.rvTodayTransactions)
        val tvEmptyHistory = findViewById<TextView>(R.id.tvEmptyHistory)
        rvTodayTransactions.layoutManager = LinearLayoutManager(this)

        transactionAdapter = TransactionAdapter { trx ->
            replaySpecificPayment(trx)
        }
        rvTodayTransactions.adapter = transactionAdapter

        updateStatusDisplay(tvStatusPill)

        tvDashboardSubtitle?.text = profileManager.getShopName().ifEmpty { "Digital Soundbox" }

        btnAboutIcon?.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        btnReceivePayment.setOnClickListener {
            startActivity(Intent(this, QrGeneratorActivity::class.java))
        }

        btnReplayHero.setOnClickListener {
            replayLastPayment()
        }

        btnReceiptHero.setOnClickListener {
            promptAndSendWhatsAppReceipt()
        }

        cardSummary?.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        cardHistory?.setOnClickListener {
            startActivity(Intent(this, LedgerActivity::class.java))
        }

        cardCustomerQr?.setOnClickListener {
            startActivity(Intent(this, QrGeneratorActivity::class.java))
        }

        cardVoiceSettings?.setOnClickListener {
            startActivity(Intent(this, VoiceSettingsActivity::class.java))
        }

        cardAppLanguage?.setOnClickListener {
            startActivity(Intent(this, LanguageSettingsActivity::class.java))
        }

        cardSafeDemo?.setOnClickListener {
            startActivity(Intent(this, SafeDemoActivity::class.java))
        }

        cardDiagnostic?.setOnClickListener {
            startActivity(Intent(this, MerchantDiagnosticsActivity::class.java))
        }

        cardShopProfile?.setOnClickListener {
            promptShopProfileDialog()
        }

        cardSubscription?.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }

        checkAndPromptShopSetup()
        observeTodayTransactions(tvEmptyHistory)
    }

    private fun promptShopProfileDialog() {
        val dialog = ShopSetupDialog(
            this,
            onProfileSaved = {
                observeDashboardData()
            },
            onSelectLogoRequested = {
                selectLogoLauncher.launch("image/*")
            }
        )
        dialog.show()
    }

    private fun checkAndPromptShopSetup() {
        if (!profileManager.hasProfile()) {
            activeShopSetupDialog = ShopSetupDialog(
                this,
                onProfileSaved = {
                    observeDashboardData()
                },
                onSelectLogoRequested = {
                    selectLogoLauncher.launch("image/*")
                }
            )
            activeShopSetupDialog?.show()
        }
    }

    override fun onResume() {
        super.onResume()
        val tvDashboardSubtitle = findViewById<TextView>(R.id.tvDashboardSubtitle)
        tvDashboardSubtitle?.text = profileManager.getShopName().ifEmpty { "Digital Soundbox" }
        observeDashboardData()
    }

    private fun observeTodayTransactions(tvEmptyHistory: TextView) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = System.currentTimeMillis() + 86400000L

        lifecycleScope.launch {
            database.transactionDao().getTodayTransactionsFlow(startOfDay, endOfDay).collect { list ->
                if (list.isNotEmpty()) {
                    tvEmptyHistory.visibility = View.GONE
                    transactionAdapter.updateTransactions(list)
                } else {
                    tvEmptyHistory.visibility = View.VISIBLE
                    transactionAdapter.updateTransactions(emptyList())
                }
            }
        }
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
            val endOfDay = System.currentTimeMillis() + 86400000L

            val latest = database.transactionDao().getLatestTransaction()
            val todayTotal = database.transactionDao().getDailyTotal(startOfDay, endOfDay) ?: 0.0
            val todayCount = database.transactionDao().getDailyCount(startOfDay, endOfDay)

            withContext(Dispatchers.Main) {
                tvTodayTotal.text = "Rs. ${"%.2f".format(todayTotal)}"
                tvTodayCount.text = "$todayCount"

                if (todayCount > 0) {
                    tvTodayStatus.text = "✓ $todayCount payment${if (todayCount != 1) "s" else ""} received today"
                } else {
                    tvTodayStatus.text = "🕒 No payments received yet today"
                }

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
        val layoutExpiryWarning = findViewById<View>(R.id.layoutExpiryWarning)
        val tvExpiryWarningText = findViewById<TextView>(R.id.tvExpiryWarningText)

        val isActive = subscriptionManager.isSubscriptionActive()
        val remainingDays = subscriptionManager.getRemainingDays()

        tvSubscriptionDays?.text = "$remainingDays days remaining"

        if (isActive) {
            tvStatusPill.text = "✓ Active"
            tvStatusPill.setBackgroundColor(android.graphics.Color.parseColor("#064E3B"))
            tvStatusPill.setTextColor(android.graphics.Color.parseColor("#19C878"))

            if (remainingDays <= 5) {
                layoutExpiryWarning?.visibility = View.VISIBLE
                tvExpiryWarningText?.text = "⚠️ Subscription expiring in $remainingDays day${if (remainingDays != 1L) "s" else ""}! Tap to renew."
                layoutExpiryWarning?.setOnClickListener {
                    startActivity(Intent(this, SubscriptionActivity::class.java))
                }
            } else {
                layoutExpiryWarning?.visibility = View.GONE
            }
        } else {
            tvStatusPill.text = "● Expired"
            tvStatusPill.setBackgroundColor(android.graphics.Color.parseColor("#7F1D1D"))
            tvStatusPill.setTextColor(android.graphics.Color.parseColor("#EF4444"))

            layoutExpiryWarning?.visibility = View.VISIBLE
            tvExpiryWarningText?.text = "❌ Subscription Expired! Tap to renew now."
            layoutExpiryWarning?.setOnClickListener {
                startActivity(Intent(this, SubscriptionActivity::class.java))
            }
        }
    }

    private fun replaySpecificPayment(trx: TransactionEntity) {
        val tokens = mutableListOf<Int>()
        val dingRes = resources.getIdentifier("ding", "raw", packageName)
        if (dingRes != 0) tokens.add(dingRes)

        val providerResName = if (trx.provider.equals("JazzCash", true)) "jazzcash_par" else "easypaisa_par"
        val providerRes = resources.getIdentifier(providerResName, "raw", packageName)
        if (providerRes != 0) tokens.add(providerRes)

        tokens.addAll(NumberToWordsConverter.getUrduResIds(this, trx.amount))

        val rupayRes = resources.getIdentifier("rupay", "raw", packageName)
        if (rupayRes != 0) tokens.add(rupayRes)

        val wasoolRes = resources.getIdentifier("wasool_huay", "raw", packageName)
        if (wasoolRes != 0) tokens.add(wasoolRes)

        if (tokens.isNotEmpty()) {
            audioPlayerManager.playSequence(tokens)
        }
    }

    private fun replayLastPayment() {
        lifecycleScope.launch(Dispatchers.IO) {
            val latest = database.transactionDao().getLatestTransaction()
            withContext(Dispatchers.Main) {
                if (latest != null) {
                    replaySpecificPayment(latest)
                } else {
                    Toast.makeText(this@MainActivity, "No previous transaction to replay.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun promptAndSendWhatsAppReceipt() {
        lifecycleScope.launch(Dispatchers.IO) {
            val latest = database.transactionDao().getLatestTransaction()
            val shopName = profileManager.getShopName()
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
                        *$shopName - Digital Receipt*
                        Wasool Shuda: Rs. ${latest.amount}
                        Via: ${latest.provider}
                        Trx ID: ${latest.id}
                        Tareekh: $dateStr
                        Tasdeeq Shuda via BolPaisa Soundbox
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

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
