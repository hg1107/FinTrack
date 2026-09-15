package com.fintrack.data.repository

import com.fintrack.data.local.dao.CardOutstanding
import com.fintrack.data.local.dao.CreditCardDao
import com.fintrack.data.local.entity.CardTransaction
import com.fintrack.data.local.entity.CreditCard
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardRepository @Inject constructor(private val dao: CreditCardDao) {

    fun getAllCards(): Flow<List<CreditCard>> = dao.getAllCards()
    fun getCardCount(): Flow<Int> = dao.getCardCount()
    suspend fun getCardById(id: Long): CreditCard? = dao.getCardById(id)
    suspend fun insertCard(card: CreditCard): Long = dao.insertCard(card)
    suspend fun updateCard(card: CreditCard) = dao.updateCard(card)
    suspend fun deleteCard(card: CreditCard) = dao.deleteCard(card)

    fun getTransactionsForCard(cardId: Long): Flow<List<CardTransaction>> = dao.getTransactionsForCard(cardId)
    fun getRecentTransactionsForCard(cardId: Long, limit: Int = 5): Flow<List<CardTransaction>> =
        dao.getRecentTransactionsForCard(cardId, limit)
    fun getAllRecentTransactions(limit: Int = 10): Flow<List<CardTransaction>> = dao.getAllRecentTransactions(limit)

    suspend fun insertTransaction(t: CardTransaction): Long = dao.insertTransaction(t)
    suspend fun updateTransaction(t: CardTransaction) = dao.updateTransaction(t)
    suspend fun deleteTransaction(t: CardTransaction) = dao.deleteTransaction(t)

    fun getOutstandingBalance(cardId: Long): Flow<Double> = dao.getOutstandingBalance(cardId)
    fun getTotalOutstanding(): Flow<Double> = dao.getTotalOutstanding()
    fun getLastPaymentDate(cardId: Long): Flow<LocalDate?> = dao.getLastPaymentDate(cardId)

    /** Single-query per-card outstanding map for list screen display. */
    fun getAllCardOutstandings(): Flow<List<CardOutstanding>> = dao.getAllCardOutstandings()
}
