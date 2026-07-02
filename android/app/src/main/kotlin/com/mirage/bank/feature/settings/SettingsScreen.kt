package com.mirage.bank.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.BuildConfig
import com.mirage.bank.common.components.UpdateBanner
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.feature.personal.AccountRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onLoggedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel(
        factory = GenericViewModelFactory {
            SettingsViewModel(AccountRepository(container.apiService), container.sessionManager)
        },
    )
    val state by viewModel.uiState.collectAsState()
    val updateState by container.updateRepository.state.collectAsState()
    val me = state.me

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        UpdateBanner(
            state = updateState,
            onDownload = { container.updateRepository.startDownload(it) },
            onInstall = {
                if (container.updateRepository.canInstallUnknownApps()) {
                    container.updateRepository.promptInstall()
                } else {
                    context.startActivity(container.updateRepository.unknownSourcesSettingsIntent())
                }
            },
            onDismiss = { container.updateRepository.dismiss() },
            modifier = Modifier.padding(top = 16.dp),
        )

        if (me != null) {
            Text(me.email, modifier = Modifier.padding(top = 16.dp))
            Text(me.name, modifier = Modifier.padding(top = 4.dp))

            Button(onClick = viewModel::toggleTheme, modifier = Modifier.padding(top = 16.dp)) {
                Text(if (me.theme == "dark") "Switch to light theme" else "Switch to dark theme")
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }

        Text("Version ${BuildConfig.VERSION_NAME}", modifier = Modifier.padding(top = 24.dp))

        Button(
            onClick = { scope.launch { container.updateRepository.checkNow() } },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Check for updates")
        }

        OutlinedButton(
            onClick = {
                viewModel.logout()
                onLoggedOut()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Text("Log out")
        }
    }
}
