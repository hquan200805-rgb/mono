package com.chitieu.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chitieu.app.feature.budget.BudgetScreen
import com.chitieu.app.feature.insight.InsightScreen
import com.chitieu.app.feature.overview.OverviewScreen
import com.chitieu.app.feature.pending.PendingScreen
import com.chitieu.app.feature.report.ReportScreen
import com.chitieu.app.feature.settings.SettingsScreen
import com.chitieu.app.feature.transaction.AddTransactionScreen
import com.chitieu.app.feature.transaction.TransactionScreen
import com.chitieu.app.feature.transfer.TransferScreen

@Composable
fun ChiTieuNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Overview.route,
        modifier = modifier
    ) {
        composable(Screen.Overview.route) {
            OverviewScreen(
                onTransferClick = { navController.navigate(Screen.Transfer.route) },
                onPendingClick = { navController.navigate(Screen.Pending.route) },
                onInsightClick = { navController.navigate(Screen.Insight.route) }
            )
        }
        composable(Screen.Transaction.route) {
            TransactionScreen(
                onAddClick = { navController.navigate(Screen.AddTransaction.route) },
                onEditClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                onTransferClick = { navController.navigate(Screen.Transfer.route) }
            )
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) {
            AddTransactionScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(Screen.Transfer.route) {
            TransferScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(Screen.Pending.route) {
            PendingScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Insight.route) {
            InsightScreen()
        }
        composable(Screen.Budget.route) { BudgetScreen() }
        composable(Screen.Report.route) { ReportScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
