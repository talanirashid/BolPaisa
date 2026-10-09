package com.bolpaisa.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bolpaisa.app.R
import com.bolpaisa.app.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<TransactionEntity> = emptyList(),
    private val onReplayClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    fun updateTransactions(newTransactions: List<TransactionEntity>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBadge: TextView = itemView.findViewById(R.id.tvBadge)
        private val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
        private val tvTransactionMeta: TextView = itemView.findViewById(R.id.tvTransactionMeta)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val btnReplayItem: ImageButton = itemView.findViewById(R.id.btnReplayItem)

        fun bind(trx: TransactionEntity) {
            val providerUpper = trx.provider.uppercase()
            val badgeText = when {
                providerUpper.contains("EASYPAISA") -> "EP"
                providerUpper.contains("JAZZCASH") -> "JC"
                else -> "RT"
            }
            tvBadge.text = badgeText

            tvSenderName.text = trx.senderName?.ifEmpty { trx.provider } ?: trx.provider

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = timeFormat.format(Date(trx.timestamp))
            tvTransactionMeta.text = "${trx.provider} • $timeStr"

            tvAmount.text = "+ Rs. ${trx.amount}"

            btnReplayItem.setOnClickListener {
                onReplayClick(trx)
            }
        }
    }
}
