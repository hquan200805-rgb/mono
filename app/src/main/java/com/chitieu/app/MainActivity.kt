package com.chitieu.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chitieu.app.core.common.SeedData
import com.chitieu.app.core.designsystem.theme.ChiTieuTheme
import com.chitieu.app.core.lock.AppLockRepository
import com.chitieu.app.feature.settings.LockScreen
import com.chitieu.app.navigation.ChiTieuBottomBar
import com.chitieu.app.navigation.ChiTieuNavHost
import com.chitieu.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var seedData: SeedData
    @Inject lateinit var appLockRepository: AppLockRepository

    companion object {
        const val ACTION_ADD_TRANSACTION = "com.chitieu.app.ACTION_ADD_TRANSACTION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChiTieuTheme {
                val lockEnabled by appLockRepository.lockEnabled.collectAsState(initial = false)
                var unlocked by remember { mutableStateOf(false) }

                // Khi tắt khóa → coi như đã mở
                LaunchedEffect(lockEnabled) {
                    if (!lockEnabled) unlocked = true
                    else unlocked = false
                }

                if (lockEnabled && !unlocked) {
                    LockScreen(
                        appLockRepository = appLockRepository,
                        onUnlocked = { unlocked = true }
                    )
                    return@ChiTieuTheme
                }

                val navController = rememberNavController()
                val navBackStackEntry = navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry.value?.destination?.route
                val showBottomBar = currentRoute in Screen.bottomBarItems.map { it.route }

                var pendingAdd by remember {
                    mutableStateOf(intent?.action == ACTION_ADD_TRANSACTION)
                }

                LaunchedEffect(Unit) {
                    seedData.seedIfNeeded()
                }

                LaunchedEffect(pendingAdd) {
                    if (pendingAdd) {
                        navController.navigate(Screen.AddTransaction.route) {
                            launchSingleTop = true
                        }
                        pendingAdd = false
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            ChiTieuBottomBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    ChiTieuNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
