package com.fintrack.data.local.dao

import androidx.room.*
import com.fintrack.data.local.entity.Ledger
import com.fintrack.data.local.entity.LedgerEntry
import com.fintrack.data.local.entity.LedgerSourceType
import kotlinx.coroutines.flow.Flow

/** Per-ledger summary projection — used for the list screen. */
data class LedgerSummary(
    val ledgerId: Long,
    val total: Double,
    val entryCount: Int
)

@Dao
interface LedgerDao {

    // --- Ledgers ---
    @Query("SELECT * FROM ledgers WHERE isArchived = 0 ORDER BY createdDate ASC")
    fun getAllLedgers(): Flow<List<Ledger>>

    @Query("SELECT * FROM ledgers WHERE id = :id")
    suspend fun getLedgerById(id: Long): Ledger?

    @Query("SELECT * FROM ledgers WHERE name = :name LIMIT 1")
    suspend fun getLedgerByName(name: String): Ledger?

    @Query("SELECT COUNT(*) FROM ledgers WHERE isArchived = 0")
    fun getLedgerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: Ledger): Long

    @Update
    suspend fun updateLedger(ledger: Ledger)

    @Delete
    suspend fun deleteLedger(ledger: Ledger)

    // --- Ledger Entries ---
    @Query("SELECT * FROM ledger_entries WHERE ledgerId = :ledgerId ORDER BY date DESC")
    fun getEntriesForLedger(ledgerId: Long): Flow<List<LedgerEntry>>

    @Query("""
        SELECT * FROM ledger_entries
        WHERE ledgerId = :ledgerId
          AND (:query = '' OR note LIKE '%' || :query || '%')
          AND (:from IS NULL OR date >= :from)
          AND (:to IS NULL OR date <= :to)
        ORDER BY date DESC
    """)
    fun searchEntries(
        ledgerId: Long,
        query: String = "",
        from: String? = null,
        to: String? = null
    ): Flow<List<LedgerEntry>>

    @Query("""
        SELECT * FROM ledger_entries
        WHERE sourceType = :sourceType AND sourceId = :sourceId
        LIMIT 1
    """)
    suspend fun getEntryBySourceId(sourceType: String, sourceId: Long): LedgerEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry): Long

    @Update
    suspend fun updateEntry(entry: LedgerEntry)

    @Delete
    suspend fun deleteEntry(entry: LedgerEntry)

    @Query("DELETE FROM ledger_entries WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun deleteEntryBySourceId(sourceType: String, sourceId: Long)

    // --- Aggregates ---
    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM ledger_entries
        WHERE ledgerId = :ledgerId
          AND strftime('%m', date) = :monthStr
          AND strftime('%Y', date) = :yearStr
    """)
    fun getMonthlyTotal(ledgerId: Long, monthStr: String, yearStr: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM ledger_entries
        WHERE ledgerId = :ledgerId
          AND strftime('%Y', date) = :yearStr
    """)
    fun getYearlyTotal(ledgerId: Long, yearStr: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger_entries")
    fun getTotalBalance(): Flow<Double>

    /** Per-ledger totals and counts — single query for list screen. */
    @Query("""
        SELECT ledgerId,
               COALESCE(SUM(amount), 0) AS total,
               COUNT(*) AS entryCount
        FROM ledger_entries
        GROUP BY ledgerId
    """)
    fun getAllLedgerSummaries(): Flow<List<LedgerSummary>>
}
