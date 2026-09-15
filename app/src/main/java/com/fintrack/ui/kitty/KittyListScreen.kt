package com.fintrack.ui.kitty

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.data.local.entity.Kitty
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KittyListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: KittyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filtered by viewModel.filteredKitties.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitty Parties", style = MaterialTheme.typography.headlineLarge, color = OnSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = { FinFab(onClick = onNavigateToAdd) },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search kitties...", color = OnSurfaceVariant) },
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
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Groups,
                    title = "No kitties yet",
                    subtitle = "Tap + to create your first kitty party",
                    modifier = Modifier.padding(top = 48.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filtered, key = { it.id }) { kitty ->
                        val memberCount = state.memberCountMap[kitty.id] ?: 0
                        KittyListCard(
                            kitty = kitty,
                            memberCount = memberCount,
                            onClick = { onNavigateToDetail(kitty.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KittyListCard(kitty: Kitty, memberCount: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        kitty.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PaidGreen
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(kitty.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Text(
                        "Starts: ${monthName(kitty.startMonth)} ${kitty.startYear}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                // Member count badge
                if (memberCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$memberCount",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PaidGreen
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
            }
        }
    }
}
