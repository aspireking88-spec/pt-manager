package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voucher_history")
data class VoucherHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val voucherId: String,
    val voucherCode: String,
    val packageName: String,
    val printedAt: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS",
    val printerName: String? = null,
    val accountId: String = "",
    val accountName: String? = null,
    val tenantName: String? = null,
    val groupName: String? = null
)
