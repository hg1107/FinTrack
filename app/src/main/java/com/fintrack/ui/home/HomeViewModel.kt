package com.fintrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.Emi
import com.fintrack.data.repository.BankAccountRepository
import com.fintrack.data.repository.CreditCardRepository
import com.fintrack.data.repository.KittyRepository
import com.fintrack.data.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeUiState(
    val kittyCount: Int = 0,
    val cardCount: Int = 0,
    val accountCount: Int = 0,
    val totalOutstanding: Double = 0.0,
    val totalBankBalance: Double = 0.0,
    val netWorth: Double = 0.0,
    val upcomingEmis: List<Emi> = emptyList(),
    val recentTransactions: List<AccountTransaction> = emptyList(),
    val ledgerCount: Int = 0,
    val totalLedgerBalance: Double = 0.0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    kittyRepo: KittyRepository,
    cardRepo: CreditCardRepository,
    bankRepo: BankAccountRepository,
    ledgerRepo: LedgerRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        kittyRepo.getKittyCount(),
        cardRepo.getCardCount(),
        bankRepo.getAccountCount(),
        cardRepo.getTotalOutstanding(),
        bankRepo.getTotalBalance(),
        bankRepo.getUpcomingUnpaidEmis(3),
        bankRepo.getAllRecentTransactions(5),
        ledgerRepo.getLedgerCount(),
        ledgerRepo.getTotalBalance()
    ) { values ->
        val kittyCount     = values[0] as Int
        val cardCount      = values[1] as Int
        val accountCount   = values[2] as Int
        @Suppress("UNCHECKED_CAST")
        val outstanding    = values[3] as Double
        @Suppress("UNCHECKED_CAST")
        val bankBalance    = values[4] as Double
        @Suppress("UNCHECKED_CAST")
        val upcomingEmis   = values[5] as List<Emi>
        @Suppress("UNCHECKED_CAST")
        val recentTxns     = values[6] as List<AccountTransaction>
        val ledgerCount    = values[7] as Int
        @Suppress("UNCHECKED_CAST")
        val ledgerBalance  = values[8] as Double
        HomeUiState(
            kittyCount = kittyCount,
            cardCount = cardCount,
            accountCount = accountCount,
            totalOutstanding = outstanding,
            totalBankBalance = bankBalance,
            netWorth = bankBalance - outstanding,
            upcomingEmis = upcomingEmis,
            recentTransactions = recentTxns,
            ledgerCount = ledgerCount,
            totalLedgerBalance = ledgerBalance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
