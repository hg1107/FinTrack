package com.fintrack.data.local.dao

import androidx.room.*
import com.fintrack.data.local.entity.CardTransaction
import com.fintrack.data.local.entity.CreditCard
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Per-card outstanding balance projection — used to populate list-screen without N+1 queries. */
data class CardOutstanding(val cardId: Long, val outstanding: Double)

@Dao
interface CreditCardDao {

    // --- Cards ---
    @Query("SELECT * FROM credit_cards ORDER BY createdDate DESC")
    fun getAllCards(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCardById(id: Long): CreditCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCard): Long

    @Update
    suspend fun updateCard(card: CreditCard)

    @Delete
    suspend fun deleteCard(card: CreditCard)

    @Query("SELECT COUNT(*) FROM credit_cards")
    fun getCardCount(): Flow<Int>

    // --- Transactions ---
    @Query("SELECT * FROM card_transactions WHERE cardId = :cardId ORDER BY date DESC")
    fun getTransactionsForCard(cardId: Long): Flow<List<CardTransaction>>

    @Query("SELECT * FROM card_transactions WHERE cardId = :cardId ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactionsForCard(cardId: Long, limit: Int = 5): Flow<List<CardTransaction>>

    @Query("SELECT * FROM card_transactions ORDER BY date DESC LIMIT :limit")
    fun getAllRecentTransactions(limit: Int = 10): Flow<List<CardTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CardTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: CardTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: CardTransaction)

    // Outstanding = sum of DEBIT - sum of CREDIT (per card)
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE -amount END), 0)
        FROM card_transactions WHERE cardId = :cardId
    """)
    fun getOutstandingBalance(cardId: Long): Flow<Double>

    // Global total outstanding
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE -amount END), 0)
        FROM card_transactions
    """)
    fun getTotalOutstanding(): Flow<Double>

    @Query("SELECT MAX(date) FROM card_transactions WHERE cardId = :cardId AND type = 'CREDIT'")
    fun getLastPaymentDate(cardId: Long): Flow<LocalDate?>

    /** Returns outstanding balance grouped by cardId — single query for list screen badges. */
    @Query("""
        SELECT cardId, 
               COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE -amount END), 0) AS outstanding
        FROM card_transactions 
        GROUP BY cardId
    """)
    fun getAllCardOutstandings(): Flow<List<CardOutstanding>>
}
