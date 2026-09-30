package com.fintrack.ui.accounts

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.AccountTransaction
import com.fintrack.data.local.entity.Emi
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.ui.cards.AddEditTransactionSheet
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountDetailScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: BankAccountViewModel = hiltViewModel()
) {
    LaunchedEffect(accountId) { viewModel.selectAccount(accountId) }

    val state by viewModel.uiState.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val account = state.selectedAccount ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Transactions", "EMI Schedule")

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<AccountTransaction?>(null) }
    var showEmiSheet by remember { mutableStateOf(false) }
    var editingEmi by remember { mutableStateOf<Emi?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(account.name, style = MaterialTheme.typography.headlineMedium, color = OnSurface) },
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
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = SurfaceVariant) {
                        DropdownMenuItem(text = { Text("Edit Account", color = OnSurface) }, onClick = { menuExpanded = false; onNavigateToEdit(account.id) })
                        DropdownMenuItem(text = { Text("Delete Account", color = Error) }, onClick = { menuExpanded = false; showDeleteDialog = true })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            FinFab(onClick = {
                if (selectedTab == 0) {
                    editingTransaction = null
                    showTransactionSheet = true
                } else {
                    editingEmi = null
                    showEmiSheet = true
                }
            })
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
            // Balance card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Available Balance", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatAmount(state.balance),
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                            color = if (state.balance >= 0) CreditGreen else DebitRed
                        )
                        if (state.balance < 0) {
                            Spacer(Modifier.height(2.dp))
                            Text("(Overdrawn)", style = MaterialTheme.typography.bodySmall, color = DebitRed)
                        }
                        if (account.lastFourDigits.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("**** ${account.lastFourDigits}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                }
            }

            // Tab row
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Surface,
                    contentColor = Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Primary,
                            height = 2.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) Primary else OnSurfaceVariant,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // ── Filter chips + search ──
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountTransactionFilter.entries.forEach { f ->
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

                // Transactions tab
                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = if (state.searchQuery.isBlank()) "No transactions" else "No results",
                            subtitle = if (state.searchQuery.isBlank()) "Tap + to add your first transaction" else "Try a different search or filter",
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        AccountTransactionItem(
                            tx = tx,
                            onEdit = { editingTransaction = tx; showTransactionSheet = true },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            } else {
                // EMI Schedule tab
                if (state.emis.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.CalendarMonth,
                            title = "No EMIs set up",
                            subtitle = "Tap + to add an EMI schedule",
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                } else {
                    items(state.emis, key = { it.id }) { emi ->
                        EmiCard(
                            emi = emi,
                            onTogglePaid = { viewModel.toggleEmiPaid(emi) },
                            onToggleReminder = { viewModel.updateEmi(emi, emi.label, emi.amount, emi.dueDayOfMonth, emi.tenureMonths, emi.startDate, emi.reminderDaysBefore, !emi.isReminderEnabled) },
                            onEdit = { editingEmi = emi; showEmiSheet = true },
                            onDelete = { viewModel.deleteEmi(emi) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Transaction sheet (refactored — no CardTransaction hack)
    if (showTransactionSheet) {
        AddEditTransactionSheet(
            existingDate = editingTransaction?.date,
            existingAmount = editingTransaction?.amount,
            existingType = editingTransaction?.type ?: TransactionType.DEBIT,
            existingNote = editingTransaction?.note ?: "",
            onDismiss = { showTransactionSheet = false },
            onSave = { date, amount, type, note ->
                val tx = editingTransaction
                if (tx == null) viewModel.addTransaction(accountId, date, amount, type, note)
                else viewModel.updateTransaction(tx, date, amount, type, note)
                showTransactionSheet = false
            }
        )
    }

    // EMI sheet
    if (showEmiSheet) {
        AddEditEmiSheet(
            existing = editingEmi,
            onDismiss = { showEmiSheet = false },
            onSave = { label, amount, dayOfMonth, tenureMonths, startDate, reminderDays, reminderEnabled ->
                val ex = editingEmi
                if (ex == null) {
                    viewModel.saveEmi(accountId, label, amount, dayOfMonth, tenureMonths, startDate, reminderDays, reminderEnabled) {}
                } else {
                    viewModel.updateEmi(ex, label, amount, dayOfMonth, tenureMonths, startDate, reminderDays, reminderEnabled)
                }
                showEmiSheet = false
            }
        )
    }

    // Delete account dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", color = OnSurface) },
            text = { Text("All transactions and EMIs for \"${account.name}\" will be permanently deleted.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(account)
                    showDeleteDialog = false
                    onNavigateBack()
                }) { Text("Delete", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = SurfaceVariant
        )
    }
}

@Composable
private fun AccountTransactionItem(
    tx: AccountTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tx.note.ifBlank { tx.type.name }, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Text(formatDate(tx.date), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            val sign = if (tx.type == TransactionType.DEBIT) "-" else "+"
            val color = if (tx.type == TransactionType.DEBIT) DebitRed else CreditGreen
            Text(
                "$sign${formatAmount(tx.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = SurfaceVariant) {
                    DropdownMenuItem(text = { Text("Edit", color = OnSurface) }, onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete", color = Error) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun EmiCard(
    emi: Emi,
    onTogglePaid: () -> Unit,
    onToggleReminder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Compute remaining EMIs if tenure is known
    val remainingText: String? = if (emi.tenureMonths > 0 && emi.startDate != null) {
        val today = java.time.LocalDate.now()
        val elapsed = (today.year - emi.startDate.year) * 12 + (today.monthValue - emi.startDate.monthValue)
        val remaining = (emi.tenureMonths - elapsed).coerceAtLeast(0)
        "$remaining of ${emi.tenureMonths} installments remaining"
    } else if (emi.tenureMonths > 0) {
        "${emi.tenureMonths} total installments"
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (!emi.isPaid) SurfaceVariant else Surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emi.label, style = MaterialTheme.typography.titleMedium, color = OnSurface, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = SurfaceVariant) {
                        DropdownMenuItem(text = { Text("Edit", color = OnSurface) }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete", color = Error) }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }

            Row {
                Column(Modifier.weight(1f)) {
                    Text("Amount", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Text(formatAmount(emi.amount), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OnSurface)
                }
                Column(Modifier.weight(1f)) {
                    Text("Due Date", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Text("Day ${emi.dueDayOfMonth}", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                }
            }

            if (emi.startDate != null) {
                Text("Started: ${formatDate(emi.startDate)}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            if (remainingText != null) {
                Text(remainingText, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Amber)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, modifier = Modifier.weight(1f))
                StatusChip(emi.isPaid)
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onTogglePaid,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(if (emi.isPaid) Error else Primary))
                ) {
                    Text(if (emi.isPaid) "Mark Unpaid" else "Mark Paid", style = MaterialTheme.typography.labelSmall, color = if (emi.isPaid) Error else Primary)
                }
            }

            // Reminder toggle
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Reminder", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        Text("${emi.reminderDaysBefore} day(s) before due", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    Switch(
                        checked = emi.isReminderEnabled,
                        onCheckedChange = { onToggleReminder() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = PrimaryContainer)
                    )
                }
            }
        }
    }
}
