package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherHistoryDao {
    @Query("SELECT * FROM voucher_history ORDER BY printedAt DESC")
    fun getAllHistory(): Flow<List<VoucherHistoryEntity>>

    @Query("SELECT * FROM voucher_history WHERE accountId = :accountId ORDER BY printedAt DESC")
    fun getHistoryForAccount(accountId: String): Flow<List<VoucherHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: VoucherHistoryEntity): Long

    @Query("DELETE FROM voucher_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM voucher_history")
    suspend fun clearAll()
}
