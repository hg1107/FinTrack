package com.fintrack.ui.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.CreditCard
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards = remember(state.cards, searchQuery) {
        if (searchQuery.isBlank()) state.cards
        else state.cards.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credit Cards", style = MaterialTheme.typography.headlineLarge, color = OnSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = { FinFab(onClick = onNavigateToAdd) },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total outstanding header
            item {
                Spacer(Modifier.height(4.dp))
                Text("Total Outstanding", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    formatAmount(state.totalOutstanding),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = DebitRed
                )
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search cards...", color = OnSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface
                    )
                )
            }

            if (filteredCards.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.CreditCard,
                        title = if (searchQuery.isBlank()) "No cards yet" else "No cards found",
                        subtitle = if (searchQuery.isBlank()) "Tap + to add your first credit card" else "Try a different search term",
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(filteredCards, key = { it.id }) { card ->
                    val outstanding = state.cardOutstandingMap[card.id] ?: 0.0
                    CreditCardListItem(
                        card = card,
                        outstanding = outstanding,
                        onClick = { onNavigateToDetail(card.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditCardListItem(card: CreditCard, outstanding: Double, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    if (card.lastFourDigits.isNotBlank()) {
                        Text("**** ${card.lastFourDigits}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    if (card.billingDueDay > 0) {
                        Text("Due: ${card.billingDueDay.ordinal()} of month",
                            style = MaterialTheme.typography.bodySmall, color = Amber)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (outstanding > 0) {
                        Text(
                            formatAmount(outstanding),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DebitRed
                        )
                        if (card.creditLimit > 0) {
                            val pct = ((outstanding / card.creditLimit) * 100).toInt()
                            Text("${pct}% used", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
            }
            // Mini utilization bar for cards with a limit set
            if (card.creditLimit > 0 && outstanding > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (outstanding / card.creditLimit).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (outstanding / card.creditLimit > 0.8) Error else Amber,
                    trackColor = SurfaceContainer
                )
            }
        }
    }
}

private fun Int.ordinal(): String {
    val suffix = when {
        this in 11..13 -> "th"
        this % 10 == 1 -> "st"
        this % 10 == 2 -> "nd"
        this % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$this$suffix"
}
