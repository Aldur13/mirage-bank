package com.mirage.bank.feature.support

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.navigation.Routes

fun NavGraphBuilder.supportGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.SUPPORT_TICKETS) {
        TicketListScreen(
            onNewTicket = { navController.navigate(Routes.SUPPORT_NEW_TICKET) },
            onOpenTicket = { ticketId -> navController.navigate(Routes.supportTicketDetail(ticketId)) },
        )
    }

    composable(Routes.SUPPORT_NEW_TICKET) {
        NewTicketScreen(
            onCreated = { ticketId ->
                navController.navigate(Routes.supportTicketDetail(ticketId)) {
                    popUpTo(Routes.SUPPORT_TICKETS)
                }
            },
        )
    }

    composable(
        route = Routes.SUPPORT_TICKET_DETAIL,
        arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val ticketId = backStackEntry.arguments?.getString("ticketId").orEmpty()
        TicketDetailScreen(ticketId = ticketId)
    }
}
