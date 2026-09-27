package com.example.data.db

import kotlinx.coroutines.flow.Flow

class VoucherHistoryRepository(private val dao: VoucherHistoryDao) {
    val allHistory: Flow<List<VoucherHistoryEntity>> = dao.getAllHistory()

    fun historyForAccount(accountId: String): Flow<List<VoucherHistoryEntity>> =
        dao.getHistoryForAccount(accountId)

    suspend fun recordPrint(
        voucherId: String,
        voucherCode: String,
        packageName: String,
        printerName: String? = null,
        accountId: String = "",
        accountName: String? = null,
        tenantName: String? = null,
        groupName: String? = null
    ): Long {
        val entity = VoucherHistoryEntity(
            voucherId = voucherId,
            voucherCode = voucherCode,
            packageName = packageName,
            printerName = printerName,
            accountId = accountId,
            accountName = accountName,
            tenantName = tenantName,
            groupName = groupName
        )
        return dao.insertHistory(entity)
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun clear() = dao.clearAll()
}
