package com.fintrack.ui.kitty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fintrack.data.local.entity.BankAccount
import com.fintrack.data.local.entity.KittyMember
import com.fintrack.data.local.entity.KittyPayment
import com.fintrack.data.local.entity.PaymentMode
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*
import java.time.LocalDate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// ─────────── Payment Entry Bottom Sheet ───────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntrySheet(
    member: KittyMember,
    existingPayment: KittyPayment?,
    month: Int,
    year: Int,
    accounts: List<BankAccount> = emptyList(),
    hosts: List<KittyMember> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Boolean, Double, LocalDate?, PaymentMode?, String?, String?) -> Unit
) {
    var isPaid by remember { mutableStateOf(existingPayment?.isPaid ?: false) }
    var amountStr by remember { mutableStateOf((existingPayment?.amountPaid?.takeIf { it > 0 } ?: member.amount).toString()) }
    var datePaid by remember { mutableStateOf(existingPayment?.datePaid ?: LocalDate.now()) }
    var selectedMode by remember { mutableStateOf(existingPayment?.paymentMode ?: PaymentMode.CASH) }
    var onlineAccount by remember { mutableStateOf(existingPayment?.onlineAccountName ?: "") }
    var note by remember { mutableStateOf(existingPayment?.note ?: "") }
    var amountError by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Text(
                "Payment — ${member.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface
            )
            Text(
                "${monthName(month)} $year",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )

            // Paid toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mark as Paid", style = MaterialTheme.typography.titleMedium, color = OnSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = isPaid,
                    onCheckedChange = { isPaid = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = PrimaryContainer)
                )
            }

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it; amountError = false },
                label = "Amount",
                isError = amountError
            )
            if (amountError) Text("Enter a valid amount", style = MaterialTheme.typography.bodySmall, color = Error)

            DateInputField(date = datePaid, onDateSelected = { datePaid = it }, label = "Date Paid")

            // Payment mode selector
            Text("Payment Mode", style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    val label = when (mode) {
                        PaymentMode.CASH -> "Cash"
                        PaymentMode.ONLINE -> "Online"
                        PaymentMode.OFFICE -> "Office"
                        PaymentMode.HOST -> "Host"
                        PaymentMode.SELF -> "Self"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMode = mode },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (mode) {
                                PaymentMode.CASH -> PrimaryContainer
                                PaymentMode.ONLINE -> Color(0xFF1E3A8A)
                                PaymentMode.OFFICE -> Color(0xFF312E81)
                                PaymentMode.HOST -> AmberContainer
                                PaymentMode.SELF -> Color(0xFF134E4A)
                            },
                            selectedLabelColor = when (mode) {
                                PaymentMode.CASH -> PaidGreen
                                PaymentMode.ONLINE -> Color(0xFF60A5FA)
                                PaymentMode.OFFICE -> Color(0xFFA78BFA)
                                PaymentMode.HOST -> Amber
                                PaymentMode.SELF -> Color(0xFF2DD4BF)
                            },
                            containerColor = SurfaceContainer,
                            labelColor = OnSurfaceVariant
                        )
                    )
                }
            }

            // Informational helper boxes based on selected mode
            when (selectedMode) {
                PaymentMode.HOST -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                            val hostNames = hosts.map { it.name }.filter { it.isNotBlank() }
                            Text(
                                text = if (hostNames.isNotEmpty()) "Paid directly to host: ${hostNames.joinToString(", ")}"
                                       else "Paid directly to this month's host",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface
                            )
                        }
                    }
                }
                PaymentMode.SELF -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF134E4A).copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Self payment / host self-adjustment for ${monthName(month)} $year",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface
                            )
                        }
                    }
                }
                PaymentMode.OFFICE -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF312E81).copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Payment recorded under Raj Office ledger",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface
                            )
                        }
                    }
                }
                PaymentMode.CASH -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = PaidGreen, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Cash payment recorded under Cash ledger",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface
                            )
                        }
                    }
                }
                PaymentMode.ONLINE -> { /* Handled below by bank account / UPI field */ }
            }

            if (selectedMode == PaymentMode.ONLINE) {
                if (accounts.isNotEmpty()) {
                    // Dropdown of saved bank accounts
                    var accountExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = accountExpanded,
                        onExpandedChange = { accountExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = onlineAccount.ifBlank { "Select account..." },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pay from Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false },
                            containerColor = SurfaceVariant
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name, color = OnSurface) },
                                    onClick = {
                                        onlineAccount = account.name
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = onlineAccount,
                        onValueChange = { onlineAccount = it },
                        label = { Text("Account / UPI (e.g. GPay)") },
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
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
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

            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount < 0) { amountError = true; return@Button }
                    onSave(
                        isPaid, amount,
                        if (isPaid) datePaid else null,
                        if (isPaid) selectedMode else null,
                        if (isPaid && selectedMode == PaymentMode.ONLINE) onlineAccount.ifBlank { null } else null,
                        note.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Payment", color = OnPrimary)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = OnSurface)
            }
        }
    }
}

// ─────────── Add Member Bottom Sheet ───────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberSheet(
    existingMember: KittyMember? = null,
    onDismiss: () -> Unit,
    onSave: (String, Int, Double) -> Unit
) {
    var name by remember { mutableStateOf(existingMember?.name ?: "") }
    var hostMonthStr by remember { mutableStateOf(existingMember?.hostMonth?.toString() ?: "1") }
    var amountStr by remember { mutableStateOf(existingMember?.amount?.toString() ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var hostMonthExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Text(
                if (existingMember == null) "Add Member" else "Edit Member",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Member Name") },
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

            // Host Month dropdown
            val hostMonth = hostMonthStr.toIntOrNull() ?: 1
            ExposedDropdownMenuBox(
                expanded = hostMonthExpanded,
                onExpandedChange = { hostMonthExpanded = it }
            ) {
                OutlinedTextField(
                    value = monthName(hostMonth),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Host Month") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hostMonthExpanded) },
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
                    expanded = hostMonthExpanded,
                    onDismissRequest = { hostMonthExpanded = false },
                    containerColor = SurfaceContainer
                ) {
                    (1..12).forEach { m ->
                        DropdownMenuItem(
                            text = { Text(monthName(m), color = OnSurface) },
                            onClick = { hostMonthStr = m.toString(); hostMonthExpanded = false }
                        )
                    }
                }
            }

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it; amountError = false },
                label = "Monthly Amount",
                isError = amountError
            )

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) { amountError = true; return@Button }
                    onSave(name.trim(), hostMonth, amount)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Member", color = OnPrimary)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = OnSurface) }
        }
    }
}
