package com.mirage.bank.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.navigation.Routes
import com.mirage.bank.feature.auth.navigateToLoginClearingStack

fun NavGraphBuilder.settingsGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.SETTINGS) {
        SettingsScreen(onLoggedOut = { navController.navigateToLoginClearingStack() })
    }
}
