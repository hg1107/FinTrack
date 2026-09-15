package com.fintrack.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBankAccountScreen(
    accountId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: BankAccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isEdit = accountId != null
    val existing = if (isEdit) state.accounts.find { it.id == accountId } else null

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var lastFour by remember(existing) { mutableStateOf(existing?.lastFourDigits ?: "") }
    var nameError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Edit Account" else "Add Bank Account",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Enter your account details to start tracking your finances with precision.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )

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
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. SBI Savings", color = OnSurfaceVariant) },
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
                    if (nameError) Text("Account name is required", style = MaterialTheme.typography.bodySmall, color = Error)

                    OutlinedTextField(
                        value = lastFour,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) lastFour = it },
                        label = { Text("Last 4 Digits (optional)") },
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

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    if (isEdit && existing != null) {
                        viewModel.updateAccount(existing, name, lastFour)
                    } else {
                        viewModel.saveAccount(name, lastFour) {}
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(0.dp)) // spacer trick
                Text("✓  Save Account", color = OnPrimary)
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = OnSurface) }
        }
    }
}
