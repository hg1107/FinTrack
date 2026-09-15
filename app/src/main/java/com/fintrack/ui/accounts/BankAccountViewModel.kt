package com.fintrack.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.BankAccount
import com.fintrack.data.local.entity.Emi
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.data.repository.BankAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class AccountTransactionFilter { ALL, DEBITS, CREDITS }

data class BankUiState(
    val accounts: List<BankAccount> = emptyList(),
    val selectedAccount: BankAccount? = null,
    val transactions: List<AccountTransaction> = emptyList(),
    val emis: List<Emi> = emptyList(),
    val balance: Double = 0.0,
    val totalBalance: Double = 0.0,
    val upcomingEmi: Emi? = null,
    val transactionFilter: AccountTransactionFilter = AccountTransactionFilter.ALL,
    val searchQuery: String = "",
    val snackbarMessage: String? = null
)

@HiltViewModel
class BankAccountViewModel @Inject constructor(
    private val repository: BankAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BankUiState())
    val uiState: StateFlow<BankUiState> = _uiState.asStateFlow()

    /** Filtered + searched transactions, derived from state */
    val filteredTransactions: StateFlow<List<AccountTransaction>> = _uiState.map { state ->
        state.transactions
            .let { txns ->
                when (state.transactionFilter) {
                    AccountTransactionFilter.ALL     -> txns
                    AccountTransactionFilter.DEBITS  -> txns.filter { it.type == TransactionType.DEBIT }
                    AccountTransactionFilter.CREDITS -> txns.filter { it.type == TransactionType.CREDIT }
                }
            }
            .let { txns ->
                if (state.searchQuery.isBlank()) txns
                else txns.filter { it.note.contains(state.searchQuery, ignoreCase = true) }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Filtered bank accounts for the list screen (name search). */
    private val _accountSearchQuery = MutableStateFlow("")
    val accountSearchQuery: StateFlow<String> = _accountSearchQuery.asStateFlow()

    val filteredAccounts: StateFlow<List<BankAccount>> = combine(
        _uiState.map { it.accounts },
        _accountSearchQuery
    ) { accounts, query ->
        if (query.isBlank()) accounts
        else accounts.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            launch {
                repository.getAllAccounts().collect { accounts ->
                    _uiState.update { it.copy(accounts = accounts) }
                }
            }
            launch {
                repository.getTotalBalance().collect { total ->
                    _uiState.update { it.copy(totalBalance = total) }
                }
            }
        }
    }

    fun selectAccount(accountId: Long) {
        viewModelScope.launch {
            val account = repository.getAccountById(accountId) ?: return@launch
            _uiState.update { it.copy(selectedAccount = account,
                transactionFilter = AccountTransactionFilter.ALL, searchQuery = "") }

            launch {
                repository.getTransactionsForAccount(accountId).collect { txns ->
                    _uiState.update { it.copy(transactions = txns) }
                }
            }
            launch {
                repository.getBalance(accountId).collect { bal ->
                    _uiState.update { it.copy(balance = bal) }
                }
            }
            launch {
                repository.getEmisForAccount(accountId).collect { emis ->
                    val upcoming = emis.filter { !it.isPaid }.minByOrNull { it.dueDayOfMonth }
                    _uiState.update { it.copy(emis = emis, upcomingEmi = upcoming) }
                }
            }
        }
    }

    fun setTransactionFilter(f: AccountTransactionFilter) = _uiState.update { it.copy(transactionFilter = f) }
    fun setSearchQuery(q: String) = _uiState.update { it.copy(searchQuery = q) }
    fun setAccountSearchQuery(q: String) { _accountSearchQuery.value = q }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun saveAccount(name: String, lastFour: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.insertAccount(BankAccount(name = name.trim(), lastFourDigits = lastFour.trim()))
            onDone()
        }
    }

    fun updateAccount(account: BankAccount, name: String, lastFour: String) {
        viewModelScope.launch {
            repository.updateAccount(account.copy(name = name.trim(), lastFourDigits = lastFour.trim()))
        }
    }

    fun deleteAccount(account: BankAccount) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    fun addTransaction(accountId: Long, date: LocalDate, amount: Double, type: TransactionType, note: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                AccountTransaction(accountId = accountId, date = date, amount = amount, type = type, note = note.trim())
            )
            _uiState.update { it.copy(snackbarMessage = "Transaction saved") }
        }
    }

    fun updateTransaction(tx: AccountTransaction, date: LocalDate, amount: Double, type: TransactionType, note: String) {
        viewModelScope.launch {
            repository.updateTransaction(tx.copy(date = date, amount = amount, type = type, note = note.trim()))
            _uiState.update { it.copy(snackbarMessage = "Transaction updated") }
        }
    }

    fun deleteTransaction(tx: AccountTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _uiState.update { it.copy(snackbarMessage = "Transaction deleted") }
        }
    }

    fun saveEmi(
        accountId: Long, label: String, amount: Double,
        dueDayOfMonth: Int, tenureMonths: Int, startDate: LocalDate?,
        reminderDaysBefore: Int, isReminderEnabled: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.insertEmi(
                Emi(
                    accountId = accountId,
                    label = label.trim(),
                    amount = amount,
                    dueDayOfMonth = dueDayOfMonth,
                    tenureMonths = tenureMonths,
                    startDate = startDate,
                    reminderDaysBefore = reminderDaysBefore,
                    isReminderEnabled = isReminderEnabled
                )
            )
            _uiState.update { it.copy(snackbarMessage = "EMI \"${label.trim()}\" added") }
            onDone()
        }
    }

    fun updateEmi(
        emi: Emi, label: String, amount: Double, dueDayOfMonth: Int,
        tenureMonths: Int, startDate: LocalDate?,
        reminderDaysBefore: Int, isReminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.updateEmi(
                emi.copy(label = label.trim(), amount = amount, dueDayOfMonth = dueDayOfMonth,
                    tenureMonths = tenureMonths, startDate = startDate,
                    reminderDaysBefore = reminderDaysBefore, isReminderEnabled = isReminderEnabled)
            )
        }
    }

    fun toggleEmiPaid(emi: Emi) {
        viewModelScope.launch {
            val nowPaid = !emi.isPaid
            repository.updateEmi(
                emi.copy(isPaid = nowPaid, paidDate = if (nowPaid) LocalDate.now() else null)
            )
            // When marking as PAID, auto-log a DEBIT transaction on the linked bank account
            if (nowPaid) {
                repository.insertTransaction(
                    AccountTransaction(
                        accountId = emi.accountId,
                        date = LocalDate.now(),
                        amount = emi.amount,
                        type = TransactionType.DEBIT,
                        note = "EMI: ${emi.label}"
                    )
                )
                _uiState.update { it.copy(snackbarMessage = "EMI paid & ₹${emi.amount.toLong()} debited") }
            } else {
                _uiState.update { it.copy(snackbarMessage = "EMI marked unpaid") }
            }
        }
    }

    fun deleteEmi(emi: Emi) {
        viewModelScope.launch {
            repository.deleteEmi(emi)
            _uiState.update { it.copy(snackbarMessage = "EMI \"${emi.label}\" deleted") }
        }
    }
}
