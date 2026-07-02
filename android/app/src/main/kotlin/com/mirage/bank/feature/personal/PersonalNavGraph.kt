package com.mirage.bank.feature.personal

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.navigation.Routes
import com.mirage.bank.feature.profile.ChangePasswordScreen
import com.mirage.bank.feature.profile.ProfileScreen

/**
 * Pattern every other feature package follows: one NavGraphBuilder extension
 * per feature, registered once in core/navigation/MirageNavHost.kt. Adding a
 * new feature package never requires editing another feature's file.
 */
fun NavGraphBuilder.personalGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.BALANCE) { BalanceScreen() }
    composable(Routes.TRANSFER) { TransferScreen(onDone = { navController.popBackStack() }) }
    composable(Routes.WITHDRAW) { WithdrawScreen(onDone = { navController.popBackStack() }) }
    composable(Routes.TRANSACTIONS) { TransactionsScreen() }
    composable(Routes.PROFILE) {
        ProfileScreen(onNavigateToChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) })
    }
    composable(Routes.CHANGE_PASSWORD) {
        ChangePasswordScreen(onDone = { navController.popBackStack() })
    }
}
