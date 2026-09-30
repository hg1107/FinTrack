package com.fintrack.ui.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.CardTransaction
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailScreen(
    cardId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    LaunchedEffect(cardId) { viewModel.selectCard(cardId) }

    val state by viewModel.uiState.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val card = state.selectedCard ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() }
    }

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<CardTransaction?>(null) }
    var showDeleteCardDialog by remember { mutableStateOf(false) }

    // Running balance from all (unfiltered) transactions
    val txnsWithBalance = remember(state.transactions) {
        var running = 0.0
        state.transactions.sortedByDescending { it.date }.map { tx ->
            running += if (tx.type == TransactionType.DEBIT) tx.amount else -tx.amount
            Pair(tx, running)
        }
    }

    // For filtered view, lookup running balance from pre-computed map
    val balanceMap = remember(txnsWithBalance) { txnsWithBalance.associate { it.first.id to it.second } }

    val availableCredit = card.creditLimit - state.outstandingBalance

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(card.name, style = MaterialTheme.typography.headlineMedium, color = OnSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface)
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = OnSurface)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = SurfaceVariant
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Card", color = OnSurface) },
                            onClick = { menuExpanded = false; onNavigateToEdit(card.id) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Card", color = Error) },
                            onClick = { menuExpanded = false; showDeleteCardDialog = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            FinFab(onClick = { editingTransaction = null; showTransactionSheet = true })
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current balance card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("CURRENT BALANCE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatAmount(state.outstandingBalance),
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                            color = if (state.outstandingBalance > 0) DebitRed else PaidGreen
                        )
                        if (state.lastPaymentDate != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Last payment: ${formatDateShort(state.lastPaymentDate!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                        // Due date
                        if (card.billingDueDay > 0) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, tint = Amber,
                                    modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Payment due: day ${card.billingDueDay} of each month",
                                    style = MaterialTheme.typography.bodySmall, color = Amber
                                )
                            }
                        }
                        if (card.statementDay > 0) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Statement generates: day ${card.statementDay} of each month",
                                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Available credit with progress bar
            if (card.creditLimit > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("AVAILABLE CREDIT", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        formatAmount(maxOf(0.0, availableCredit)),
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                        color = OnSurface
                                    )
                                }
                                Icon(Icons.Default.CreditCard, null, tint = OnSurfaceVariant)
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (state.outstandingBalance / card.creditLimit).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = if (state.outstandingBalance / card.creditLimit > 0.8) Error else Primary,
                                trackColor = SurfaceContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Limit: ${formatAmount(card.creditLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Filter chips + search ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Type filter chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionFilter.entries.forEach { f ->
                            FilterChip(
                                selected = state.transactionFilter == f,
                                onClick = { viewModel.setTransactionFilter(f) },
                                label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryContainer,
                                    selectedLabelColor = PaidGreen,
                                    containerColor = SurfaceContainer,
                                    labelColor = OnSurfaceVariant
                                )
                            )
                        }
                    }
                    // Search field
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = { Text("Search transactions...", color = OnSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp)) },
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
            }

            // Transactions header with toggle
            item {
                SectionHeader(
                    title = if (state.showAllTransactions) "All Transactions" else "Recent Transactions",
                    action = if (filteredTransactions.size > 5) {
                        {
                            TextButton(onClick = { viewModel.toggleShowAllTransactions() }) {
                                Text(if (state.showAllTransactions) "Show Less" else "View All", color = Primary)
                            }
                        }
                    } else null
                )
            }

            val displayList = if (state.showAllTransactions) filteredTransactions else filteredTransactions.take(5)

            if (displayList.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = if (state.searchQuery.isBlank()) "No transactions" else "No results",
                        subtitle = if (state.searchQuery.isBlank()) "Tap + to log your first transaction" else "Try a different search or filter"
                    )
                }
            } else {
                items(displayList, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        runningBalance = balanceMap[tx.id] ?: 0.0,
                        onEdit = { editingTransaction = tx; showTransactionSheet = true },
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Transaction add/edit sheet (refactored — no CardTransaction dependency)
    if (showTransactionSheet) {
        AddEditTransactionSheet(
            existingDate = editingTransaction?.date,
            existingAmount = editingTransaction?.amount,
            existingType = editingTransaction?.type ?: TransactionType.DEBIT,
            existingNote = editingTransaction?.note ?: "",
            onDismiss = { showTransactionSheet = false },
            onSave = { date, amount, type, note ->
                val tx = editingTransaction
                if (tx == null) {
                    viewModel.addTransaction(cardId, date, amount, type, note)
                } else {
                    viewModel.updateTransaction(tx, date, amount, type, note)
                }
                showTransactionSheet = false
            }
        )
    }

    // Delete card dialog
    if (showDeleteCardDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCardDialog = false },
            title = { Text("Delete Card?", color = OnSurface) },
            text = { Text("All transactions for \"${card.name}\" will be permanently deleted.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(card)
                    showDeleteCardDialog = false
                    onNavigateBack()
                }) { Text("Delete", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCardDialog = false }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = SurfaceVariant
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: CardTransaction,
    runningBalance: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.note.ifBlank { transaction.type.name },
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
                Text(
                    formatDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val sign = if (transaction.type == TransactionType.DEBIT) "-" else "+"
                val color = if (transaction.type == TransactionType.DEBIT) DebitRed else CreditGreen
                Text(
                    "$sign${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = color
                )
                Text(
                    "Bal: ${formatAmount(runningBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = SurfaceVariant
                ) {
                    DropdownMenuItem(text = { Text("Edit", color = OnSurface) }, onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete", color = Error) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}
