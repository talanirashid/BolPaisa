package com.bolpaisa.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getTodayTransactionsFlow(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTransaction(): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun getDailyTotal(startOfDay: Long, endOfDay: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun getDailyCount(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfMonth AND timestamp <= :endOfMonth ORDER BY timestamp DESC")
    suspend fun getMonthlyPayments(startOfMonth: Long, endOfMonth: Long): List<TransactionEntity>
}
