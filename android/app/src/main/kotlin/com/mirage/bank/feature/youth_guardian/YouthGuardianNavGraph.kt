package com.mirage.bank.feature.youth_guardian

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.navigation.Routes

/**
 * Pattern every other feature package follows: one NavGraphBuilder extension
 * per feature, registered once in core/navigation/MirageNavHost.kt. Adding a
 * new feature package never requires editing another feature's file.
 */
fun NavGraphBuilder.youthGuardianGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.GUARDIAN_DASHBOARD) {
        GuardianDashboardScreen(
            onViewTransactions = { navController.navigate(Routes.GUARDIAN_WARD_TRANSACTIONS) },
        )
    }
    composable(Routes.GUARDIAN_WARD_TRANSACTIONS) { GuardianWardTransactionsScreen() }
}
