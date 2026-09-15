package com.fintrack.ui.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fintrack.data.local.entity.TransactionType
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*
import java.time.LocalDate

/**
 * Generic Add/Edit Transaction bottom sheet, reused for both credit-card
 * and bank-account transactions (no longer tied to CardTransaction entity).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    existingDate: LocalDate? = null,
    existingAmount: Double? = null,
    existingType: TransactionType = TransactionType.DEBIT,
    existingNote: String = "",
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double, TransactionType, String) -> Unit
) {
    var date by remember { mutableStateOf(existingDate ?: LocalDate.now()) }
    var amountStr by remember { mutableStateOf(existingAmount?.toString() ?: "") }
    var type by remember { mutableStateOf(existingType) }
    var note by remember { mutableStateOf(existingNote) }
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
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (existingDate == null && existingAmount == null) "Add Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface
            )

            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransactionType.entries.forEach { t ->
                    val isSelected = type == t
                    FilterChip(
                        selected = isSelected,
                        onClick = { type = t },
                        label = { Text(t.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (t == TransactionType.DEBIT) ErrorContainer else PrimaryContainer,
                            selectedLabelColor = if (t == TransactionType.DEBIT) DebitRed else CreditGreen,
                            containerColor = SurfaceContainer,
                            labelColor = OnSurfaceVariant
                        )
                    )
                }
            }

            DateInputField(date = date, onDateSelected = { date = it }, label = "Date")

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it; amountError = false },
                label = "Amount",
                isError = amountError
            )
            if (amountError) Text("Enter a valid amount", style = MaterialTheme.typography.bodySmall, color = Error)

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Description") },
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
                    if (amount == null || amount <= 0) { amountError = true; return@Button }
                    onSave(date, amount, type, note)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Transaction", color = OnPrimary)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = OnSurface) }
        }
    }
}
