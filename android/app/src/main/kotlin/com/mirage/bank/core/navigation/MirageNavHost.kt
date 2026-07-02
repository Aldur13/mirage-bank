package com.mirage.bank.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mirage.bank.core.di.AppContainer
import com.mirage.bank.core.session.AuthEventBus
import com.mirage.bank.feature.admin.adminGraph
import com.mirage.bank.feature.auth.authGraph
import com.mirage.bank.feature.auth.navigateToLoginClearingStack
import com.mirage.bank.feature.business.businessGraph
import com.mirage.bank.feature.home.homeGraph
import com.mirage.bank.feature.personal.personalGraph
import com.mirage.bank.feature.premium.premiumGraph
import com.mirage.bank.feature.settings.settingsGraph
import com.mirage.bank.feature.support.supportGraph
import com.mirage.bank.feature.youth_guardian.youthGuardianGraph

/**
 * Every feature package exposes exactly one `fun NavGraphBuilder.xGraph(nav, container)`
 * extension (see feature/personal/PersonalNavGraph.kt for the pattern). This
 * file only wires them together so multiple features can be worked on
 * without touching this shared file's body beyond one registration line.
 */
@Composable
fun MirageNavHost(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (container.sessionManager.hasValidLocalToken()) Routes.HOME else Routes.LOGIN

    LaunchedEffect(Unit) {
        AuthEventBus.sessionExpired.collect {
            navController.navigateToLoginClearingStack()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        authGraph(navController, container)
        homeGraph(navController, container)
        personalGraph(navController, container)
        youthGuardianGraph(navController, container)
        businessGraph(navController, container)
        supportGraph(navController, container)
        premiumGraph(navController, container)
        adminGraph(navController, container)
        settingsGraph(navController, container)
    }
}
