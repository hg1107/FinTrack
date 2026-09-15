package com.fintrack.data.local.dao

import androidx.room.*
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.BankAccount
import com.fintrack.data.local.entity.Emi
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {

    // --- Accounts ---
    @Query("SELECT * FROM bank_accounts ORDER BY createdDate DESC")
    fun getAllAccounts(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccount): Long

    @Update
    suspend fun updateAccount(account: BankAccount)

    @Delete
    suspend fun deleteAccount(account: BankAccount)

    @Query("SELECT COUNT(*) FROM bank_accounts")
    fun getAccountCount(): Flow<Int>

    // --- Transactions ---
    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactionsForAccount(accountId: Long, limit: Int = 5): Flow<List<AccountTransaction>>

    /** Global recent transactions across all accounts — used for the Home screen dashboard. */
    @Query("SELECT * FROM account_transactions ORDER BY date DESC LIMIT :limit")
    fun getAllRecentTransactions(limit: Int = 5): Flow<List<AccountTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AccountTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: AccountTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: AccountTransaction)

    // Balance = sum of CREDIT - sum of DEBIT
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0)
        FROM account_transactions WHERE accountId = :accountId
    """)
    fun getBalance(accountId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0)
        FROM account_transactions
    """)
    fun getTotalBalance(): Flow<Double>

    // --- EMIs ---
    @Query("SELECT * FROM emis WHERE accountId = :accountId ORDER BY dueDayOfMonth ASC")
    fun getEmisForAccount(accountId: Long): Flow<List<Emi>>

    /**
     * Fetches EMIs that have reminders enabled AND are not yet paid.
     * (Was previously only filtering on isReminderEnabled, fetching paid EMIs unnecessarily.)
     */
    @Query("SELECT * FROM emis WHERE isReminderEnabled = 1 AND isPaid = 0")
    suspend fun getAllActiveEmis(): List<Emi>

    /** Upcoming unpaid EMIs across all accounts — used for the Home screen dashboard widget. */
    @Query("SELECT * FROM emis WHERE isPaid = 0 ORDER BY dueDayOfMonth ASC LIMIT :limit")
    fun getUpcomingUnpaidEmis(limit: Int = 3): Flow<List<Emi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmi(emi: Emi): Long

    @Update
    suspend fun updateEmi(emi: Emi)

    @Delete
    suspend fun deleteEmi(emi: Emi)
}
