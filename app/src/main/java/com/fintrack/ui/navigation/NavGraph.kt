package com.fintrack.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.fintrack.ui.accounts.*
import com.fintrack.ui.cards.*
import com.fintrack.ui.home.HomeScreen
import com.fintrack.ui.kitty.*
import com.fintrack.ui.ledger.LedgerDetailScreen
import com.fintrack.ui.ledger.LedgersScreen

// Route constants
object Routes {
    const val HOME = "home"
    const val KITTIES = "kitties"
    const val KITTY_DETAIL = "kitty/{kittyId}"
    const val ADD_KITTY = "add_kitty"
    const val EDIT_KITTY = "edit_kitty/{kittyId}"
    const val CARDS = "cards"
    const val CARD_DETAIL = "card/{cardId}"
    const val ADD_CARD = "add_card"
    const val EDIT_CARD = "edit_card/{cardId}"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "account/{accountId}"
    const val ADD_ACCOUNT = "add_account"
    const val EDIT_ACCOUNT = "edit_account/{accountId}"
    const val LEDGERS = "ledgers"
    const val LEDGER_DETAIL = "ledger/{ledgerId}"
}

@Composable
fun FinTrackNavGraph(navController: NavHostController, bottomPadding: PaddingValues = PaddingValues()) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = androidx.compose.ui.Modifier.padding(bottom = bottomPadding.calculateBottomPadding())
    ) {

        // ── Home ──
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToKitties = { navController.navigate(Routes.KITTIES) },
                onNavigateToCards = { navController.navigate(Routes.CARDS) },
                onNavigateToAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onNavigateToLedgers = { navController.navigate(Routes.LEDGERS) }
            )
        }

        // ── Kitty ──
        composable(Routes.KITTIES) {
            KittyListScreen(
                onNavigateToDetail = { id -> navController.navigate("kitty/$id") },
                onNavigateToAdd = { navController.navigate(Routes.ADD_KITTY) }
            )
        }
        composable(
            Routes.KITTY_DETAIL,
            arguments = listOf(navArgument("kittyId") { type = NavType.LongType })
        ) { back ->
            val kittyId = back.arguments!!.getLong("kittyId")
            KittyDetailScreen(
                kittyId = kittyId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("edit_kitty/$id") }
            )
        }
        composable(Routes.ADD_KITTY) {
            AddEditKittyScreen(
                kittyId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.EDIT_KITTY,
            arguments = listOf(navArgument("kittyId") { type = NavType.LongType })
        ) { back ->
            val kittyId = back.arguments!!.getLong("kittyId")
            AddEditKittyScreen(
                kittyId = kittyId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Credit Cards ──
        composable(Routes.CARDS) {
            CreditCardsScreen(
                onNavigateToDetail = { id -> navController.navigate("card/$id") },
                onNavigateToAdd = { navController.navigate(Routes.ADD_CARD) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.CARD_DETAIL,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { back ->
            val cardId = back.arguments!!.getLong("cardId")
            CreditCardDetailScreen(
                cardId = cardId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("edit_card/$id") }
            )
        }
        composable(Routes.ADD_CARD) {
            AddEditCardScreen(
                cardId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.EDIT_CARD,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { back ->
            val cardId = back.arguments!!.getLong("cardId")
            AddEditCardScreen(
                cardId = cardId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Bank Accounts ──
        composable(Routes.ACCOUNTS) {
            BankAccountsScreen(
                onNavigateToDetail = { id -> navController.navigate("account/$id") },
                onNavigateToAdd = { navController.navigate(Routes.ADD_ACCOUNT) }
            )
        }
        composable(
            Routes.ACCOUNT_DETAIL,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { back ->
            val accountId = back.arguments!!.getLong("accountId")
            BankAccountDetailScreen(
                accountId = accountId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("edit_account/$id") }
            )
        }
        composable(Routes.ADD_ACCOUNT) {
            AddEditBankAccountScreen(
                accountId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.EDIT_ACCOUNT,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { back ->
            val accountId = back.arguments!!.getLong("accountId")
            AddEditBankAccountScreen(
                accountId = accountId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Ledgers ──
        composable(Routes.LEDGERS) {
            LedgersScreen(
                onNavigateToDetail = { id -> navController.navigate("ledger/$id") }
            )
        }
        composable(
            Routes.LEDGER_DETAIL,
            arguments = listOf(navArgument("ledgerId") { type = NavType.LongType })
        ) { back ->
            val ledgerId = back.arguments!!.getLong("ledgerId")
            LedgerDetailScreen(
                ledgerId = ledgerId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
