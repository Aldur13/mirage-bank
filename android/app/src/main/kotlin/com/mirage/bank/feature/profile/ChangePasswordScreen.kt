package com.mirage.bank.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.feature.personal.AccountRepository

@Composable
fun ChangePasswordScreen(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ChangePasswordViewModel = viewModel(
        factory = GenericViewModelFactory { ChangePasswordViewModel(AccountRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onDone()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Change password", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.oldPassword,
            onValueChange = viewModel::onOldChange,
            label = { Text("Current password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        OutlinedTextField(
            value = state.newPassword,
            onValueChange = viewModel::onNewChange,
            label = { Text("New password (8+ characters)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        Button(
            onClick = viewModel::submit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (state.isLoading) "Saving..." else "Change password")
        }
    }
}
