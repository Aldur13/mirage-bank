package com.mirage.bank.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatIsoTimestamp
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.TicketMessageItem
import com.mirage.bank.feature.support.PriorityChip
import com.mirage.bank.feature.support.StatusChip

private val TICKET_STATUSES = listOf("open", "in_progress", "waiting", "resolved", "closed")
private val TICKET_PRIORITIES = listOf("low", "normal", "high", "urgent")

@Composable
fun AdminTicketDetailScreen(ticketId: String) {
    val container = LocalAppContainer.current
    val viewModel: AdminTicketDetailViewModel = viewModel(
        factory = GenericViewModelFactory { AdminTicketDetailViewModel(AdminRepository(container.apiService), ticketId) },
    )
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error != null && state.ticket == null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            state.ticket != null -> {
                val ticket = state.ticket!!
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(ticket.subject, style = MaterialTheme.typography.headlineSmall)
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatusChip(ticket.status)
                            PriorityChip(ticket.priority)
                        }
                        Text(
                            "Created ${formatIsoTimestamp(ticket.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PickerButton(
                                label = "Status",
                                current = ticket.status,
                                options = TICKET_STATUSES,
                                enabled = !state.isUpdating,
                                onSelect = viewModel::updateStatus,
                                modifier = Modifier.weight(1f),
                            )
                            PickerButton(
                                label = "Priority",
                                current = ticket.priority,
                                options = TICKET_PRIORITIES,
                                enabled = !state.isUpdating,
                                onSelect = viewModel::updatePriority,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    HorizontalDivider()

                    LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                        items(ticket.messages, key = { it.id }) { message ->
                            MessageBubble(message)
                        }
                    }

                    HorizontalDivider()

                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        OutlinedTextField(
                            value = state.replyText,
                            onValueChange = viewModel::onReplyTextChange,
                            label = { Text("Reply") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Button(
                                onClick = viewModel::sendReply,
                                enabled = !state.isSending && state.replyText.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(if (state.isSending) "Sending..." else "Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerButton(
    label: String,
    current: String,
    options: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${current.replace('_', ' ')}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace('_', ' ')) },
                    onClick = {
                        expanded = false
                        if (option != current) onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: TicketMessageItem) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message.authorName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (message.isStaff) {
                    Text(
                        "STAFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }
            Text(
                formatIsoTimestamp(message.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(message.content, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
