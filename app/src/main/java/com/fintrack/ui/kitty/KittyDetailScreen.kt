package com.fintrack.ui.kitty
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import com.fintrack.data.local.entity.PaymentMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showMarkAllPaidDialog by remember { mutableStateOf(false) }
    var markAllPaidMode by remember { mutableStateOf(PaymentMode.CASH) }
    var showSearchBar by remember { mutableStateOf(false) }
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
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = state.memberSearchQuery,
                            onValueChange = viewModel::setMemberSearchQuery,
                            placeholder = { Text("Search members...", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (state.memberSearchQuery.isNotEmpty()) viewModel.setMemberSearchQuery("")
                                    else showSearchBar = false
                                }) {
                                    Icon(Icons.Default.Close, null, tint = OnSurface)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Outline,
                                focusedTextColor = OnSurface,
                                unfocusedTextColor = OnSurface
                            )
                        )
                    } else {
                        Text(kitty.name, style = MaterialTheme.typography.headlineMedium, color = OnSurface)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface)
                    }
                },
                actions = {
                    if (!showSearchBar) {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, "Search Members", tint = OnSurface)
                        }
                        IconButton(onClick = {
                            shareKittyStatus(
                                context = context,
                                kittyName = kitty.name,
                                month = state.selectedMonth,
                                year = state.selectedYear,
                                target = totalTarget,
                                collected = paidTotal,
                                members = state.members,
                                payments = state.payments,
                                hosts = hostsForMonth
                            )
                        }) {
                            Icon(Icons.Default.Share, "Share Status", tint = OnSurface)
                        }
                    }
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
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = PaidGreen) },
                            text = { Text("Mark All as Paid", color = PaidGreen) },
                            onClick = { menuExpanded = false; showMarkAllPaidDialog = true }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Share, null, tint = OnSurface) },
                            text = { Text("Share Status Report", color = OnSurface) },
                            onClick = {
                                menuExpanded = false
                                shareKittyStatus(
                                    context = context,
                                    kittyName = kitty.name,
                                    month = state.selectedMonth,
                                    year = state.selectedYear,
                                    target = totalTarget,
                                    collected = paidTotal,
                                    members = state.members,
                                    payments = state.payments,
                                    hosts = hostsForMonth
                                )
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = OnSurface) },
                            text = { Text("Edit Kitty", color = OnSurface) },
                            onClick = { menuExpanded = false; onNavigateToEdit(kitty.id) }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.RestartAlt, null, tint = Error) },
                            text = { Text("Reset All to Unpaid", color = Error) },
                            onClick = { menuExpanded = false; showResetDialog = true }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Error) },
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToEdit(kitty.id) },
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(Outline)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp), tint = OnSurface)
                            Spacer(Modifier.width(4.dp))
                            Text("EDIT", color = OnSurface, style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { editingMember = null; showAddMemberSheet = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(15.dp), tint = PaidGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("ADD PERSON", color = PaidGreen, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showMarkAllPaidDialog = true },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = PaidGreen.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp), tint = PaidGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("MARK ALL PAID", color = PaidGreen, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(Error.copy(alpha = 0.6f))),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = ErrorContainer.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(15.dp), tint = Error)
                            Spacer(Modifier.width(4.dp))
                            Text("RESET", color = Error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Summary with Progress Bar & Collection Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("TARGET", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                Spacer(Modifier.height(2.dp))
                                Text(formatAmount(totalTarget), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OnSurface)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("COLLECTED", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                Spacer(Modifier.height(2.dp))
                                Text(formatAmount(paidTotal), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = PaidGreen)
                            }
                            val paidCount = state.payments.values.count { it?.isPaid == true }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("STATUS", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                Spacer(Modifier.height(2.dp))
                                Text("$paidCount/${state.members.size} Paid", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = if (paidCount == state.members.size && state.members.isNotEmpty()) PaidGreen else Amber)
                            }
                        }
                        val progress = if (totalTarget > 0) (paidTotal / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PaidGreen,
                            trackColor = SurfaceContainer
                        )

                        // Mode Breakdown (Cash, Online, Office, Host, Self)
                        val paidPayments = state.payments.values.filterNotNull().filter { it.isPaid }
                        val modeTotals = PaymentMode.entries.mapNotNull { mode ->
                            val sum = paidPayments.filter { it.paymentMode == mode }.sumOf { it.amountPaid }
                            if (sum > 0) mode to sum else null
                        }
                        if (modeTotals.isNotEmpty()) {
                            HorizontalDivider(color = Outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("By Mode:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                modeTotals.forEach { (mode, sum) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ModeChip(mode.name)
                                            Text(
                                                formatAmount(sum),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = OnSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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

            // Filter & Sort chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val paidCount = state.payments.values.count { it?.isPaid == true }
                    val pendingCount = state.members.size - paidCount

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        listOf(
                            KittyPaymentFilter.ALL to "All (${state.members.size})",
                            KittyPaymentFilter.PAID to "Paid ($paidCount)",
                            KittyPaymentFilter.PENDING to "Pending ($pendingCount)"
                        ).forEach { (filter, label) ->
                            FilterChip(
                                selected = state.filterStatus == filter,
                                onClick = { viewModel.setFilterStatus(filter) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (filter) {
                                        KittyPaymentFilter.PAID -> PaidGreen.copy(alpha = 0.2f)
                                        KittyPaymentFilter.PENDING -> Amber.copy(alpha = 0.2f)
                                        else -> PrimaryContainer
                                    },
                                    selectedLabelColor = when (filter) {
                                        KittyPaymentFilter.PAID -> PaidGreen
                                        KittyPaymentFilter.PENDING -> Amber
                                        else -> PaidGreen
                                    },
                                    containerColor = SurfaceContainer,
                                    labelColor = OnSurfaceVariant
                                )
                            )
                        }
                    }

                    SortChipRow(
                        currentSort = state.sortOrder,
                        onSortSelected = viewModel::setSortOrder
                    )
                }
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
                        onTogglePaid = {
                            viewModel.toggleMemberPaidStatus(member)
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
            hosts = hostsForMonth,
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

    // Mark all as paid confirmation
    if (showMarkAllPaidDialog) {
        val unpaidCount = state.members.count { state.payments[it.id]?.isPaid != true }
        AlertDialog(
            onDismissRequest = { showMarkAllPaidDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaidGreen) },
            title = { Text("Mark All as Paid?", color = OnSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Mark all $unpaidCount unpaid members as Paid for ${monthName(state.selectedMonth)} ${state.selectedYear}.\n\nSelect payment mode:",
                        color = OnSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMode.entries.forEach { mode ->
                            val isSelected = markAllPaidMode == mode
                            val label = when (mode) {
                                PaymentMode.CASH -> "Cash"
                                PaymentMode.ONLINE -> "Online"
                                PaymentMode.OFFICE -> "Office"
                                PaymentMode.HOST -> "Host"
                                PaymentMode.SELF -> "Self"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { markAllPaidMode = mode },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAllAsPaid(markAllPaidMode)
                        showMarkAllPaidDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Confirm Paid", color = OnPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showMarkAllPaidDialog = false }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = SurfaceVariant
        )
    }

    // Reset all payments confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Error) },
            title = { Text("Reset All Payments?", color = OnSurface) },
            text = {
                Text(
                    "This will set all members' payment status to Unpaid for ${monthName(state.selectedMonth)} ${state.selectedYear}.\n\nEach member's contribution amount will be kept intact.\n\nAre you sure you want to proceed?",
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllPayments()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Reset All to Unpaid", color = OnPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = SurfaceVariant
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
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(Error))
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
    val currentIndex = months.indexOfFirst { it.first == selectedMonth && it.second == selectedYear }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                if (currentIndex > 0) {
                    val (prevM, prevY) = months[currentIndex - 1]
                    onSelect(prevM, prevY)
                }
            },
            enabled = currentIndex > 0
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Previous Month",
                tint = if (currentIndex > 0) OnSurface else OnSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = Primary, modifier = Modifier.size(18.dp)) },
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

        IconButton(
            onClick = {
                if (currentIndex in 0 until months.size - 1) {
                    val (nextM, nextY) = months[currentIndex + 1]
                    onSelect(nextM, nextY)
                }
            },
            enabled = currentIndex in 0 until months.size - 1
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next Month",
                tint = if (currentIndex in 0 until months.size - 1) OnSurface else OnSurfaceVariant.copy(alpha = 0.3f)
            )
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
    onTogglePaid: () -> Unit,
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
                val displayAmount = if (payment != null && payment.amountPaid > 0) payment.amountPaid else member.amount
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Amount:", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatAmount(displayAmount),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status:", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    StatusChip(
                        isPaid = payment?.isPaid == true,
                        onClick = onTogglePaid
                    )
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
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (payment?.isPaid == true) "Mark as Unpaid" else "Mark as Paid",
                                color = if (payment?.isPaid == true) Error else PaidGreen
                            )
                        },
                        onClick = { showMenu = false; onTogglePaid() }
                    )
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        sheetState = sheetState,
        containerColor = SurfaceVariant,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
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

// ─── Share Monthly Status Summary Helper ───
private fun shareKittyStatus(
    context: Context,
    kittyName: String,
    month: Int,
    year: Int,
    target: Double,
    collected: Double,
    members: List<KittyMember>,
    payments: Map<Long, KittyPayment?>,
    hosts: List<KittyMember>
) {
    val pending = (target - collected).coerceAtLeast(0.0)
    val paidMembers = members.filter { payments[it.id]?.isPaid == true }
    val pendingMembers = members.filter { payments[it.id]?.isPaid != true }

    val message = buildString {
        appendLine("FinTrack — Kitty Collection Status")
        appendLine("Kitty: $kittyName")
        appendLine("Month: ${monthName(month)} $year")
        if (hosts.isNotEmpty()) {
            appendLine("Host(s): ${hosts.joinToString { it.name }}")
        }
        appendLine("------------------------------")
        appendLine("Target: ${formatAmount(target)}")
        appendLine("Collected: ${formatAmount(collected)} (${paidMembers.size}/${members.size} Paid)")
        appendLine("Pending: ${formatAmount(pending)} (${pendingMembers.size} Unpaid)")
        appendLine("------------------------------")

        if (paidMembers.isNotEmpty()) {
            appendLine("\nPaid Members (${paidMembers.size}):")
            paidMembers.forEachIndexed { i, m ->
                val p = payments[m.id]
                val amt = if (p != null && p.amountPaid > 0) p.amountPaid else m.amount
                val modeLabel = when (p?.paymentMode) {
                    PaymentMode.CASH -> "Cash"
                    PaymentMode.ONLINE -> "Online" + (if (p.onlineAccountName != null) " - ${p.onlineAccountName}" else "")
                    PaymentMode.OFFICE -> "Raj Office"
                    PaymentMode.HOST -> "Host"
                    PaymentMode.SELF -> "Self"
                    null -> ""
                }
                val modeStr = if (modeLabel.isNotBlank()) " ($modeLabel)" else ""
                appendLine("${i + 1}. ${m.name}: ${formatAmount(amt)}$modeStr")
            }
        }

        if (pendingMembers.isNotEmpty()) {
            appendLine("\nPending Members (${pendingMembers.size}):")
            pendingMembers.forEachIndexed { i, m ->
                val p = payments[m.id]
                val amt = if (p != null && p.amountPaid > 0) p.amountPaid else m.amount
                appendLine("${i + 1}. ${m.name}: ${formatAmount(amt)}")
            }
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$kittyName — ${monthName(month)} $year Status")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "Share Kitty Status Report"))
}
