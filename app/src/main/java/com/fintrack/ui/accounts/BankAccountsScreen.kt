package com.fintrack.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.BankAccount
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: BankAccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filteredAccounts by viewModel.filteredAccounts.collectAsState()
    val accountSearchQuery by viewModel.accountSearchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bank Accounts", style = MaterialTheme.typography.headlineLarge, color = OnSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = { FinFab(onClick = onNavigateToAdd) },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total liquid assets header
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Total Liquid Assets", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatAmount(state.totalBalance),
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (state.totalBalance >= 0) CreditGreen else DebitRed
                        )
                        if (state.totalBalance < 0) {
                            Spacer(Modifier.height(2.dp))
                            Text("(Overdrawn)", style = MaterialTheme.typography.bodySmall, color = DebitRed)
                        }
                    }
                }
            }

            // EMI Alert banner (upcoming EMI)
            if (state.upcomingEmi != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberContainer),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Amber.copy(alpha = 0.5f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Amber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, null, tint = Amber)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("EMI Alert", style = MaterialTheme.typography.titleMedium, color = Amber)
                                Text(
                                    "${state.upcomingEmi!!.label} — due on day ${state.upcomingEmi!!.dueDayOfMonth}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurface
                                )
                            }
                            Text(
                                formatAmount(state.upcomingEmi!!.amount),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Amber
                            )
                        }
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = accountSearchQuery,
                    onValueChange = viewModel::setAccountSearchQuery,
                    placeholder = { Text("Search accounts...", color = OnSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface
                    )
                )
            }

            item {
                Text("My Accounts", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
            }

            if (filteredAccounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.AccountBalance,
                        title = if (accountSearchQuery.isBlank()) "No accounts yet" else "No accounts found",
                        subtitle = if (accountSearchQuery.isBlank()) "Tap + to add your first bank account" else "Try a different search term",
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(filteredAccounts, key = { it.id }) { account ->
                    BankAccountListCard(
                        account = account,
                        onClick = { onNavigateToDetail(account.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BankAccountListCard(account: BankAccount, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalance, null, tint = Primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                if (account.lastFourDigits.isNotBlank()) {
                    Text("**** ${account.lastFourDigits}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = OnSurfaceVariant
            )
        }
    }
}
