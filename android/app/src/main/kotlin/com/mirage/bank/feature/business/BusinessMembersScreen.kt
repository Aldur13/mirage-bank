package com.mirage.bank.feature.business

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.mirage.bank.core.network.dto.BusinessMemberItem

@Composable
fun BusinessMembersScreen() {
    val container = LocalAppContainer.current
    val viewModel: BusinessMembersViewModel = viewModel(
        factory = GenericViewModelFactory { BusinessMembersViewModel(BusinessRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    var showInviteDialog by remember { mutableStateOf(false) }
    var memberPendingRemoval by remember { mutableStateOf<BusinessMemberItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showInviteDialog = true }) {
                Text("+")
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
                state.data != null -> {
                    val data = state.data!!
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(data.companyName, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Team members",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        LazyColumn {
                            items(data.members) { member ->
                                MemberRow(
                                    member = member,
                                    onRemoveClick = { memberPendingRemoval = member },
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        InviteMemberDialog(
            isInviting = state.isInviting,
            error = state.inviteError,
            onDismiss = {
                showInviteDialog = false
                viewModel.clearInviteError()
            },
            onInvite = { email, role -> viewModel.invite(email, role) },
        )
    }

    memberPendingRemoval?.let { member ->
        RemoveMemberDialog(
            member = member,
            isRemoving = state.isRemoving,
            error = state.removeError,
            onDismiss = {
                memberPendingRemoval = null
                viewModel.clearRemoveError()
            },
            onConfirm = { viewModel.removeMember(member.userId) },
        )
    }
}

@Composable
private fun MemberRow(member: BusinessMemberItem, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(member.email, style = MaterialTheme.typography.bodySmall)
            Text(
                "${member.role} - joined ${formatIsoTimestamp(member.joinedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onRemoveClick, enabled = member.role != "owner") {
            Text("Remove")
        }
    }
}

@Composable
private fun InviteMemberDialog(
    isInviting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onInvite: (email: String, role: String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("employee") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite team member") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Role",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == "manager",
                        onClick = { role = "manager" },
                        label = { Text("Manager") },
                    )
                    FilterChip(
                        selected = role == "employee",
                        onClick = { role = "employee" },
                        label = { Text("Employee") },
                    )
                }
                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(email, role) },
                enabled = !isInviting && email.isNotBlank(),
            ) {
                Text(if (isInviting) "Inviting..." else "Invite")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RemoveMemberDialog(
    member: BusinessMemberItem,
    isRemoving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove member") },
        text = {
            Column {
                Text("Remove ${member.name} (${member.email}) from the team?")
                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isRemoving) {
                Text(if (isRemoving) "Removing..." else "Remove")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
