package com.mirage.bank.feature.support

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.mirage.bank.core.network.dto.TicketSummary

@Composable
fun TicketListScreen(onNewTicket: () -> Unit, onOpenTicket: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: TicketListViewModel = viewModel(
        factory = GenericViewModelFactory { TicketListViewModel(SupportRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTicket) {
                Icon(Icons.Filled.Add, contentDescription = "New ticket")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                state.tickets.isEmpty() -> Text(
                    "No support tickets yet",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.tickets, key = { it.id }) { ticket ->
                        TicketRow(ticket, onClick = { onOpenTicket(ticket.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketRow(ticket: TicketSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(ticket.subject, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(ticket.status)
            PriorityChip(ticket.priority)
            Text(
                "${ticket.messageCount} messages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Updated ${formatIsoTimestamp(ticket.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "open" -> MaterialTheme.colorScheme.primary
        "in_progress" -> MaterialTheme.colorScheme.tertiary
        "waiting" -> MaterialTheme.colorScheme.secondary
        "resolved" -> MaterialTheme.colorScheme.outline
        "closed" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(status.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun PriorityChip(priority: String) {
    val color = when (priority) {
        "urgent" -> MaterialTheme.colorScheme.error
        "high" -> MaterialTheme.colorScheme.error
        "normal" -> MaterialTheme.colorScheme.onSurfaceVariant
        "low" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(priority, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
