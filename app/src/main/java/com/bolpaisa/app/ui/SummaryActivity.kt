package com.bolpaisa.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bolpaisa.app.R
import com.bolpaisa.app.audio.AudioPlayerManager
import com.bolpaisa.app.audio.NumberToWordsConverter
import com.bolpaisa.app.data.AppDatabase
import com.bolpaisa.app.data.TransactionEntity
import com.bolpaisa.app.reports.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class SummaryActivity : BaseActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var audioPlayerManager: AudioPlayerManager

    private var currentFilter = "DAILY" // DAILY, WEEKLY, MONTHLY, YEARLY
    private var currentList: List<TransactionEntity> = emptyList()
    private var currentTotal = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        database = AppDatabase.getDatabase(this)
        audioPlayerManager = AudioPlayerManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rgPeriod = findViewById<RadioGroup>(R.id.rgPeriodSelector)
        val rbDaily = findViewById<RadioButton>(R.id.rbDaily)
        val rbWeekly = findViewById<RadioButton>(R.id.rbWeekly)
        val rbMonthly = findViewById<RadioButton>(R.id.rbMonthly)
        val rbYearly = findViewById<RadioButton>(R.id.rbYearly)
        val btnExportPdf = findViewById<Button>(R.id.btnExportPdf)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvSummaryTransactions)

        btnBack.setOnClickListener { finish() }

        rvTransactions.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter { trx ->
            replaySpecificPayment(trx)
        }
        rvTransactions.adapter = adapter

        rgPeriod.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.rbWeekly -> "WEEKLY"
                R.id.rbMonthly -> "MONTHLY"
                R.id.rbYearly -> "YEARLY"
                else -> "DAILY"
            }
            loadSummaryData()
        }

        btnExportPdf.setOnClickListener {
            PdfReportGenerator.generateDailyReport(this, currentList, currentTotal)
        }

        loadSummaryData()
    }

    private fun loadSummaryData() {
        val tvTotal = findViewById<TextView>(R.id.tvSummaryTotal)
        val tvCount = findViewById<TextView>(R.id.tvSummaryCount)
        val tvBreakdown = findViewById<TextView>(R.id.tvGatewayBreakdown)

        lifecycleScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val now = System.currentTimeMillis()

            val startOfPeriod = when (currentFilter) {
                "WEEKLY" -> {
                    calendar.apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }
                "MONTHLY" -> {
                    calendar.apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }
                "YEARLY" -> {
                    calendar.apply {
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }
                else -> { // DAILY
                    calendar.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }
            }

            val list = database.transactionDao().getMonthlyPayments(startOfPeriod, now)
            var total = 0.0
            var epTotal = 0.0
            var jcTotal = 0.0
            var rtTotal = 0.0

            for (trx in list) {
                total += trx.amount
                val providerUpper = trx.provider.uppercase()
                when {
                    providerUpper.contains("EASYPAISA") -> epTotal += trx.amount
                    providerUpper.contains("JAZZCASH") -> jcTotal += trx.amount
                    else -> rtTotal += trx.amount
                }
            }

            currentList = list
            currentTotal = total

            withContext(Dispatchers.Main) {
                tvTotal.text = "Rs. ${"%.2f".format(total)}"
                tvCount.text = "${list.size} Payment${if (list.size != 1) "s" else ""}"
                tvBreakdown.text = "Easypaisa: Rs. ${epTotal.toLong()} • JazzCash: Rs. ${jcTotal.toLong()} • Raast: Rs. ${rtTotal.toLong()}"
                adapter.updateTransactions(list)
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

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
