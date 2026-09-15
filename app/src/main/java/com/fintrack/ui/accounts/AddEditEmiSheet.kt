package com.fintrack.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fintrack.data.local.entity.Emi
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmiSheet(
    existing: Emi?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, Int, LocalDate?, Int, Boolean) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var amountStr by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var dueDayStr by remember { mutableStateOf(existing?.dueDayOfMonth?.toString() ?: "") }
    var tenureStr by remember { mutableStateOf(existing?.tenureMonths?.takeIf { it > 0 }?.toString() ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate) }
    var reminderDaysStr by remember { mutableStateOf(existing?.reminderDaysBefore?.toString() ?: "2") }
    var reminderEnabled by remember { mutableStateOf(existing?.isReminderEnabled ?: true) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var labelError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var dayError by remember { mutableStateOf(false) }

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
                if (existing == null) "Add EMI" else "Edit EMI",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it; labelError = false },
                label = { Text("Loan / EMI Name") },
                placeholder = { Text("e.g. Home Loan", color = OnSurfaceVariant) },
                isError = labelError,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                    focusedLabelColor = Primary, unfocusedLabelColor = OnSurfaceVariant
                )
            )

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it; amountError = false },
                label = "Monthly EMI Amount",
                isError = amountError
            )
            if (amountError) Text("Enter a valid amount", style = MaterialTheme.typography.bodySmall, color = Error)

            OutlinedTextField(
                value = dueDayStr,
                onValueChange = {
                    if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 1..31)) {
                        dueDayStr = it; dayError = false
                    }
                },
                label = { Text("Due Day of Month (1–31)") },
                isError = dayError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                    focusedLabelColor = Primary, unfocusedLabelColor = OnSurfaceVariant
                )
            )
            if (dayError) Text("Enter a valid day (1–31)", style = MaterialTheme.typography.bodySmall, color = Error)

            // Tenure
            OutlinedTextField(
                value = tenureStr,
                onValueChange = {
                    if (it.isEmpty() || it.toIntOrNull() != null) tenureStr = it
                },
                label = { Text("Total Installments (optional, e.g. 36 months)") },
                placeholder = { Text("Leave blank if indefinite", color = OnSurfaceVariant) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                    focusedLabelColor = Primary, unfocusedLabelColor = OnSurfaceVariant
                )
            )

            // Start date (for remaining calculation)
            DateInputField(
                date = startDate ?: LocalDate.now(),
                onDateSelected = { startDate = it },
                label = "EMI Start Date (optional — for remaining count)"
            )

            // Reminder toggle
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable Reminder", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        Text("Push notification before due date", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = PrimaryContainer)
                    )
                }
            }

            if (reminderEnabled) {
                OutlinedTextField(
                    value = reminderDaysStr,
                    onValueChange = {
                        if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 1..14)) reminderDaysStr = it
                    },
                    label = { Text("Remind N days before (1–14)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                        focusedLabelColor = Primary, unfocusedLabelColor = OnSurfaceVariant
                    )
                )
            }

            Button(
                onClick = {
                    if (label.isBlank()) { labelError = true; return@Button }
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) { amountError = true; return@Button }
                    val day = dueDayStr.toIntOrNull()
                    if (day == null || day !in 1..31) { dayError = true; return@Button }
                    val tenure = tenureStr.toIntOrNull() ?: 0
                    val reminderDays = reminderDaysStr.toIntOrNull() ?: 2
                    onSave(label.trim(), amount, day, tenure, startDate, reminderDays, reminderEnabled)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save EMI", color = OnPrimary)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = OnSurface) }
        }
    }
}
