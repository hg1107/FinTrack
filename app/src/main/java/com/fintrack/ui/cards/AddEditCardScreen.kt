package com.fintrack.ui.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    cardId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isEdit = cardId != null
    val existing = if (isEdit) state.cards.find { it.id == cardId } else null

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var lastFour by remember(existing) { mutableStateOf(existing?.lastFourDigits ?: "") }
    var limitStr by remember(existing) { mutableStateOf(existing?.creditLimit?.toString() ?: "") }
    var billingDueDayStr by remember(existing) { mutableStateOf(existing?.billingDueDay?.takeIf { it > 0 }?.toString() ?: "") }
    var statementDayStr by remember(existing) { mutableStateOf(existing?.statementDay?.takeIf { it > 0 }?.toString() ?: "") }
    var nameError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Edit Card" else "Add Card",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = false },
                        label = { Text("Card Name") },
                        placeholder = { Text("e.g. HDFC Millenia", color = OnSurfaceVariant) },
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
                    if (nameError) Text("Card name is required", style = MaterialTheme.typography.bodySmall, color = Error)

                    OutlinedTextField(
                        value = lastFour,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) lastFour = it },
                        label = { Text("Last 4 Digits (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Outline,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = OnSurfaceVariant
                        )
                    )

                    AmountInputField(
                        value = limitStr,
                        onValueChange = { limitStr = it },
                        label = "Credit Limit"
                    )

                    OutlinedTextField(
                        value = billingDueDayStr,
                        onValueChange = {
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 1..31)) billingDueDayStr = it
                        },
                        label = { Text("Payment Due Day (1–31, optional)") },
                        placeholder = { Text("e.g. 15", color = OnSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Outline,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = OnSurfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = statementDayStr,
                        onValueChange = {
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 1..31)) statementDayStr = it
                        },
                        label = { Text("Statement Generation Day (1–31, optional)") },
                        placeholder = { Text("e.g. 5", color = OnSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    val limit = limitStr.toDoubleOrNull() ?: 0.0
                    val dueDay = billingDueDayStr.toIntOrNull() ?: 0
                    val stmDay = statementDayStr.toIntOrNull() ?: 0
                    if (isEdit && existing != null) {
                        viewModel.updateCard(existing, name, lastFour, limit, dueDay, stmDay)
                    } else {
                        viewModel.saveCard(name, lastFour, limit, dueDay, stmDay) {}
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Card", color = OnPrimary)
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = OnSurface) }
        }
    }
}
