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
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.common.util.formatIsoTimestamp
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.CashbackTxnItem

@Composable
fun CashbackScreen() {
    val container = LocalAppContainer.current
    val viewModel: CashbackViewModel = viewModel(
        factory = GenericViewModelFactory { CashbackViewModel(PremiumRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.cashback != null -> {
                val cashback = state.cashback!!
                LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    item {
                        Text("Cashback", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            formatCents(cashback.totalCashbackCents),
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            "Earning ${cashback.ratePct}% back on purchases",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        )
                        HorizontalDivider()
                        Text("History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    }
                    if (cashback.transactions.isEmpty()) {
                        item { Text("No cashback earned yet.", modifier = Modifier.padding(top = 12.dp)) }
                    }
                    items(cashback.transactions) { txn -> CashbackRow(txn) }
                }
            }
        }
    }
}

@Composable
private fun CashbackRow(txn: CashbackTxnItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(txn.description)
            Text("+${formatCents(txn.amountCents)}", color = MaterialTheme.colorScheme.primary)
        }
        Text(
            "${formatIsoTimestamp(txn.timestamp)} · ${txn.ratePct}%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
