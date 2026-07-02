package com.mirage.bank.feature.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.LedgerResponse
import com.mirage.bank.core.network.dto.TreasuryResponse

@Composable
fun AdminTreasuryLedgerScreen() {
    val container = LocalAppContainer.current
    val viewModel: AdminTreasuryLedgerViewModel = viewModel(
        factory = GenericViewModelFactory { AdminTreasuryLedgerViewModel(AdminRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error != null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            state.treasury != null && state.ledger != null -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                TreasuryCard(state.treasury!!)
                LedgerCard(state.ledger!!, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun TreasuryCard(treasury: TreasuryResponse, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Treasury", style = MaterialTheme.typography.titleLarge)
            Text(
                "Balance: ${formatCents(treasury.balanceCents, treasury.currency)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Issued: ${formatCents(treasury.issuedCents, treasury.currency)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun LedgerCard(ledger: LedgerResponse, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Ledger", style = MaterialTheme.typography.titleLarge)
            Text(
                if (ledger.balanced) "Balanced" else "UNBALANCED",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (ledger.balanced) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "User balances total: ${formatCents(ledger.userBalanceCents)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Treasury balance: ${formatCents(ledger.treasuryBalanceCents)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Grand total: ${formatCents(ledger.totalCents)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Accounts: ${ledger.accountCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
