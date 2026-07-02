package com.mirage.bank.feature.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.mirage.bank.core.network.dto.AdminTransactionItem

@Composable
fun AdminTransactionsScreen() {
    val container = LocalAppContainer.current
    val viewModel: AdminTransactionsViewModel = viewModel(
        factory = GenericViewModelFactory { AdminTransactionsViewModel(AdminRepository(container.apiService)) },
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
            state.transactions.isEmpty() -> Text(
                "No transactions yet",
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.transactions, key = { it.id }) { transaction ->
                    AdminTransactionRow(transaction)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AdminTransactionRow(transaction: AdminTransactionItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("${transaction.fromName} -> ${transaction.toName}", style = MaterialTheme.typography.titleMedium)
        Text(
            "${transaction.type} · ${formatCents(transaction.amountCents)} · ${transaction.status}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (transaction.description.isNotBlank()) {
            Text(
                transaction.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            formatIsoTimestamp(transaction.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
