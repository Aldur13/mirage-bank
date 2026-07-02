package com.mirage.bank.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.feature.personal.AccountRepository

@Composable
fun ProfileScreen(onNavigateToChangePassword: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ProfileViewModel = viewModel(
        factory = GenericViewModelFactory { ProfileViewModel(AccountRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()
    val me = state.me

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)

        if (me != null) {
            Text(me.email, modifier = Modifier.padding(top = 16.dp))
            Text("Account type: ${me.accountType}")
            Text("Status: ${me.status}")

            OutlinedTextField(
                value = state.nameInput,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(onClick = viewModel::saveName, modifier = Modifier.padding(top = 8.dp)) {
                Text("Save name")
            }
            if (state.saved) Text("Saved", modifier = Modifier.padding(top = 4.dp))

            Button(onClick = viewModel::toggleTheme, modifier = Modifier.padding(top = 16.dp)) {
                Text(if (me.theme == "dark") "Switch to light theme" else "Switch to dark theme")
            }

            TextButton(onClick = onNavigateToChangePassword, modifier = Modifier.padding(top = 16.dp)) {
                Text("Change password")
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
    }
}
