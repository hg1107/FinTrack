package com.fintrack.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgersScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: LedgerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ledgers", style = MaterialTheme.typography.headlineMedium, color = OnSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            FinFab(onClick = { showAddSheet = true })
        },
        containerColor = Background
    ) { padding ->
        if (state.ledgers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Bookmarks,
                    title = "No ledgers yet",
                    subtitle = "Tap + to create a custom ledger"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.ledgers, key = { it.id }) { ledger ->
                    val summary = state.summaries[ledger.id]
                    LedgerCard(
                        ledger = ledger,
                        total = summary?.total ?: 0.0,
                        entryCount = summary?.entryCount ?: 0,
                        onClick = { onNavigateToDetail(ledger.id) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddLedgerSheet(
            onDismiss = { showAddSheet = false },
            onCreate = { name, colorHex, iconId ->
                viewModel.createLedger(name, colorHex, iconId) { newId ->
                    showAddSheet = false
                    onNavigateToDetail(newId)
                }
            }
        )
    }
}

// ─── Ledger list card ───
@Composable
private fun LedgerCard(
    ledger: com.fintrack.data.local.entity.Ledger,
    total: Double,
    entryCount: Int,
    onClick: () -> Unit
) {
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(ledger.colorHex)) }
        .getOrDefault(Primary)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ledgerIcon(ledger.iconId),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ledger.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(Modifier.width(6.dp))
                    if (ledger.type == com.fintrack.data.local.entity.LedgerType.AUTO) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PrimaryContainer
                        ) {
                            Text(
                                "AUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaidGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Text(
                formatAmount(total),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}

// ─── Add Ledger Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLedgerSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf("#2ECC71") }
    var selectedIcon by remember { mutableStateOf("book") }

    val colorOptions = listOf(
        "#2ECC71" to "Emerald",
        "#3498DB" to "Blue",
        "#9B59B6" to "Purple",
        "#E67E22" to "Orange",
        "#E74C3C" to "Red",
        "#F39C12" to "Amber",
        "#1ABC9C" to "Teal",
        "#E91E63" to "Pink"
    )
    val iconOptions = listOf(
        "book" to Icons.Default.Book,
        "payments" to Icons.Default.Payments,
        "business" to Icons.Default.Business,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "home" to Icons.Default.Home,
        "cleaning" to Icons.Default.CleaningServices,
        "local_offer" to Icons.Default.LocalOffer,
        "savings" to Icons.Default.Savings
    )

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
            Text("New Ledger", style = MaterialTheme.typography.headlineMedium, color = OnSurface)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Ledger Name") },
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

            // Color picker
            Text("Color", style = MaterialTheme.typography.titleSmall, color = OnSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colorOptions.forEach { (hex, _) ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Primary)
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.background(color)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { selectedColor = hex },
                            modifier = Modifier.size(if (isSelected) 34.dp else 28.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Icon picker
            Text("Icon", style = MaterialTheme.typography.titleSmall, color = OnSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                iconOptions.forEach { (id, icon) ->
                    val isSelected = selectedIcon == id
                    val accentColor = runCatching { Color(android.graphics.Color.parseColor(selectedColor)) }.getOrDefault(Primary)
                    IconButton(
                        onClick = { selectedIcon = id },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else SurfaceContainer)
                    ) {
                        Icon(icon, null, tint = if (isSelected) accentColor else OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    onCreate(name.trim(), selectedColor, selectedIcon)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Create Ledger", color = OnPrimary) }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = OnSurface) }
        }
    }
}

/** Maps iconId string to a Material icon — add entries here as new icons are needed. */
fun ledgerIcon(iconId: String) = when (iconId) {
    "payments"      -> Icons.Default.Payments
    "business"      -> Icons.Default.Business
    "shopping_bag"  -> Icons.Default.ShoppingBag
    "home"          -> Icons.Default.Home
    "cleaning"      -> Icons.Default.CleaningServices
    "local_offer"   -> Icons.Default.LocalOffer
    "savings"       -> Icons.Default.Savings
    else            -> Icons.Default.Book
}
