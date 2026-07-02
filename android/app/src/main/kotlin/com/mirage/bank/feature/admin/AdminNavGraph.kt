package com.mirage.bank.feature.admin

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.navigation.Routes

/**
 * Pattern every other feature package follows: one NavGraphBuilder extension
 * per feature, registered once in core/navigation/MirageNavHost.kt.
 */
fun NavGraphBuilder.adminGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.ADMIN_USERS) { AdminUsersScreen() }
    composable(Routes.ADMIN_TRANSACTIONS) { AdminTransactionsScreen() }
    composable(Routes.ADMIN_TREASURY_LEDGER) { AdminTreasuryLedgerScreen() }
    composable(Routes.ADMIN_AUDIT_LOG) { AdminAuditLogScreen() }
    composable(Routes.ADMIN_SUPPORT) {
        AdminSupportScreen(
            onOpenTicket = { ticketId -> navController.navigate(Routes.adminSupportTicketDetail(ticketId)) },
        )
    }

    composable(
        route = Routes.ADMIN_SUPPORT_TICKET_DETAIL,
        arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val ticketId = backStackEntry.arguments?.getString("ticketId").orEmpty()
        AdminTicketDetailScreen(ticketId = ticketId)
    }
}
