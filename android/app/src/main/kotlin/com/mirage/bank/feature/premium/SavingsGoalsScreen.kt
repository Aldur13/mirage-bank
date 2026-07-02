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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.SavingsGoalItem

@Composable
fun SavingsGoalsScreen() {
    val container = LocalAppContainer.current
    val viewModel: SavingsGoalsViewModel = viewModel(
        factory = GenericViewModelFactory { SavingsGoalsViewModel(PremiumRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "New goal")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null && state.goals.isEmpty() ->
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                else -> {
                    if (state.goals.isEmpty()) {
                        Text("No savings goals yet. Tap + to create one.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            item { Text("Savings goals", style = MaterialTheme.typography.headlineSmall) }
                            items(state.goals, key = { it.id }) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onContribute = { amount -> viewModel.contribute(goal.id, amount) },
                                    onSetAmount = { amount -> viewModel.setCurrentAmount(goal.id, amount) },
                                    onDelete = { viewModel.deleteGoal(goal.id) },
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
        CreateGoalDialog(
            isSubmitting = state.isSubmitting,
            onDismiss = viewModel::dismissCreateDialog,
            onCreate = { name, targetCents, deadline -> viewModel.createGoal(name, targetCents, deadline) },
        )
    }
}

@Composable
private fun GoalCard(
    goal: SavingsGoalItem,
    onContribute: (Long) -> Unit,
    onSetAmount: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    var showContributeDialog by remember { mutableStateOf(false) }
    var showSetAmountDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                Text("${goal.progressPct.toInt()}%")
            }
            goal.deadline?.let {
                Text("Due $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
            LinearProgressIndicator(
                progress = (goal.progressPct / 100.0).toFloat().coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                "${formatCents(goal.currentAmountCents)} of ${formatCents(goal.targetAmountCents)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                TextButton(onClick = { showContributeDialog = true }) { Text("Contribute") }
                TextButton(onClick = { showSetAmountDialog = true }) { Text("Set amount") }
                TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete") }
            }
        }
    }

    if (showContributeDialog) {
        AmountInputDialog(
            title = "Contribute to ${goal.name}",
            label = "Amount to add (EUR)",
            onDismiss = { showContributeDialog = false },
            onConfirm = { cents ->
                onContribute(cents)
                showContributeDialog = false
            },
        )
    }

    if (showSetAmountDialog) {
        AmountInputDialog(
            title = "Set current amount",
            label = "Current amount (EUR)",
            onDismiss = { showSetAmountDialog = false },
            onConfirm = { cents ->
                onSetAmount(cents)
                showSetAmountDialog = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete goal?") },
            text = { Text("This will permanently remove \"${goal.name}\".") },
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
private fun AmountInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var amountInput by remember { mutableStateOf("") }
    val cents = amountInput.toDoubleOrNull()?.let { (it * 100).toLong() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = cents != null && cents > 0, onClick = { cents?.let(onConfirm) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CreateGoalDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, targetCents: Long, deadline: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    val targetCents = targetInput.toDoubleOrNull()?.let { (it * 100).toLong() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings goal") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target amount (EUR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (optional, e.g. 2026-12-31)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && name.isNotBlank() && targetCents != null && targetCents > 0,
                onClick = { targetCents?.let { onCreate(name, it, deadline.ifBlank { null }) } },
            ) {
                Text(if (isSubmitting) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
