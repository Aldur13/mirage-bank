package com.mirage.bank.feature.business

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
fun NavGraphBuilder.businessGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.BUSINESS_MEMBERS) { BusinessMembersScreen() }
}
