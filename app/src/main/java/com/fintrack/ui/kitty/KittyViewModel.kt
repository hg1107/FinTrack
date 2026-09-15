package com.fintrack.ui.kitty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.BankAccount
import com.fintrack.data.local.entity.Kitty
import com.fintrack.data.local.entity.KittyMember
import com.fintrack.data.local.entity.KittyPayment
import com.fintrack.data.local.entity.KittyPayout
import com.fintrack.data.local.entity.Ledger
import com.fintrack.data.local.entity.LedgerEntry
import com.fintrack.data.local.entity.LedgerSourceType
import com.fintrack.data.local.entity.LedgerType
import com.fintrack.data.local.entity.PaymentMode
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.data.repository.BankAccountRepository
import com.fintrack.data.repository.KittyRepository
import com.fintrack.data.repository.LedgerRepository
import com.fintrack.ui.shared.monthName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ─── Sort options for the member list ───
enum class KittySort { NAME, HOST_MONTH, PAID_STATUS }

data class KittyUiState(
    val kitties: List<Kitty> = emptyList(),
    val selectedKitty: Kitty? = null,
    val members: List<KittyMember> = emptyList(),
    val payments: Map<Long, KittyPayment?> = emptyMap(),      // memberId -> payment for selected month/year
    val payoutsForMonth: List<KittyPayout> = emptyList(),     // all payout records for selected month
    val memberCountMap: Map<Long, Int> = emptyMap(),          // kittyId -> member count (list screen)
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val searchQuery: String = "",
    val sortOrder: KittySort = KittySort.HOST_MONTH
)

/**
 * Maps a PaymentMode to the name of the ledger that should receive an auto-entry.
 * Add future modes here — no changes to core upsertPayment logic required.
 */
val paymentModeLedgerMap: Map<PaymentMode, String> = mapOf(
    PaymentMode.CASH   to "Cash",
    PaymentMode.OFFICE to "Raj Office"
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class KittyViewModel @Inject constructor(
    private val repository: KittyRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KittyUiState())
    val uiState: StateFlow<KittyUiState> = _uiState.asStateFlow()

    /** All saved bank accounts — used in the payment sheet online-account dropdown */
    val allAccounts: StateFlow<List<BankAccount>> = bankAccountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ─── Selected kitty ID — drives members + payments reactively ───
    private val _selectedKittyId = MutableStateFlow<Long?>(null)

    init {
        // All kitties
        viewModelScope.launch {
            repository.getAllKitties().collect { kitties ->
                _uiState.update { it.copy(kitties = kitties) }
            }
        }
        // Member count map for list badges
        viewModelScope.launch {
            repository.getMemberCountsForAllKitties().collect { counts ->
                _uiState.update { it.copy(memberCountMap = counts.associate { c -> c.kittyId to c.count }) }
            }
        }
        // Members — reactive to selected kitty (flatMapLatest cancels old collectors)
        viewModelScope.launch {
            _selectedKittyId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList())
                else repository.getMembersForKitty(id)
            }.collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }
        // Payments — reactive to selected kitty AND selected month/year
        viewModelScope.launch {
            combine(
                _selectedKittyId,
                _uiState.map { it.selectedMonth to it.selectedYear }.distinctUntilChanged()
            ) { id, (month, year) -> Triple(id, month, year) }
                .flatMapLatest { (id, month, year) ->
                    if (id == null) flowOf(emptyList<KittyPayment>())
                    else repository.getPaymentsForKittyMonth(id, month, year)
                }.collect { paymentList ->
                    _uiState.update { it.copy(payments = paymentList.associateBy { it.memberId }) }
                }
        }
        // Payouts for selected month — reactive to selected kitty AND month/year
        viewModelScope.launch {
            combine(
                _selectedKittyId,
                _uiState.map { it.selectedMonth to it.selectedYear }.distinctUntilChanged()
            ) { id, (month, year) -> Triple(id, month, year) }
                .flatMapLatest { (id, month, year) ->
                    if (id == null) flowOf(emptyList<KittyPayout>())
                    else repository.getPayoutsForKittyMonth(id, month, year)
                }.collect { payouts ->
                    _uiState.update { it.copy(payoutsForMonth = payouts) }
                }
        }
    }

    val filteredKitties: StateFlow<List<Kitty>> = uiState.map { state ->
        if (state.searchQuery.isBlank()) state.kitties
        else state.kitties.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Members sorted according to the selected sort order */
    val sortedMembers: StateFlow<List<KittyMember>> = uiState.map { state ->
        val members = state.members
        val payments = state.payments
        when (state.sortOrder) {
            KittySort.NAME      -> members.sortedBy { it.name.lowercase() }
            KittySort.HOST_MONTH -> members.sortedBy { it.hostMonth }
            KittySort.PAID_STATUS -> members.sortedWith(
                compareBy({ payments[it.id]?.isPaid == true }, { it.name.lowercase() })
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Members who are hosting in the currently selected month — drives the HostsBox in the detail screen. */
    val hostsForSelectedMonth: StateFlow<List<KittyMember>> = uiState.map { state ->
        state.members.filter { it.hostMonth == state.selectedMonth }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(q: String) = _uiState.update { it.copy(searchQuery = q) }

    fun setSortOrder(sort: KittySort) = _uiState.update { it.copy(sortOrder = sort) }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun selectKitty(kittyId: Long) {
        viewModelScope.launch {
            val kitty = repository.getKittyById(kittyId) ?: return@launch
            _uiState.update { it.copy(selectedKitty = kitty) }
            _selectedKittyId.value = kittyId
        }
    }

    fun setSelectedMonthYear(month: Int, year: Int) {
        _uiState.update { it.copy(selectedMonth = month, selectedYear = year) }
        // Payouts re-fetched reactively via the flatMapLatest collector above
    }

    fun saveKitty(name: String, startMonth: Int, startYear: Int, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertKitty(
                Kitty(name = name.trim(), startMonth = startMonth, startYear = startYear)
            )
            onDone(id)
        }
    }

    fun updateKitty(kitty: Kitty, name: String, startMonth: Int, startYear: Int) {
        viewModelScope.launch {
            repository.updateKitty(kitty.copy(name = name.trim(), startMonth = startMonth, startYear = startYear))
        }
    }

    fun deleteKitty(kitty: Kitty) {
        viewModelScope.launch { repository.deleteKitty(kitty) }
    }

    fun addMember(kittyId: Long, name: String, hostMonth: Int, amount: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.insertMember(KittyMember(kittyId = kittyId, name = name.trim(), hostMonth = hostMonth, amount = amount))
            _uiState.update { it.copy(snackbarMessage = "Member \"$name\" added") }
            onDone()
        }
    }

    fun updateMember(member: KittyMember, name: String, hostMonth: Int, amount: Double) {
        viewModelScope.launch {
            repository.updateMember(member.copy(name = name.trim(), hostMonth = hostMonth, amount = amount))
            _uiState.update { it.copy(snackbarMessage = "Member updated") }
        }
    }

    fun deleteMember(member: KittyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
            _uiState.update { it.copy(snackbarMessage = "\"${member.name}\" removed") }
        }
    }

    fun upsertPayment(
        memberId: Long, month: Int, year: Int,
        isPaid: Boolean, amountPaid: Double,
        datePaid: LocalDate?, mode: PaymentMode?, onlineAccount: String?,
        note: String?
    ) {
        viewModelScope.launch {
            val existing = repository.getPaymentForMemberMonth(memberId, month, year)
            val payment = KittyPayment(
                id = existing?.id ?: 0,
                memberId = memberId,
                month = month,
                year = year,
                isPaid = isPaid,
                amountPaid = amountPaid,
                datePaid = datePaid,
                paymentMode = mode,
                onlineAccountName = onlineAccount,
                note = note
            )
            val savedId = repository.upsertPayment(payment)
            val paymentId = if (existing?.id != null && existing.id != 0L) existing.id else savedId

            // Auto-log a CREDIT transaction on the linked bank account when paying online
            if (isPaid && mode == PaymentMode.ONLINE && onlineAccount != null) {
                val account = allAccounts.value.firstOrNull { it.name == onlineAccount }
                if (account != null) {
                    val member = repository.getMemberById(memberId)
                    val kittyName = _uiState.value.selectedKitty?.name ?: ""
                    val txNote = buildString {
                        append("Kitty")
                        if (kittyName.isNotBlank()) append(": $kittyName")
                        if (member != null) append(" — ${member.name}")
                        append(" — ${monthName(month)} $year")
                    }
                    bankAccountRepository.insertTransaction(
                        AccountTransaction(
                            accountId = account.id,
                            date = datePaid ?: LocalDate.now(),
                            amount = amountPaid,
                            type = TransactionType.CREDIT,
                            note = txNote
                        )
                    )
                }
            }

            // ── Auto-ledger entry via extensible paymentModeLedgerMap ──
            if (isPaid && mode != null) {
                val ledgerName = paymentModeLedgerMap[mode]
                if (ledgerName != null) {
                    val ledger = ledgerRepository.getLedgerByName(ledgerName)
                    if (ledger != null) {
                        val member = repository.getMemberById(memberId)
                        val kittyName = _uiState.value.selectedKitty?.name ?: ""
                        val entryNote = note?.takeIf { it.isNotBlank() } ?: buildString {
                            append("Kitty")
                            if (kittyName.isNotBlank()) append(": $kittyName")
                            if (member != null) append(" — ${member.name}")
                            append(" — ${monthName(month)} $year")
                        }
                        // Update or create the ledger entry for this payment
                        val existingEntry = ledgerRepository.getEntryBySourceId(LedgerSourceType.KITTY_PAYMENT, paymentId)
                        if (existingEntry != null) {
                            ledgerRepository.updateEntry(
                                existingEntry.copy(amount = amountPaid, date = datePaid ?: LocalDate.now(), note = entryNote)
                            )
                        } else {
                            ledgerRepository.insertEntry(
                                LedgerEntry(
                                    ledgerId = ledger.id,
                                    amount = amountPaid,
                                    date = datePaid ?: LocalDate.now(),
                                    note = entryNote,
                                    sourceType = LedgerSourceType.KITTY_PAYMENT,
                                    sourceId = paymentId
                                )
                            )
                        }
                    }
                }
            } else if (!isPaid && existing != null) {
                // Payment cleared — remove any auto-generated ledger entry
                ledgerRepository.deleteEntryBySourceId(LedgerSourceType.KITTY_PAYMENT, existing.id)
            }

            _uiState.update { it.copy(snackbarMessage = if (isPaid) "Payment saved" else "Payment cleared") }
        }
    }

    // ─── Payout management ───
    fun upsertPayout(
        kittyId: Long, month: Int, year: Int,
        hostMemberId: Long, amountReceived: Double,
        receivedDate: LocalDate?, notes: String
    ) {
        viewModelScope.launch {
            val existing = repository.getPayoutForKittyMonthSuspend(kittyId, month, year)
            val payout = KittyPayout(
                id = existing?.id ?: 0,
                kittyId = kittyId,
                month = month,
                year = year,
                hostMemberId = hostMemberId,
                amountReceived = amountReceived,
                receivedDate = receivedDate,
                notes = notes
            )
            repository.upsertPayout(payout)
            _uiState.update { it.copy(snackbarMessage = "Payout recorded") }
        }
    }

    fun deletePayout(payout: KittyPayout) {
        viewModelScope.launch {
            repository.deletePayout(payout)
            _uiState.update { it.copy(snackbarMessage = "Payout removed") }
        }
    }

    // ─── Member payment history (all months) ───
    fun getPaymentsForMember(memberId: Long): Flow<List<KittyPayment>> =
        repository.getPaymentsForMember(memberId)

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
