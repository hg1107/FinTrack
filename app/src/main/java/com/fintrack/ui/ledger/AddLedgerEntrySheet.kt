package com.fintrack.ui.ledger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fintrack.ui.shared.AmountInputField
import com.fintrack.ui.shared.DateInputField
import com.fintrack.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerEntrySheet(
    existingEntry: com.fintrack.data.local.entity.LedgerEntry? = null,
    onDismiss: () -> Unit,
    onSave: (Double, LocalDate, String?, Boolean) -> Unit
) {
    var amountStr by remember { mutableStateOf(existingEntry?.amount?.toString() ?: "") }
    var date by remember { mutableStateOf(existingEntry?.date ?: LocalDate.now()) }
    var note by remember { mutableStateOf(existingEntry?.note ?: "") }
    var isRecurring by remember { mutableStateOf(existingEntry?.isRecurring ?: false) }
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
                if (existingEntry == null) "Add Entry" else "Edit Entry",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface
            )

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it; amountError = false },
                label = "Amount",
                isError = amountError
            )
            if (amountError) Text("Enter a valid amount", style = MaterialTheme.typography.bodySmall, color = Error)

            DateInputField(date = date, onDateSelected = { date = it }, label = "Date")

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring entry", style = MaterialTheme.typography.titleMedium, color = OnSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = PrimaryContainer)
                )
            }

            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) { amountError = true; return@Button }
                    onSave(amount, date, note.trim().ifBlank { null }, isRecurring)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save Entry", color = OnPrimary) }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = OnSurface) }
        }
    }
}
