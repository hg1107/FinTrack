package com.fintrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.Emi
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToKitties: () -> Unit,
    onNavigateToCards: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToLedgers: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("FinTrack", style = MaterialTheme.typography.headlineLarge, color = Primary)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Welcome back,",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
            Text(
                "Your Overview",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 26.sp, fontWeight = FontWeight.Bold
                ),
                color = OnSurface
            )

            Spacer(Modifier.height(12.dp))

            // ── Net Worth hero card ──
            NetWorthCard(
                netWorth = state.netWorth,
                bankBalance = state.totalBankBalance,
                cardOutstanding = state.totalOutstanding
            )

            Spacer(Modifier.height(8.dp))

            // ── Module navigation cards ──
            SummaryCard(
                icon = Icons.Default.Groups,
                title = "Kitty Parties",
                subtitle = "${state.kittyCount} active ${if (state.kittyCount == 1) "kitty" else "kitties"}",
                iconTint = Primary,
                onClick = onNavigateToKitties
            )

            SummaryCard(
                icon = Icons.Default.CreditCard,
                title = "Credit Cards",
                subtitle = "${state.cardCount} ${if (state.cardCount == 1) "card" else "cards"} · Outstanding ${formatAmount(state.totalOutstanding)}",
                iconTint = Amber,
                onClick = onNavigateToCards
            )

            SummaryCard(
                icon = Icons.Default.AccountBalance,
                title = "Bank Accounts",
                subtitle = "${state.accountCount} ${if (state.accountCount == 1) "account" else "accounts"} · Balance ${formatAmount(state.totalBankBalance)}",
                iconTint = Primary,
                onClick = onNavigateToAccounts
            )

            SummaryCard(
                icon = Icons.Default.Bookmarks,
                title = "Ledgers",
                subtitle = "${state.ledgerCount} ${if (state.ledgerCount == 1) "ledger" else "ledgers"} · Total ${formatAmount(state.totalLedgerBalance)}",
                iconTint = Amber,
                onClick = onNavigateToLedgers
            )

            // ── Upcoming EMIs widget ──
            if (state.upcomingEmis.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SectionHeader(title = "Upcoming EMIs")
                state.upcomingEmis.forEach { emi ->
                    UpcomingEmiRow(emi = emi)
                }
            }

            // ── Recent Transactions widget ──
            if (state.recentTransactions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title = "Recent Transactions",
                    action = {
                        TextButton(onClick = onNavigateToAccounts) {
                            Text("See all", color = Primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                )
                state.recentTransactions.forEach { tx ->
                    RecentTransactionRow(tx = tx)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Net Worth hero card ───
@Composable
private fun NetWorthCard(netWorth: Double, bankBalance: Double, cardOutstanding: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                formatAmount(netWorth),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
                color = if (netWorth >= 0) CreditGreen else DebitRed
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Bank Balance", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatAmount(bankBalance),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = CreditGreen
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Card Outstanding", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatAmount(cardOutstanding),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (cardOutstanding > 0) DebitRed else OnSurface
                    )
                }
            }
        }
    }
}

// ─── Upcoming EMI row ───
@Composable
private fun UpcomingEmiRow(emi: Emi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AmberContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Amber.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = Amber, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(emi.label, style = MaterialTheme.typography.titleSmall, color = OnSurface)
                Text(
                    "Due on day ${emi.dueDayOfMonth} of the month",
                    style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                )
            }
            Text(
                formatAmount(emi.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Amber
            )
        }
    }
}

// ─── Recent transaction row ───
@Composable
private fun RecentTransactionRow(tx: AccountTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val isDebit = tx.type == TransactionType.DEBIT
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(if (isDebit) ErrorContainer else PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null,
                    tint = if (isDebit) DebitRed else CreditGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tx.note.ifBlank { tx.type.name },
                    style = MaterialTheme.typography.titleSmall, color = OnSurface
                )
                Text(formatDate(tx.date), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            val sign = if (isDebit) "-" else "+"
            Text(
                "$sign${formatAmount(tx.amount)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDebit) DebitRed else CreditGreen
            )
        }
    }
}
