package com.fintrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fintrack.ui.navigation.FinTrackBottomNavBar
import com.fintrack.ui.navigation.FinTrackNavGraph
import com.fintrack.ui.navigation.Routes
import com.fintrack.ui.theme.Background
import com.fintrack.ui.theme.FinTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            FinTrackTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val topLevelRoutes = remember {
                    setOf(Routes.HOME, Routes.KITTIES, Routes.CARDS, Routes.ACCOUNTS, Routes.LEDGERS)
                }
                val showBottomBar = currentRoute in topLevelRoutes

                Scaffold(
                    containerColor = Background,
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomBar) {
                            FinTrackBottomNavBar(navController)
                        }
                    }
                ) { innerPadding ->
                    FinTrackNavGraph(
                        navController = navController,
                        bottomPadding = if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues()
                    )
                }
            }
        }
    }
}

