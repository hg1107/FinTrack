package com.fintrack.ui.kitty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*
import java.time.LocalDate

data class MemberDraft(
    val name: String = "",
    val hostMonth: Int = 1,
    val amountStr: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditKittyScreen(
    kittyId: Long?,     // null = new kitty
    onNavigateBack: () -> Unit,
    viewModel: KittyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isEdit = kittyId != null

    LaunchedEffect(kittyId) {
        if (kittyId != null) viewModel.selectKitty(kittyId)
    }

    val existingKitty = if (isEdit) state.selectedKitty else null

    var kittyName by remember(existingKitty) { mutableStateOf(existingKitty?.name ?: "") }
    var startMonth by remember(existingKitty) { mutableStateOf(existingKitty?.startMonth ?: LocalDate.now().monthValue) }
    var startYear by remember(existingKitty) { mutableStateOf(existingKitty?.startYear ?: LocalDate.now().year) }
    var members by remember { mutableStateOf(listOf(MemberDraft())) }
    var nameError by remember { mutableStateOf(false) }
    var startMonthExpanded by remember { mutableStateOf(false) }

    // Load existing members for edit
    LaunchedEffect(state.members) {
        if (isEdit && state.members.isNotEmpty()) {
            members = state.members.map { MemberDraft(it.name, it.hostMonth, it.amount.toString()) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Edit Kitty" else "Add New Kitty",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface)
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
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = kittyName,
                            onValueChange = { kittyName = it; nameError = false },
                            label = { Text("Kitty Name") },
                            placeholder = { Text("e.g. Summer Vacation Fund", color = OnSurfaceVariant) },
                            isError = nameError,
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
                        if (nameError) Text("Kitty name is required", style = MaterialTheme.typography.bodySmall, color = Error)

                        // Start Month
                        ExposedDropdownMenuBox(expanded = startMonthExpanded, onExpandedChange = { startMonthExpanded = it }) {
                            OutlinedTextField(
                                value = "${monthName(startMonth)} $startYear",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Start Month") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startMonthExpanded) },
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
                                expanded = startMonthExpanded,
                                onDismissRequest = { startMonthExpanded = false },
                                containerColor = SurfaceVariant
                            ) {
                                // June 2025 – today + 36 months (dynamic)
                                val options = buildList {
                                    var yr = 2025; var mo = 6
                                    val today = java.time.LocalDate.now()
                                    val endYr = today.year + 3
                                    val endMo = today.monthValue
                                    while (yr < endYr || (yr == endYr && mo <= endMo)) {
                                        add(Pair(mo, yr))
                                        mo++; if (mo > 12) { mo = 1; yr++ }
                                    }
                                }
                                options.forEach { (m, y) ->
                                    DropdownMenuItem(
                                        text = { Text("${monthName(m)} $y", color = OnSurface) },
                                        onClick = { startMonth = m; startYear = y; startMonthExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Members", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
            }

            itemsIndexed(members) { index, draft ->
                MemberDraftCard(
                    draft = draft,
                    canDelete = members.size > 1,
                    onUpdate = { updated -> members = members.toMutableList().also { it[index] = updated } },
                    onDelete = { members = members.toMutableList().also { it.removeAt(index) } }
                )
            }

            item {
                TextButton(
                    onClick = { members = members + MemberDraft() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, tint = Primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Member", color = Primary)
                }
            }

            item {
                Button(
                    onClick = {
                        if (kittyName.isBlank()) { nameError = true; return@Button }
                        if (isEdit && existingKitty != null) {
                            viewModel.updateKitty(existingKitty, kittyName, startMonth, startYear)
                            // Update each member
                            members.forEachIndexed { idx, draft ->
                                val amount = draft.amountStr.toDoubleOrNull() ?: 0.0
                                val existing = state.members.getOrNull(idx)
                                if (existing != null) {
                                    viewModel.updateMember(existing, draft.name, draft.hostMonth, amount)
                                } else {
                                    viewModel.addMember(existingKitty.id, draft.name, draft.hostMonth, amount) {}
                                }
                            }
                        } else {
                            viewModel.saveKitty(kittyName, startMonth, startYear) { newId ->
                                members.forEach { draft ->
                                    val amount = draft.amountStr.toDoubleOrNull() ?: 0.0
                                    viewModel.addMember(newId, draft.name, draft.hostMonth, amount) {}
                                }
                            }
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Kitty", color = OnPrimary)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberDraftCard(
    draft: MemberDraft,
    canDelete: Boolean,
    onUpdate: (MemberDraft) -> Unit,
    onDelete: () -> Unit
) {
    var hostMonthExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onUpdate(draft.copy(name = it)) },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Alice", color = OnSurfaceVariant) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                        focusedLabelColor = Primary, unfocusedLabelColor = OnSurfaceVariant
                    )
                )
                if (canDelete) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = Error)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = hostMonthExpanded,
                    onExpandedChange = { hostMonthExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = monthName(draft.hostMonth),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Host Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hostMonthExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = hostMonthExpanded,
                        onDismissRequest = { hostMonthExpanded = false },
                        containerColor = SurfaceVariant
                    ) {
                        (1..12).forEach { m ->
                            DropdownMenuItem(
                                text = { Text(monthName(m), color = OnSurface) },
                                onClick = { onUpdate(draft.copy(hostMonth = m)); hostMonthExpanded = false }
                            )
                        }
                    }
                }

                AmountInputField(
                    value = draft.amountStr,
                    onValueChange = { onUpdate(draft.copy(amountStr = it)) },
                    label = "Amount",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
