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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.VirtualCardItem

@Composable
fun VirtualCardsScreen() {
    val container = LocalAppContainer.current
    val viewModel: VirtualCardsViewModel = viewModel(
        factory = GenericViewModelFactory { VirtualCardsViewModel(PremiumRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (state.cards.size < 5) {
                FloatingActionButton(onClick = viewModel::showCreateDialog) {
                    Icon(Icons.Filled.Add, contentDescription = "New card")
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null && state.cards.isEmpty() ->
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                else -> {
                    if (state.cards.isEmpty()) {
                        Text("No virtual cards yet. Tap + to create one.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            item {
                                Text("Virtual cards", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "${state.cards.size}/5 cards",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            items(state.cards, key = { it.id }) { card ->
                                CardRow(
                                    card = card,
                                    onToggleFrozen = { viewModel.toggleFrozen(card) },
                                    onDelete = { viewModel.deleteCard(card.id) },
                                )
                            }
                            state.error?.let { err ->
                                item {
                                    Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateCardDialog(
            isSubmitting = state.isSubmitting,
            onDismiss = viewModel::dismissCreateDialog,
            onCreate = { label, limitCents -> viewModel.createCard(label, limitCents) },
        )
    }

    state.revealCard?.let { card ->
        RevealCardDialog(card = card, onDismiss = viewModel::dismissReveal)
    }
}

@Composable
private fun CardRow(card: VirtualCardItem, onToggleFrozen: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(card.label, style = MaterialTheme.typography.titleMedium)
                if (card.frozen) {
                    Text("Frozen", color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                card.cardNumber,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text("Expires ${card.expiry}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            card.spendingLimitCents?.let {
                Text("Limit: ${formatCents(it)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (card.frozen) "Unfreeze" else "Freeze")
                    Switch(checked = !card.frozen, onCheckedChange = { onToggleFrozen() })
                }
                TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete card?") },
            text = { Text("This will permanently remove \"${card.label}\".") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CreateCardDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onCreate: (label: String?, limitCents: Long?) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var limitInput by remember { mutableStateOf("") }
    val limitCents = limitInput.toDoubleOrNull()?.let { (it * 100).toLong() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New virtual card") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Spending limit (optional, EUR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = { onCreate(label.ifBlank { null }, limitCents) },
            ) {
                Text(if (isSubmitting) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RevealCardDialog(card: VirtualCardItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card created") },
        text = {
            Column {
                Text(
                    "Save these details now -- you won't be able to see the full number or CVV again.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    card.cardNumberFull ?: card.cardNumber,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text("Expiry: ${card.expiry}", modifier = Modifier.padding(top = 4.dp))
                card.cvv?.let { Text("CVV: $it", modifier = Modifier.padding(top = 4.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
