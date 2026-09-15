package com.fintrack.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fintrack.ui.theme.*

sealed class BottomNavDestination(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavDestination("home", "Home", Icons.Default.Home)
    object Kitties : BottomNavDestination("kitties", "Kitties", Icons.Default.Groups)
    object Cards : BottomNavDestination("cards", "Cards", Icons.Default.CreditCard)
    object Accounts : BottomNavDestination("accounts", "Accounts", Icons.Default.AccountBalance)
    object Ledgers : BottomNavDestination("ledgers", "Ledgers", Icons.Default.Bookmarks)
}

val bottomNavItems = listOf(
    BottomNavDestination.Home,
    BottomNavDestination.Kitties,
    BottomNavDestination.Cards,
    BottomNavDestination.Accounts,
    BottomNavDestination.Ledgers
)

@Composable
fun FinTrackBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(BottomNavDestination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(dest.icon, contentDescription = dest.label)
                },
                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OnPrimary,
                    selectedTextColor = Primary,
                    indicatorColor = Primary,
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant
                )
            )
        }
    }
}
