package com.fintrack.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.LedgerEntry
import com.fintrack.data.local.entity.LedgerSourceType
import com.fintrack.data.local.entity.LedgerType
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerDetailScreen(
    ledgerId: Long,
    onNavigateBack: () -> Unit,
    viewModel: LedgerViewModel = hiltViewModel()
) {
    LaunchedEffect(ledgerId) { viewModel.selectLedger(ledgerId) }

    val state by viewModel.uiState.collectAsState()
    val ledger = state.selectedLedger ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val accentColor = runCatching { Color(android.graphics.Color.parseColor(ledger.colorHex)) }
        .getOrDefault(Primary)

    var showAddEntrySheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var showDeleteDialog by remember { mutableStateOf<LedgerEntry?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showDateFilter by remember { mutableStateOf(false) }

    val isManual = ledger.type == LedgerType.MANUAL

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(ledger.name, style = MaterialTheme.typography.headlineMedium, color = OnSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface)
                    }
                },
                actions = {
                    if (ledger.type == LedgerType.AUTO) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PrimaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                "AUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaidGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            // Always show FAB — manual entries can be added to AUTO ledgers too
            FinFab(onClick = { editingEntry = null; showAddEntrySheet = true })
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Totals summary ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TotalCard("THIS MONTH", formatAmount(state.monthlyTotal), accentColor, Modifier.weight(1f))
                    TotalCard("THIS YEAR", formatAmount(state.yearlyTotal), OnSurface, Modifier.weight(1f))
                }
            }

            // ── Search bar ──
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it; viewModel.setSearchQuery(it) },
                    label = { Text("Search notes…") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceVariant) },
                    trailingIcon = {
                        if (searchText.isNotBlank()) {
                            IconButton(onClick = { searchText = ""; viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, null, tint = OnSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = OnSurfaceVariant
                    )
                )
            }

            // ── Date filter chips ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filterFrom != null || state.filterTo != null,
                        onClick = { showDateFilter = !showDateFilter },
                        label = {
                            Text(
                                if (state.filterFrom != null || state.filterTo != null)
                                    "${state.filterFrom?.let { formatDateShort(it) } ?: "…"} → ${state.filterTo?.let { formatDateShort(it) } ?: "…"}"
                                else "Date range",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer,
                            selectedLabelColor = PaidGreen,
                            containerColor = SurfaceContainer,
                            labelColor = OnSurfaceVariant
                        )
                    )
                    if (state.filterFrom != null || state.filterTo != null || searchText.isNotBlank()) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                searchText = ""
                                viewModel.clearFilters()
                            },
                            label = { Text("Clear", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ErrorContainer,
                                labelColor = DebitRed
                            )
                        )
                    }
                }
            }

            // Date range pickers (inline, shown when filter chip tapped)
            if (showDateFilter) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateInputField(
                            date = state.filterFrom,
                            onDateSelected = { viewModel.setDateFilter(it, state.filterTo) },
                            label = "From",
                            modifier = Modifier.weight(1f)
                        )
                        DateInputField(
                            date = state.filterTo,
                            onDateSelected = { viewModel.setDateFilter(state.filterFrom, it) },
                            label = "To",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Entry list ──
            if (state.entries.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No entries",
                        subtitle = "Tap + to add an entry"
                    )
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    LedgerEntryCard(
                        entry = entry,
                        accentColor = accentColor,
                        onEdit = if (isManual || entry.sourceType == LedgerSourceType.MANUAL) {
                            { editingEntry = entry; showAddEntrySheet = true }
                        } else null,
                        onDelete = if (isManual || entry.sourceType == LedgerSourceType.MANUAL) {
                            { showDeleteDialog = entry }
                        } else null,
                        onLinkedTap = {
                            // Inform user — auto entries can't be edited directly
                        }
                    )
                }
            }
        }
    }

    if (showAddEntrySheet) {
        AddLedgerEntrySheet(
            existingEntry = editingEntry,
            onDismiss = { showAddEntrySheet = false; editingEntry = null },
            onSave = { amount, date, note, isRecurring ->
                val ex = editingEntry
                if (ex == null) {
                    viewModel.addEntry(ledgerId, amount, date, note, isRecurring)
                } else {
                    viewModel.updateEntry(ex, amount, date, note, isRecurring)
                }
                showAddEntrySheet = false
                editingEntry = null
            }
        )
    }

    if (showDeleteDialog != null) {
        val entry = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Entry?", color = OnSurface) },
            text = { Text("This entry will be permanently removed.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEntry(entry); showDeleteDialog = null }) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceVariant
        )
    }
}

// ─── Totals summary card ───
@Composable
private fun TotalCard(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = valueColor)
        }
    }
}

// ─── Ledger entry row card ───
@Composable
private fun LedgerEntryCard(
    entry: LedgerEntry,
    accentColor: Color,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onLinkedTap: () -> Unit
) {
    val isAutoLinked = entry.sourceType != LedgerSourceType.MANUAL
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isAutoLinked) Icons.Default.Link else Icons.Default.Edit,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.note?.takeIf { it.isNotBlank() } ?: entry.sourceType.name.replace("_", " "),
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurface
                    )
                    if (isAutoLinked) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PrimaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "LINKED",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaidGreen,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(formatDate(entry.date), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                if (entry.isRecurring) {
                    Text("Recurring", style = MaterialTheme.typography.labelSmall, color = Primary)
                }
            }
            Text(
                formatAmount(entry.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            if (onEdit != null || onDelete != null) {
                Box {
                    IconButton(onClick = { if (isAutoLinked) onLinkedTap() else showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = SurfaceVariant) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit", color = OnSurface) },
                                onClick = { showMenu = false; onEdit() }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = Error) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }
        }
    }
}
