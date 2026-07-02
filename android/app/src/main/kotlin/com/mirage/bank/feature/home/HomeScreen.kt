package com.mirage.bank.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.components.UpdateBanner
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.core.di.LocalAppContainer

private data class NavTile(val title: String, val route: String)

@Composable
fun HomeScreen(onNavigate: (route: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = GenericViewModelFactory {
            HomeViewModel(container.apiService, container.sessionManager, container.updateRepository)
        },
    )
    val state by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val me = state.me
    if (me == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.sessionExpiredOrError ?: "Something went wrong")
        }
        return
    }

    val tiles = buildList {
        add(NavTile("Balance", com.mirage.bank.core.navigation.Routes.BALANCE))
        add(NavTile("Transfer money", com.mirage.bank.core.navigation.Routes.TRANSFER))
        add(NavTile("Withdraw", com.mirage.bank.core.navigation.Routes.WITHDRAW))
        add(NavTile("Transactions", com.mirage.bank.core.navigation.Routes.TRANSACTIONS))
        add(NavTile("Profile", com.mirage.bank.core.navigation.Routes.PROFILE))
        if (state.isGuardian) {
            add(NavTile("Manage ward account", com.mirage.bank.core.navigation.Routes.GUARDIAN_DASHBOARD))
        }
        if (state.isBusinessOwner && me.accountType == "business") {
            add(NavTile("Team members", com.mirage.bank.core.navigation.Routes.BUSINESS_MEMBERS))
        }
        add(NavTile("Support", com.mirage.bank.core.navigation.Routes.SUPPORT_TICKETS))
        if (me.isPremium) {
            add(NavTile("Spending analytics", com.mirage.bank.core.navigation.Routes.PREMIUM_ANALYTICS))
            add(NavTile("Savings goals", com.mirage.bank.core.navigation.Routes.PREMIUM_SAVINGS_GOALS))
            add(NavTile("Virtual cards", com.mirage.bank.core.navigation.Routes.PREMIUM_VIRTUAL_CARDS))
            add(NavTile("Cashback", com.mirage.bank.core.navigation.Routes.PREMIUM_CASHBACK))
            add(NavTile("Advanced security", com.mirage.bank.core.navigation.Routes.PREMIUM_ADVANCED_SECURITY))
        } else {
            add(NavTile("Go Premium", com.mirage.bank.core.navigation.Routes.PREMIUM_UPSELL))
        }
        if (me.role == "admin") {
            add(NavTile("Admin: Users", com.mirage.bank.core.navigation.Routes.ADMIN_USERS))
            add(NavTile("Admin: Transactions", com.mirage.bank.core.navigation.Routes.ADMIN_TRANSACTIONS))
            add(NavTile("Admin: Treasury & Ledger", com.mirage.bank.core.navigation.Routes.ADMIN_TREASURY_LEDGER))
            add(NavTile("Admin: Audit log", com.mirage.bank.core.navigation.Routes.ADMIN_AUDIT_LOG))
            add(NavTile("Admin: Support tickets", com.mirage.bank.core.navigation.Routes.ADMIN_SUPPORT))
        }
        add(NavTile("Settings", com.mirage.bank.core.navigation.Routes.SETTINGS))
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Hi, ${me.name}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(24.dp),
        )
        if (me.status == "frozen") {
            Card(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Your account is frozen. Transfers and withdrawals are disabled.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        UpdateBanner(
            state = updateState,
            onDownload = { container.updateRepository.startDownload(it) },
            onInstall = { container.updateRepository.promptInstall() },
            onDismiss = { container.updateRepository.dismiss() },
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tiles) { tile ->
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        androidx.compose.material3.TextButton(onClick = { onNavigate(tile.route) }) {
                            Text(tile.title)
                        }
                    }
                }
            }
        }
    }
}
