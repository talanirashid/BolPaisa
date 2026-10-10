package com.bolpaisa.app.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
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
import com.bolpaisa.app.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LedgerActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var audioPlayerManager: AudioPlayerManager

    private var allTransactions: List<TransactionEntity> = emptyList()
    private var currentFilterMode = "ALL" // ALL, TODAY, YESTERDAY
    private var currentSearchQuery = ""

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ledger)

        database = AppDatabase.getDatabase(this)
        audioPlayerManager = AudioPlayerManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etSearch = findViewById<EditText>(R.id.etSearchQuery)
        val rgFilter = findViewById<RadioGroup>(R.id.rgLedgerFilter)
        val rvHistory = findViewById<RecyclerView>(R.id.rvLedgerHistory)

        btnBack.setOnClickListener { finish() }

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter { trx ->
            showTransactionInspectDialog(trx)
        }
        rvHistory.adapter = adapter

        rgFilter.setOnCheckedChangeListener { _, checkedId ->
            currentFilterMode = when (checkedId) {
                R.id.rbToday -> "TODAY"
                R.id.rbYesterday -> "YESTERDAY"
                else -> "ALL"
            }
            applyFilters()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadAllTransactions()
    }

    private fun loadAllTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            database.transactionDao().getAllTransactions().collect { list ->
                allTransactions = list
                withContext(Dispatchers.Main) {
                    applyFilters()
                }
            }
        }
    }

    private fun applyFilters() {
        val tvSummary = findViewById<TextView>(R.id.tvFilteredSummary)

        val calendar = Calendar.getInstance()
        val startOfToday = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val startOfYesterday = startOfToday - 86400000L

        val periodFiltered = when (currentFilterMode) {
            "TODAY" -> allTransactions.filter { it.timestamp >= startOfToday }
            "YESTERDAY" -> allTransactions.filter { it.timestamp in startOfYesterday until startOfToday }
            else -> allTransactions
        }

        val searchFiltered = if (currentSearchQuery.isNotEmpty()) {
            periodFiltered.filter { trx ->
                trx.amount.toString().contains(currentSearchQuery) ||
                trx.provider.lowercase().contains(currentSearchQuery) ||
                (trx.senderName?.lowercase()?.contains(currentSearchQuery) == true) ||
                trx.id.toString().contains(currentSearchQuery)
            }
        } else {
            periodFiltered
        }

        val totalSum = searchFiltered.sumOf { it.amount }
        tvSummary.text = "${searchFiltered.size} Payments • Total: Rs. ${"%.2f".format(totalSum.toDouble())}"
        adapter.updateTransactions(searchFiltered)
    }

    private fun showTransactionInspectDialog(trx: TransactionEntity) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Payment Inspection Details")

        val dateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date(trx.timestamp))
        val sender = trx.senderName ?: "Unknown Customer"

        val detailsMsg = """
            Wasool Shuda: Rs. ${trx.amount}
            Gateway: ${trx.provider}
            Sender Name: $sender
            Transaction ID: ${trx.id}
            Tareekh: $dateStr
            Status: Verified via BolPaisa Soundbox
        """.trimIndent()

        builder.setMessage(detailsMsg)

        builder.setPositiveButton("Replay Audio") { _, _ ->
            replaySpecificPayment(trx)
        }
        builder.setNegativeButton("Close", null)
        builder.show()
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

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
