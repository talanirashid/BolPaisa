package com.bolpaisa.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val provider: String,
    val amount: Long,
    val senderName: String?,
    val timestamp: Long = System.currentTimeMillis()
)
