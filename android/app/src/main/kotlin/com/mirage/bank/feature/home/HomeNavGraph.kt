package com.mirage.bank.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.navigation.Routes

fun NavGraphBuilder.homeGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.HOME) {
        HomeScreen(onNavigate = { route -> navController.navigate(route) })
    }
}
