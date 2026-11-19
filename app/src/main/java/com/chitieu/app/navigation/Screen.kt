package com.chitieu.app.navigation

sealed class Screen(val route: String, val title: String) {
    data object Overview : Screen("overview", "Tổng quan")
    data object Transaction : Screen("transaction", "Giao dịch")
    data object Budget : Screen("budget", "Ngân sách")
    data object Report : Screen("report", "Báo cáo")
    data object Settings : Screen("settings", "Cài đặt")

    data object AddTransaction : Screen("transaction/add", "Thêm giao dịch")
    data object EditTransaction : Screen("transaction/edit/{transactionId}", "Sửa giao dịch") {
        fun createRoute(transactionId: Long) = "transaction/edit/$transactionId"
    }
    data object Transfer : Screen("transfer", "Chuyển khoản")
    data object Pending : Screen("pending", "Chờ xác nhận")
    data object Insight : Screen("insight", "Gợi ý")

    companion object {
        val bottomBarItems = listOf(Overview, Transaction, Budget, Report, Settings)
    }
}
