package com.mirage.bank.feature.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatIsoTimestamp
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.SessionInfo

@Composable
fun AdvancedSecurityScreen() {
    val container = LocalAppContainer.current
    val viewModel: AdvancedSecurityViewModel = viewModel(
        factory = GenericViewModelFactory { AdvancedSecurityViewModel(PremiumRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.security != null -> {
                val security = state.security!!
                LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    item {
                        Text("Advanced security", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Preview feature -- session data shown here is illustrative.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                        )
                        Text(
                            "Two-factor authentication: ${if (security.twoFaEnabled) "Enabled" else "Disabled"} (${security.method})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Active sessions", style = MaterialTheme.typography.titleMedium)
                    }
                    items(security.sessions) { session -> SessionCard(session) }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Trusted devices", style = MaterialTheme.typography.titleMedium)
                        if (security.trustedDevices.isEmpty()) {
                            Text("No trusted devices.", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    items(security.trustedDevices) { device ->
                        Text(device, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionInfo) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(session.device, style = MaterialTheme.typography.titleSmall)
                if (session.current) {
                    Text("This device", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(session.location, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Text(session.ipAddress, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            Text(
                "Last active ${formatIsoTimestamp(session.lastActive)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
