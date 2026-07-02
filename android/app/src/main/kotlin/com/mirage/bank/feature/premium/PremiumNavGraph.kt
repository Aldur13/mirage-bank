package com.mirage.bank.feature.premium

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
fun NavGraphBuilder.premiumGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.PREMIUM_UPSELL) {
        PremiumUpsellScreen(onPurchased = { navController.popBackStack() })
    }
    composable(Routes.PREMIUM_ANALYTICS) { SpendingAnalyticsScreen() }
    composable(Routes.PREMIUM_SAVINGS_GOALS) { SavingsGoalsScreen() }
    composable(Routes.PREMIUM_VIRTUAL_CARDS) { VirtualCardsScreen() }
    composable(Routes.PREMIUM_CASHBACK) { CashbackScreen() }
    composable(Routes.PREMIUM_ADVANCED_SECURITY) { AdvancedSecurityScreen() }
}
