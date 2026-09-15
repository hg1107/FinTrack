package com.fintrack.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.data.local.dao.LedgerSummary
import com.fintrack.data.local.entity.Ledger
import com.fintrack.data.local.entity.LedgerEntry
import com.fintrack.data.local.entity.LedgerSourceType
import com.fintrack.data.local.entity.LedgerType
import com.fintrack.data.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LedgerUiState(
    val ledgers: List<Ledger> = emptyList(),
    val summaries: Map<Long, LedgerSummary> = emptyMap(),   // ledgerId -> LedgerSummary
    val selectedLedger: Ledger? = null,
    val entries: List<LedgerEntry> = emptyList(),
    val searchQuery: String = "",
    val filterFrom: LocalDate? = null,
    val filterTo: LocalDate? = null,
    val monthlyTotal: Double = 0.0,
    val yearlyTotal: Double = 0.0,
    val snackbarMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    private val _selectedLedgerId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            repository.getAllLedgers().collect { ledgers ->
                _uiState.update { it.copy(ledgers = ledgers) }
            }
        }
        viewModelScope.launch {
            repository.getAllLedgerSummaries().collect { list ->
                _uiState.update { it.copy(summaries = list.associateBy { s -> s.ledgerId }) }
            }
        }
        // Entries — reactive to selected ledger + search/date filters
        viewModelScope.launch {
            combine(
                _selectedLedgerId,
                _uiState.map { Triple(it.searchQuery, it.filterFrom, it.filterTo) }.distinctUntilChanged()
            ) { id, (q, from, to) -> listOf(id, q, from, to) }
                .flatMapLatest { args ->
                    val id = args[0] as Long? ?: return@flatMapLatest flowOf(emptyList<LedgerEntry>())
                    val q = args[1] as String
                    val from = args[2] as LocalDate?
                    val to = args[3] as LocalDate?
                    repository.searchEntries(id, q, from?.toString(), to?.toString())
                }.collect { entries ->
                    _uiState.update { it.copy(entries = entries) }
                }
        }
        // Monthly & yearly totals — reactive to selected ledger
        viewModelScope.launch {
            _selectedLedgerId.flatMapLatest { id ->
                if (id == null) flowOf(0.0)
                else {
                    val now = LocalDate.now()
                    repository.getMonthlyTotal(id, now.monthValue, now.year)
                }
            }.collect { total ->
                _uiState.update { it.copy(monthlyTotal = total) }
            }
        }
        viewModelScope.launch {
            _selectedLedgerId.flatMapLatest { id ->
                if (id == null) flowOf(0.0)
                else repository.getYearlyTotal(id, LocalDate.now().year)
            }.collect { total ->
                _uiState.update { it.copy(yearlyTotal = total) }
            }
        }
    }

    fun selectLedger(ledgerId: Long) {
        viewModelScope.launch {
            val ledger = repository.getLedgerById(ledgerId) ?: return@launch
            _uiState.update { it.copy(selectedLedger = ledger) }
            _selectedLedgerId.value = ledgerId
        }
    }

    fun setSearchQuery(q: String) = _uiState.update { it.copy(searchQuery = q) }

    fun setDateFilter(from: LocalDate?, to: LocalDate?) =
        _uiState.update { it.copy(filterFrom = from, filterTo = to) }

    fun clearFilters() = _uiState.update { it.copy(searchQuery = "", filterFrom = null, filterTo = null) }

    fun createLedger(name: String, colorHex: String, iconId: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertLedger(
                Ledger(name = name.trim(), type = LedgerType.MANUAL, colorHex = colorHex, iconId = iconId)
            )
            _uiState.update { it.copy(snackbarMessage = "Ledger \"${name.trim()}\" created") }
            onDone(id)
        }
    }

    fun addEntry(ledgerId: Long, amount: Double, date: LocalDate, note: String?, isRecurring: Boolean) {
        viewModelScope.launch {
            repository.insertEntry(
                LedgerEntry(
                    ledgerId = ledgerId,
                    amount = amount,
                    date = date,
                    note = note,
                    sourceType = LedgerSourceType.MANUAL,
                    isRecurring = isRecurring
                )
            )
            _uiState.update { it.copy(snackbarMessage = "Entry added") }
        }
    }

    fun updateEntry(entry: LedgerEntry, amount: Double, date: LocalDate, note: String?, isRecurring: Boolean) {
        if (entry.sourceType != LedgerSourceType.MANUAL) {
            _uiState.update { it.copy(snackbarMessage = "Edit the source record to update this entry") }
            return
        }
        viewModelScope.launch {
            repository.updateEntry(entry.copy(amount = amount, date = date, note = note, isRecurring = isRecurring))
            _uiState.update { it.copy(snackbarMessage = "Entry updated") }
        }
    }

    fun deleteEntry(entry: LedgerEntry) {
        if (entry.sourceType != LedgerSourceType.MANUAL) {
            _uiState.update { it.copy(snackbarMessage = "Delete the source record to remove this entry") }
            return
        }
        viewModelScope.launch {
            repository.deleteEntry(entry)
            _uiState.update { it.copy(snackbarMessage = "Entry deleted") }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
}
