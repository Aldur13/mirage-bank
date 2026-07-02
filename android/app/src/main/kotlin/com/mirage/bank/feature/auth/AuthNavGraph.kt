package com.mirage.bank.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.navigation.Routes

fun NavGraphBuilder.authGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.LOGIN) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            },
            onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
        )
    }

    composable(Routes.REGISTER) {
        RegisterScreen(onRegistered = { navController.popBackStack() })
    }
}

/** Call from anywhere (e.g. on session-expired) to send the user back to a clean Login screen. */
fun NavController.navigateToLoginClearingStack() {
    navigate(Routes.LOGIN) {
        popUpTo(0) { inclusive = true }
    }
}
