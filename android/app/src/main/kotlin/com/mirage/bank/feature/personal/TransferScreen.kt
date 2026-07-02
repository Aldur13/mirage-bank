package com.mirage.bank.feature.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.mirage.bank.core.di.LocalAppContainer

@Composable
fun TransferScreen(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: TransferViewModel = viewModel(
        factory = GenericViewModelFactory { TransferViewModel(AccountRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Transfer money", style = MaterialTheme.typography.headlineSmall)

        if (state.successMessage != null) {
            Text(state.successMessage!!, modifier = Modifier.padding(top = 16.dp))
            LaunchedEffect(Unit) { onDone() }
            return@Column
        }

        OutlinedTextField(
            value = state.toEmail,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Recipient email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        OutlinedTextField(
            value = state.amountInput,
            onValueChange = viewModel::onAmountChange,
            label = { Text("Amount (EUR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Text("Category (optional)", modifier = Modifier.padding(top = 16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            items(TRANSFER_CATEGORIES) { cat ->
                FilterChip(
                    selected = state.category == cat,
                    onClick = { viewModel.onCategoryChange(if (state.category == cat) null else cat) },
                    label = { Text(cat) },
                )
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        Button(
            onClick = viewModel::transfer,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (state.isLoading) "Sending..." else "Send")
        }
    }
}
