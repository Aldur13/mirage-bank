package com.mirage.bank.feature.personal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer

@Composable
fun WithdrawScreen(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WithdrawViewModel = viewModel(
        factory = GenericViewModelFactory { WithdrawViewModel(AccountRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Withdraw funds", style = MaterialTheme.typography.headlineSmall)

        if (state.successNewBalanceCents != null) {
            Text(
                "Withdrawal complete. New balance: ${formatCents(state.successNewBalanceCents!!)}",
                modifier = Modifier.padding(top = 16.dp),
            )
            LaunchedEffect(Unit) { onDone() }
            return@Column
        }

        OutlinedTextField(
            value = state.amountInput,
            onValueChange = viewModel::onAmountChange,
            label = { Text("Amount (EUR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        Button(
            onClick = viewModel::withdraw,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (state.isLoading) "Processing..." else "Withdraw")
        }
    }
}
