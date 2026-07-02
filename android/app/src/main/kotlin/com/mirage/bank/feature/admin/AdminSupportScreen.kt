package com.mirage.bank.feature.admin

import androidx.compose.foundation.clickable
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
import com.mirage.bank.common.util.formatIsoTimestamp
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.AdminTicketSummary
import com.mirage.bank.feature.support.PriorityChip
import com.mirage.bank.feature.support.StatusChip

@Composable
fun AdminSupportScreen(onOpenTicket: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: AdminSupportViewModel = viewModel(
        factory = GenericViewModelFactory { AdminSupportViewModel(AdminRepository(container.apiService)) },
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
            state.tickets.isEmpty() -> Text(
                "No support tickets",
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.tickets, key = { it.id }) { ticket ->
                    AdminTicketRow(ticket, onClick = { onOpenTicket(ticket.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AdminTicketRow(ticket: AdminTicketSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(ticket.subject, style = MaterialTheme.typography.titleMedium)
        Text(
            "${ticket.userName} · ${ticket.userEmail}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(ticket.status)
            PriorityChip(ticket.priority)
        }
        Text(
            "Updated ${formatIsoTimestamp(ticket.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
