package com.mirage.bank.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.AdminUserItem

@Composable
fun AdminUsersScreen() {
    val container = LocalAppContainer.current
    val viewModel: AdminUsersViewModel = viewModel(
        factory = GenericViewModelFactory { AdminUsersViewModel(AdminRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()
    val currentUserId = container.sessionManager.currentUser.collectAsState().value?.id
    var creditTarget by remember { mutableStateOf<AdminUserItem?>(null) }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.filter,
            onValueChange = viewModel::onFilterChange,
            label = { Text("Search by name or email") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.filteredUsers.isEmpty() -> Text(
                    "No users found",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.filteredUsers, key = { it.id }) { user ->
                        AdminUserRow(
                            user = user,
                            isCurrentUser = user.id == currentUserId,
                            actionInProgress = state.actionInProgressUserId == user.id,
                            onFreeze = { viewModel.freeze(user.id) },
                            onUnfreeze = { viewModel.unfreeze(user.id) },
                            onDisable = { viewModel.disable(user.id) },
                            onCredit = { creditTarget = user },
                        )
                    }
                }
            }
        }
    }

    creditTarget?.let { user ->
        CreditDialog(
            user = user,
            onDismiss = { creditTarget = null },
            onConfirm = { amountCents, description ->
                viewModel.credit(user.id, amountCents, description)
                creditTarget = null
            },
        )
    }
}

@Composable
private fun AdminUserRow(
    user: AdminUserItem,
    isCurrentUser: Boolean,
    actionInProgress: Boolean,
    onFreeze: () -> Unit,
    onUnfreeze: () -> Unit,
    onDisable: () -> Unit,
    onCredit: () -> Unit,
) {
    val isAdmin = user.role == "admin"
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (isCurrentUser) "${user.name} (you)" else user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${user.status} · ${user.accountType} · ${formatCents(user.balanceCents, user.currency)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (user.status == "frozen") {
                    OutlinedButton(onClick = onUnfreeze, enabled = !isAdmin && !actionInProgress) {
                        Text("Unfreeze")
                    }
                } else {
                    OutlinedButton(onClick = onFreeze, enabled = !isAdmin && !actionInProgress) {
                        Text("Freeze")
                    }
                }
                OutlinedButton(onClick = onDisable, enabled = !isAdmin && !actionInProgress) {
                    Text("Disable")
                }
                Button(onClick = onCredit, enabled = !actionInProgress) {
                    Text("Credit")
                }
            }
        }
    }
}

@Composable
private fun CreditDialog(
    user: AdminUserItem,
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, description: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val amountCents = amountText.toDoubleOrNull()?.let { (it * 100).toLong() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Credit ${user.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { amountCents?.let { onConfirm(it, description.ifBlank { null }) } },
                enabled = amountCents != null && amountCents > 0,
            ) {
                Text("Credit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
