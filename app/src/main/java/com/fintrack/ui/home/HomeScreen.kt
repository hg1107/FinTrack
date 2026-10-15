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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.Emi
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*
import java.time.LocalTime

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Primary, PrimaryVariant))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "F",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimary
                                )
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "FinTrack",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Primary
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Time-based greeting
            val greeting = when (LocalTime.now().hour) {
                in 5..11  -> "Good morning ☀️"
                in 12..16 -> "Good afternoon 🌤️"
                in 17..20 -> "Good evening 🌇"
                else      -> "Good night 🌙"
            }
            Text(
                greeting,
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

            Spacer(Modifier.height(4.dp))

            // ── Net Worth hero card ──
            NetWorthCard(
                netWorth = state.netWorth,
                bankBalance = state.totalBankBalance,
                cardOutstanding = state.totalOutstanding
            )

            // ── Quick-action chips ──
            QuickActionRow(
                onAddTransaction = onNavigateToAccounts,
                onViewCards      = onNavigateToCards,
                onViewLedgers    = onNavigateToLedgers
            )

            // ── Module navigation cards ──
            ModuleSectionHeader("Modules")

            SummaryCard(
                icon     = Icons.Default.Groups,
                title    = "Kitty Parties",
                subtitle = "${state.kittyCount} active ${if (state.kittyCount == 1) "kitty" else "kitties"}",
                iconTint = Primary,
                onClick  = onNavigateToKitties
            )

            SummaryCard(
                icon     = Icons.Default.CreditCard,
                title    = "Credit Cards",
                subtitle = "${state.cardCount} ${if (state.cardCount == 1) "card" else "cards"} · Outstanding ${formatAmount(state.totalOutstanding)}",
                iconTint = Secondary,
                onClick  = onNavigateToCards
            )

            SummaryCard(
                icon     = Icons.Default.AccountBalance,
                title    = "Bank Accounts",
                subtitle = "${state.accountCount} ${if (state.accountCount == 1) "account" else "accounts"} · Balance ${formatAmount(state.totalBankBalance)}",
                iconTint = Primary,
                onClick  = onNavigateToAccounts
            )

            SummaryCard(
                icon     = Icons.Default.Bookmarks,
                title    = "Ledgers",
                subtitle = "${state.ledgerCount} ${if (state.ledgerCount == 1) "ledger" else "ledgers"} · Total ${formatAmount(state.totalLedgerBalance)}",
                iconTint = Amber,
                onClick  = onNavigateToLedgers
            )

            // ── Upcoming EMIs widget ──
            if (state.upcomingEmis.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SectionHeader(title = "Upcoming EMIs")
                state.upcomingEmis.forEach { emi ->
                    UpcomingEmiRow(emi = emi)
                }
            }

            // ── Recent Transactions widget ──
            if (state.recentTransactions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
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

// ─── Section label (small caps style) ───
@Composable
private fun ModuleSectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = OnSurfaceVariant
        )
    )
}

// ─── Quick action chips ───
@Composable
private fun QuickActionRow(
    onAddTransaction: () -> Unit,
    onViewCards: () -> Unit,
    onViewLedgers: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickChip(
            icon  = Icons.Default.Add,
            label = "Add Txn",
            color = Primary,
            onClick = onAddTransaction,
            modifier = Modifier.weight(1f)
        )
        QuickChip(
            icon  = Icons.Default.CreditCard,
            label = "Cards",
            color = Secondary,
            onClick = onViewCards,
            modifier = Modifier.weight(1f)
        )
        QuickChip(
            icon  = Icons.Default.Bookmarks,
            label = "Ledgers",
            color = Amber,
            onClick = onViewLedgers,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}

// ─── Net Worth hero card ───
@Composable
private fun NetWorthCard(netWorth: Double, bankBalance: Double, cardOutstanding: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Net Worth", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatAmount(netWorth),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 30.sp, fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (netWorth >= 0) CreditGreen else DebitRed
                    )
                }
                // Visual indicator pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (netWorth >= 0) PrimaryContainer else ErrorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (netWorth >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (netWorth >= 0) CreditGreen else DebitRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            if (netWorth >= 0) "Positive" else "Negative",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (netWorth >= 0) CreditGreen else DebitRed
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Outline.copy(alpha = 0.6f))
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Bank Balance tile
                FinancialTile(
                    modifier = Modifier.weight(1f),
                    label = "Bank Balance",
                    amount = bankBalance,
                    amountColor = CreditGreen,
                    bgColor = PrimaryContainer,
                    icon = Icons.Default.AccountBalance,
                    iconTint = Primary
                )
                // Card Outstanding tile
                FinancialTile(
                    modifier = Modifier.weight(1f),
                    label = "Card Outstanding",
                    amount = cardOutstanding,
                    amountColor = if (cardOutstanding > 0) DebitRed else OnSurface,
                    bgColor = if (cardOutstanding > 0) ErrorContainer else SurfaceVariant,
                    icon = Icons.Default.CreditCard,
                    iconTint = if (cardOutstanding > 0) DebitRed else OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinancialTile(
    modifier: Modifier = Modifier,
    label: String,
    amount: Double,
    amountColor: Color,
    bgColor: Color,
    icon: ImageVector,
    iconTint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                formatAmount(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
        }
    }
}

// ─── Upcoming EMI row ───
@Composable
private fun UpcomingEmiRow(emi: Emi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AmberContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Amber.copy(alpha = 0.25f)),
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
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val isDebit = tx.type == TransactionType.DEBIT
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
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
