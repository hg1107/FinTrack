package com.fintrack.data.repository

import com.fintrack.data.local.dao.BankAccountDao
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.BankAccount
import com.fintrack.data.local.entity.Emi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankAccountRepository @Inject constructor(private val dao: BankAccountDao) {

    fun getAllAccounts(): Flow<List<BankAccount>> = dao.getAllAccounts()
    fun getAccountCount(): Flow<Int> = dao.getAccountCount()
    suspend fun getAccountById(id: Long): BankAccount? = dao.getAccountById(id)
    suspend fun insertAccount(account: BankAccount): Long = dao.insertAccount(account)
    suspend fun updateAccount(account: BankAccount) = dao.updateAccount(account)
    suspend fun deleteAccount(account: BankAccount) = dao.deleteAccount(account)

    fun getTransactionsForAccount(accountId: Long): Flow<List<AccountTransaction>> =
        dao.getTransactionsForAccount(accountId)
    fun getRecentTransactionsForAccount(accountId: Long, limit: Int = 5): Flow<List<AccountTransaction>> =
        dao.getRecentTransactionsForAccount(accountId, limit)
    fun getAllRecentTransactions(limit: Int = 5): Flow<List<AccountTransaction>> =
        dao.getAllRecentTransactions(limit)

    suspend fun insertTransaction(t: AccountTransaction): Long = dao.insertTransaction(t)
    suspend fun updateTransaction(t: AccountTransaction) = dao.updateTransaction(t)
    suspend fun deleteTransaction(t: AccountTransaction) = dao.deleteTransaction(t)

    fun getBalance(accountId: Long): Flow<Double> = dao.getBalance(accountId)
    fun getTotalBalance(): Flow<Double> = dao.getTotalBalance()

    fun getEmisForAccount(accountId: Long): Flow<List<Emi>> = dao.getEmisForAccount(accountId)
    suspend fun getAllActiveEmis(): List<Emi> = dao.getAllActiveEmis()
    fun getUpcomingUnpaidEmis(limit: Int = 3): Flow<List<Emi>> = dao.getUpcomingUnpaidEmis(limit)
    suspend fun insertEmi(emi: Emi): Long = dao.insertEmi(emi)
    suspend fun updateEmi(emi: Emi) = dao.updateEmi(emi)
    suspend fun deleteEmi(emi: Emi) = dao.deleteEmi(emi)
}
