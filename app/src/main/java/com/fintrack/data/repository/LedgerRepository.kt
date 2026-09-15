package com.fintrack.data.repository

import com.fintrack.data.local.dao.LedgerDao
import com.fintrack.data.local.dao.LedgerSummary
import com.fintrack.data.local.entity.Ledger
import com.fintrack.data.local.entity.LedgerEntry
import com.fintrack.data.local.entity.LedgerSourceType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository @Inject constructor(private val dao: LedgerDao) {

    fun getAllLedgers(): Flow<List<Ledger>> = dao.getAllLedgers()
    fun getLedgerCount(): Flow<Int> = dao.getLedgerCount()
    suspend fun getLedgerById(id: Long): Ledger? = dao.getLedgerById(id)
    suspend fun getLedgerByName(name: String): Ledger? = dao.getLedgerByName(name)
    suspend fun insertLedger(ledger: Ledger): Long = dao.insertLedger(ledger)
    suspend fun updateLedger(ledger: Ledger) = dao.updateLedger(ledger)
    suspend fun deleteLedger(ledger: Ledger) = dao.deleteLedger(ledger)

    fun getEntriesForLedger(ledgerId: Long): Flow<List<LedgerEntry>> = dao.getEntriesForLedger(ledgerId)

    fun searchEntries(
        ledgerId: Long,
        query: String = "",
        from: String? = null,
        to: String? = null
    ): Flow<List<LedgerEntry>> = dao.searchEntries(ledgerId, query, from, to)

    suspend fun getEntryBySourceId(sourceType: LedgerSourceType, sourceId: Long): LedgerEntry? =
        dao.getEntryBySourceId(sourceType.name, sourceId)

    suspend fun insertEntry(entry: LedgerEntry): Long = dao.insertEntry(entry)
    suspend fun updateEntry(entry: LedgerEntry) = dao.updateEntry(entry)
    suspend fun deleteEntry(entry: LedgerEntry) = dao.deleteEntry(entry)
    suspend fun deleteEntryBySourceId(sourceType: LedgerSourceType, sourceId: Long) =
        dao.deleteEntryBySourceId(sourceType.name, sourceId)

    fun getMonthlyTotal(ledgerId: Long, month: Int, year: Int): Flow<Double> =
        dao.getMonthlyTotal(ledgerId, month.toString().padStart(2, '0'), year.toString())

    fun getYearlyTotal(ledgerId: Long, year: Int): Flow<Double> =
        dao.getYearlyTotal(ledgerId, year.toString())

    fun getTotalBalance(): Flow<Double> = dao.getTotalBalance()
    fun getAllLedgerSummaries(): Flow<List<LedgerSummary>> = dao.getAllLedgerSummaries()
}
