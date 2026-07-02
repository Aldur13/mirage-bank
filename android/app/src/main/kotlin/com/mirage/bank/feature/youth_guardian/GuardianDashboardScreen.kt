package com.mirage.bank.feature.youth_guardian

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer

@Composable
fun GuardianDashboardScreen(onViewTransactions: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: GuardianDashboardViewModel = viewModel(
        factory = GenericViewModelFactory { GuardianDashboardViewModel(GuardianRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()
    var showFreezeConfirm by remember { mutableStateOf(false) }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.ward == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error ?: "No ward found", color = MaterialTheme.colorScheme.error)
        }
        else -> {
            val ward = state.ward!!
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(ward.wardName, style = MaterialTheme.typography.titleLarge)
                Text(
                    ward.wardEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Text(
                    "Balance",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    formatCents(ward.balanceCents, ward.currency),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Text(
                    "Account status: ${ward.accountStatus}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ward.accountStatus == "frozen") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(top = 16.dp),
                )

                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (ward.accountStatus == "frozen") {
                        Button(
                            onClick = { viewModel.unfreeze() },
                            enabled = !state.isActionInProgress,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Unfreeze account")
                        }
                    } else {
                        Button(
                            onClick = { showFreezeConfirm = true },
                            enabled = !state.isActionInProgress,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Freeze account")
                        }
                    }

                    OutlinedButton(
                        onClick = onViewTransactions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("View transactions")
                    }
                }
            }
        }
    }

    if (showFreezeConfirm) {
        AlertDialog(
            onDismissRequest = { showFreezeConfirm = false },
            title = { Text("Freeze account?") },
            text = { Text("This blocks your ward from making any transactions until you unfreeze the account.") },
            confirmButton = {
                TextButton(onClick = {
                    showFreezeConfirm = false
                    viewModel.freeze()
                }) {
                    Text("Freeze")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFreezeConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
