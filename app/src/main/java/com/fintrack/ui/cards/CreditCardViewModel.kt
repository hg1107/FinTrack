package com.fintrack.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.data.local.entity.CardTransaction
import com.fintrack.data.local.entity.CreditCard
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TransactionFilter { ALL, DEBITS, CREDITS }

data class CardUiState(
    val cards: List<CreditCard> = emptyList(),
    val selectedCard: CreditCard? = null,
    val transactions: List<CardTransaction> = emptyList(),
    val outstandingBalance: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val cardOutstandingMap: Map<Long, Double> = emptyMap(),   // per-card outstanding for list screen
    val lastPaymentDate: LocalDate? = null,
    val showAllTransactions: Boolean = false,
    val transactionFilter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val snackbarMessage: String? = null
)

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    /** Filtered + searched transactions, derived from state */
    val filteredTransactions: StateFlow<List<CardTransaction>> = _uiState.map { state ->
        state.transactions
            .let { txns ->
                when (state.transactionFilter) {
                    TransactionFilter.ALL     -> txns
                    TransactionFilter.DEBITS  -> txns.filter { it.type == TransactionType.DEBIT }
                    TransactionFilter.CREDITS -> txns.filter { it.type == TransactionType.CREDIT }
                }
            }
            .let { txns ->
                if (state.searchQuery.isBlank()) txns
                else txns.filter { it.note.contains(state.searchQuery, ignoreCase = true) }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            launch {
                repository.getAllCards().collect { cards ->
                    _uiState.update { it.copy(cards = cards) }
                }
            }
            launch {
                repository.getTotalOutstanding().collect { total ->
                    _uiState.update { it.copy(totalOutstanding = total) }
                }
            }
            launch {
                repository.getAllCardOutstandings().collect { list ->
                    _uiState.update { it.copy(cardOutstandingMap = list.associate { it.cardId to it.outstanding }) }
                }
            }
        }
    }

    fun selectCard(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId) ?: return@launch
            _uiState.update { it.copy(selectedCard = card, showAllTransactions = false,
                transactionFilter = TransactionFilter.ALL, searchQuery = "") }

            launch {
                repository.getTransactionsForCard(cardId).collect { txns ->
                    _uiState.update { it.copy(transactions = txns) }
                }
            }
            launch {
                repository.getOutstandingBalance(cardId).collect { bal ->
                    _uiState.update { it.copy(outstandingBalance = bal) }
                }
            }
            launch {
                repository.getLastPaymentDate(cardId).collect { date ->
                    _uiState.update { it.copy(lastPaymentDate = date) }
                }
            }
        }
    }

    fun toggleShowAllTransactions() = _uiState.update { it.copy(showAllTransactions = !it.showAllTransactions) }
    fun setTransactionFilter(f: TransactionFilter) = _uiState.update { it.copy(transactionFilter = f) }
    fun setSearchQuery(q: String) = _uiState.update { it.copy(searchQuery = q) }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun saveCard(name: String, lastFour: String, creditLimit: Double, billingDueDay: Int, statementDay: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.insertCard(
                CreditCard(name = name.trim(), lastFourDigits = lastFour.trim(),
                    creditLimit = creditLimit, billingDueDay = billingDueDay, statementDay = statementDay)
            )
            onDone()
        }
    }

    fun updateCard(card: CreditCard, name: String, lastFour: String, creditLimit: Double, billingDueDay: Int, statementDay: Int) {
        viewModelScope.launch {
            repository.updateCard(card.copy(name = name.trim(), lastFourDigits = lastFour.trim(),
                creditLimit = creditLimit, billingDueDay = billingDueDay, statementDay = statementDay))
            _uiState.update { it.copy(selectedCard = it.selectedCard?.copy(
                name = name.trim(), lastFourDigits = lastFour.trim(),
                creditLimit = creditLimit, billingDueDay = billingDueDay, statementDay = statementDay)) }
        }
    }

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch { repository.deleteCard(card) }
    }

    fun addTransaction(cardId: Long, date: LocalDate, amount: Double, type: TransactionType, note: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                CardTransaction(cardId = cardId, date = date, amount = amount, type = type, note = note.trim())
            )
            _uiState.update { it.copy(snackbarMessage = "Transaction saved") }
        }
    }

    fun updateTransaction(tx: CardTransaction, date: LocalDate, amount: Double, type: TransactionType, note: String) {
        viewModelScope.launch {
            repository.updateTransaction(tx.copy(date = date, amount = amount, type = type, note = note.trim()))
            _uiState.update { it.copy(snackbarMessage = "Transaction updated") }
        }
    }

    fun deleteTransaction(tx: CardTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _uiState.update { it.copy(snackbarMessage = "Transaction deleted") }
        }
    }
}
