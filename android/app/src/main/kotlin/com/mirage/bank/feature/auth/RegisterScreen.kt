package com.mirage.bank.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.core.di.LocalAppContainer

@Composable
fun RegisterScreen(onRegistered: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: RegisterViewModel = viewModel(
        factory = GenericViewModelFactory { RegisterViewModel(AuthRepository(container.apiService, container.sessionManager)) },
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onRegistered()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text("Create your account", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.name, onValueChange = viewModel::onNameChange,
            label = { Text("Full name") }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        OutlinedTextField(
            value = state.email, onValueChange = viewModel::onEmailChange,
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = state.password, onValueChange = viewModel::onPasswordChange,
            label = { Text("Password (8+ characters)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Text("Account type", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountTypeOption.entries.forEach { option ->
                FilterChip(
                    selected = state.accountType == option,
                    onClick = { viewModel.onAccountTypeChange(option) },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        when (state.accountType) {
            AccountTypeOption.YOUTH -> OutlinedTextField(
                value = state.guardianEmail, onValueChange = viewModel::onGuardianEmailChange,
                label = { Text("Guardian's email (must already have a Mirage account)") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            AccountTypeOption.BUSINESS -> {
                OutlinedTextField(
                    value = state.companyName, onValueChange = viewModel::onCompanyNameChange,
                    label = { Text("Company name") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = state.companyReg, onValueChange = viewModel::onCompanyRegChange,
                    label = { Text("Company registration number (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            AccountTypeOption.PERSONAL -> Unit
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }

        Button(
            onClick = viewModel::register,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (state.isLoading) "Creating account..." else "Register")
        }
    }
}
