package com.mirage.bank.feature.personal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer

@Composable
fun BalanceScreen() {
    val container = LocalAppContainer.current
    val viewModel: BalanceViewModel = viewModel(
        factory = GenericViewModelFactory { BalanceViewModel(AccountRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.balance != null -> {
                val balance = state.balance!!
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Available balance", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatCents(balance.balanceCents, balance.currency),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (balance.status == "frozen") {
                        Text(
                            "Account frozen",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
