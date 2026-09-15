package com.fintrack.ui.kitty
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.KittyMember
import com.fintrack.data.local.entity.KittyPayment
import com.fintrack.data.local.entity.KittyPayout
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KittyDetailScreen(
    kittyId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: KittyViewModel = hiltViewModel()
) {
    LaunchedEffect(kittyId) { viewModel.selectKitty(kittyId) }

    val state by viewModel.uiState.collectAsState()
    val sortedMembers by viewModel.sortedMembers.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val hostsForMonth by viewModel.hostsForSelectedMonth.collectAsState()
    val kitty = state.selectedKitty ?: return

    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar observer
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<KittyMember?>(null) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<KittyMember?>(null) }
    var showDeleteMemberDialog by remember { mutableStateOf<KittyMember?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var historyMember by remember { mutableStateOf<KittyMember?>(null) }
    var historyPayments by remember { mutableStateOf<List<KittyPayment>>(emptyList()) }
    var showPayoutSheet by remember { mutableStateOf(false) }

    // Dynamic month range: start = June 2025, end = today + 36 months
    val months = remember {
        buildList {
            var m = 6; var y = 2025
            val today = LocalDate.now()
            val endM = today.monthValue; val endY = today.year + 3   // 36 months ahead
            while (y < endY || (y == endY && m <= endM)) {
                add(Pair(m, y))
                m++; if (m > 12) { m = 1; y++ }
            }
        }
    }

    val totalTarget = state.members.sumOf { it.amount }
    val paidTotal = state.payments.values.filterNotNull()
        .filter { it.isPaid }.sumOf { it.amountPaid }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(kitty.name, style = MaterialTheme.typography.headlineMedium, color = OnSurface) },
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
                            text = { Text("Edit Kitty", color = OnSurface) },
                            onClick = { menuExpanded = false; onNavigateToEdit(kitty.id) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Kitty", color = Error) },
                            onClick = { menuExpanded = false; showDeleteDialog = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month selector
            item {
                MonthYearSelector(
                    months = months,
                    selectedMonth = state.selectedMonth,
                    selectedYear = state.selectedYear,
                    onSelect = { m, y -> viewModel.setSelectedMonthYear(m, y) }
                )
            }

            // Action buttons
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onNavigateToEdit(kitty.id) },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Outline)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = OnSurface)
                        Spacer(Modifier.width(6.dp))
                        Text("EDIT", color = OnSurface)
                    }
                    Button(
                        onClick = { editingMember = null; showAddMemberSheet = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp), tint = PaidGreen)
                        Spacer(Modifier.width(6.dp))
                        Text("ADD PERSON", color = PaidGreen)
                    }
                }
            }

            // Summary
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("TARGET", formatAmount(totalTarget), OnSurface, Modifier.weight(1f))
                    MetricCard("COLLECTED", formatAmount(paidTotal), PaidGreen, Modifier.weight(1f))
                }
            }

            // Hosts for selected month
            item {
                HostsBox(
                    hosts = hostsForMonth,
                    payouts = state.payoutsForMonth,
                    month = state.selectedMonth,
                    year = state.selectedYear
                )
            }

            // Payout section
            item {
                PayoutSection(
                    payouts = state.payoutsForMonth,
                    members = state.members,
                    month = state.selectedMonth,
                    year = state.selectedYear,
                    onRecord = { showPayoutSheet = true },
                    onDelete = { payout -> viewModel.deletePayout(payout) }
                )
            }

            // Sort chips
            item {
                SortChipRow(
                    currentSort = state.sortOrder,
                    onSortSelected = viewModel::setSortOrder
                )
            }

            // Member list
            if (sortedMembers.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Person,
                        title = "No members yet",
                        subtitle = "Tap ADD PERSON to add kitty members"
                    )
                }
            } else {
                items(sortedMembers, key = { it.id }) { member ->
                    val payment = state.payments[member.id]
                    MemberPaymentCard(
                        member = member,
                        payment = payment,
                        onClick = {
                            selectedMember = member
                            showPaymentSheet = true
                        },
                        onEdit = {
                            editingMember = member
                            showAddMemberSheet = true
                        },
                        onDelete = {
                            showDeleteMemberDialog = member
                        },
                        onViewHistory = {
                            historyMember = member
                            // Collect payments for this member
                            showHistorySheet = true
                        }
                    )
                }
            }
        }
    }

    // Payment entry sheet
    if (showPaymentSheet && selectedMember != null) {
        PaymentEntrySheet(
            member = selectedMember!!,
            existingPayment = state.payments[selectedMember!!.id],
            month = state.selectedMonth,
            year = state.selectedYear,
            accounts = allAccounts,
            onDismiss = { showPaymentSheet = false },
            onSave = { isPaid, amount, date, mode, onlineAccount, note ->
                viewModel.upsertPayment(
                    selectedMember!!.id, state.selectedMonth, state.selectedYear,
                    isPaid, amount, date, mode, onlineAccount, note
                )
                showPaymentSheet = false
            }
        )
    }

    // Add / Edit member sheet
    if (showAddMemberSheet) {
        AddMemberSheet(
            existingMember = editingMember,
            onDismiss = { showAddMemberSheet = false; editingMember = null },
            onSave = { name, hostMonth, amount ->
                val ex = editingMember
                if (ex == null) {
                    viewModel.addMember(kittyId, name, hostMonth, amount) {}
                } else {
                    viewModel.updateMember(ex, name, hostMonth, amount)
                }
                showAddMemberSheet = false
                editingMember = null
            }
        )
    }

    // Member payment history sheet
    if (showHistorySheet && historyMember != null) {
        val memberPaymentsFlow = remember(historyMember!!.id) {
            viewModel.getPaymentsForMember(historyMember!!.id)
        }
        val memberPayments by memberPaymentsFlow.collectAsState(initial = emptyList())
        MemberPaymentHistorySheet(
            member = historyMember!!,
            payments = memberPayments,
            onDismiss = { showHistorySheet = false; historyMember = null }
        )
    }

    // Payout sheet
    if (showPayoutSheet) {
        PayoutEntrySheet(
            members = state.members,
            existingPayout = null,
            month = state.selectedMonth,
            year = state.selectedYear,
            onDismiss = { showPayoutSheet = false },
            onSave = { hostMemberId, amount, date, notes ->
                viewModel.upsertPayout(
                    kittyId, state.selectedMonth, state.selectedYear,
                    hostMemberId, amount, date, notes
                )
                showPayoutSheet = false
            }
        )
    }

    // Delete member confirmation
    if (showDeleteMemberDialog != null) {
        val member = showDeleteMemberDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteMemberDialog = null },
            title = { Text("Remove Member?", color = OnSurface) },
            text = { Text("Remove \"${member.name}\" and all their payment records from this kitty?", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMember(member)
                    showDeleteMemberDialog = null
                }) { Text("Remove", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMemberDialog = null }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = SurfaceVariant
        )
    }

    // Delete kitty confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Kitty?", color = OnSurface) },
            text = { Text("This will permanently delete \"${kitty.name}\" and all its data.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteKitty(kitty)
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

// ─── Payout section ───
@Composable
private fun PayoutSection(
    payouts: List<KittyPayout>,
    members: List<KittyMember>,
    month: Int, year: Int,
    onRecord: () -> Unit,
    onDelete: (KittyPayout) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (payouts.isNotEmpty()) PrimaryContainer else SurfaceContainer
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, null, tint = if (payouts.isNotEmpty()) PaidGreen else OnSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Monthly Payout — ${monthName(month)} $year",
                    style = MaterialTheme.typography.titleSmall, color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onRecord,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("+ Add", style = MaterialTheme.typography.labelSmall, color = Primary) }
            }
            if (payouts.isEmpty()) {
                Text("No payout recorded yet", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            } else {
                payouts.forEach { payout ->
                    val hostName = members.find { it.id == payout.hostMemberId }?.name ?: "Unknown"
                    HorizontalDivider(color = Outline.copy(alpha = 0.3f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Paid to: $hostName — ${formatAmount(payout.amountReceived)}",
                                style = MaterialTheme.typography.bodySmall, color = PaidGreen
                            )
                            if (payout.receivedDate != null) {
                                Text("On ${formatDate(payout.receivedDate)}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                            if (payout.notes.isNotBlank()) {
                                Text(payout.notes, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = { onDelete(payout) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Error))
                        ) { Text("✕", style = MaterialTheme.typography.labelSmall, color = Error) }
                    }
                }
            }
        }
    }
}

// ─── Sort chip row ───
@Composable
private fun SortChipRow(
    currentSort: KittySort,
    onSortSelected: (KittySort) -> Unit
) {
    val options = listOf(
        KittySort.HOST_MONTH to "Host Month",
        KittySort.NAME to "Name",
        KittySort.PAID_STATUS to "Paid Status"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Sort:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        options.forEach { (sort, label) ->
            FilterChip(
                selected = currentSort == sort,
                onClick = { onSortSelected(sort) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryContainer,
                    selectedLabelColor = PaidGreen,
                    containerColor = SurfaceContainer,
                    labelColor = OnSurfaceVariant
                )
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MonthYearSelector(
    months: List<Pair<Int, Int>>,
    selectedMonth: Int,
    selectedYear: Int,
    onSelect: (Int, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = "${monthName(selectedMonth)} $selectedYear"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = SurfaceVariant
        ) {
            months.forEach { (m, y) ->
                DropdownMenuItem(
                    text = { Text("${monthName(m)} $y", color = OnSurface) },
                    onClick = { onSelect(m, y); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
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

// ─── Hosts for selected month ───
@Composable
private fun HostsBox(
    hosts: List<KittyMember>,
    payouts: List<KittyPayout>,
    month: Int,
    year: Int
) {
    if (hosts.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AmberContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star, null,
                    tint = Amber, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Host${if (hosts.size > 1) "s" else ""} — ${monthName(month)} $year",
                    style = MaterialTheme.typography.titleSmall, color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                val combinedPayout = payouts.sumOf { it.amountReceived }
                if (combinedPayout > 0) {
                    Text(
                        formatAmount(combinedPayout),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Amber
                    )
                }
            }
            hosts.forEach { host ->
                val payout = payouts.firstOrNull { it.hostMemberId == host.id }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Amber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            host.name.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Amber
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(host.name, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(1f))
                    Text(
                        if (payout != null) formatAmount(payout.amountReceived) else "—",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (payout != null) PaidGreen else OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberPaymentCard(
    member: KittyMember,
    payment: KittyPayment?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewHistory: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Text("Hosts: ${monthName(member.hostMonth)}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Amount:", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatAmount(payment?.amountPaid ?: 0.0),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status:", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    StatusChip(payment?.isPaid == true)
                }
                if (payment?.paymentMode != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Method:", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        ModeChip(payment.paymentMode.name)
                        if (payment.onlineAccountName != null) {
                            Spacer(Modifier.width(4.dp))
                            Text("(${payment.onlineAccountName})", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        }
                    }
                }
                if (payment?.datePaid != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Paid: ${formatDate(payment.datePaid)}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
            // Context menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = SurfaceVariant) {
                    DropdownMenuItem(text = { Text("Edit Member", color = OnSurface) },
                        onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Payment History", color = OnSurface) },
                        onClick = { showMenu = false; onViewHistory() })
                    DropdownMenuItem(text = { Text("Remove Member", color = Error) },
                        onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

// ─── Payout Entry Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayoutEntrySheet(
    members: List<KittyMember>,
    existingPayout: KittyPayout?,
    month: Int,
    year: Int,
    onDismiss: () -> Unit,
    onSave: (Long, Double, LocalDate?, String) -> Unit
) {
    var selectedHostId by remember {
        mutableStateOf(existingPayout?.hostMemberId ?: members.firstOrNull()?.id ?: 0L)
    }
    var amountStr by remember { mutableStateOf(existingPayout?.amountReceived?.toString() ?: "") }
    var datePaid by remember { mutableStateOf(existingPayout?.receivedDate ?: LocalDate.now()) }
    var notes by remember { mutableStateOf(existingPayout?.notes ?: "") }
    var memberExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        contentWindowInsets = { WindowInsets(0.dp) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Record Payout — ${monthName(month)} $year",
                style = MaterialTheme.typography.headlineMedium, color = OnSurface)

            // Member dropdown
            val selectedMemberName = members.find { it.id == selectedHostId }?.name ?: "Select member"
            ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                OutlinedTextField(
                    value = selectedMemberName, onValueChange = {}, readOnly = true,
                    label = { Text("Host Member (Receiver)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface
                    )
                )
                ExposedDropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false },
                    containerColor = SurfaceVariant) {
                    members.forEach { member ->
                        DropdownMenuItem(
                            text = { Text("${member.name} (Host: ${monthName(member.hostMonth)})", color = OnSurface) },
                            onClick = { selectedHostId = member.id; memberExpanded = false }
                        )
                    }
                }
            }

            AmountInputField(value = amountStr, onValueChange = { amountStr = it; amountError = false },
                label = "Amount Received", isError = amountError)
            if (amountError) Text("Enter a valid amount", style = MaterialTheme.typography.bodySmall, color = Error)

            DateInputField(date = datePaid, onDateSelected = { datePaid = it }, label = "Received Date")

            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                    focusedLabelColor = Primary, unfocusedLabelColor = OnSurfaceVariant
                )
            )

            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) { amountError = true; return@Button }
                    onSave(selectedHostId, amount, datePaid, notes.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save Payout", color = OnPrimary) }

            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)) { Text("Cancel", color = OnSurface) }
        }
    }
}
