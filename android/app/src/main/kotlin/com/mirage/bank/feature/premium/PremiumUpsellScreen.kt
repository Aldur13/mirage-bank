package com.mirage.bank.feature.premium

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer

@Composable
fun PremiumUpsellScreen(onPurchased: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: PremiumUpsellViewModel = viewModel(
        factory = GenericViewModelFactory { PremiumUpsellViewModel(PremiumRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    if (state.purchaseSuccessMessage != null) {
        LaunchedEffect(Unit) { onPurchased() }
        return
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.status != null -> {
                val status = state.status!!
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                ) {
                    Text("Mirage Premium", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Unlock spending analytics, savings goals, virtual cards, cashback, and advanced security.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                    if (status.isPremium) {
                        Text(
                            "You're already a Premium member.",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                        status.premiumSince?.let {
                            Text("Since $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        Text(
                            formatCents(status.priceCents),
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                        Text(
                            "One-time charge to your balance",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        state.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                        }

                        Button(
                            onClick = viewModel::onBuyClick,
                            enabled = !state.isPurchasing,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        ) {
                            Text(if (state.isPurchasing) "Processing..." else "Buy Premium")
                        }
                    }
                }

                if (state.showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissConfirmDialog,
                        title = { Text("Confirm purchase") },
                        text = {
                            Text(
                                "This will deduct ${formatCents(status.priceCents)} from your balance. Continue?",
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = viewModel::confirmPurchase) { Text("Buy") }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::dismissConfirmDialog) { Text("Cancel") }
                        },
                    )
                }
            }
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
