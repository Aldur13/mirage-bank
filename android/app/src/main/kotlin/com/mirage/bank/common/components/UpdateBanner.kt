package com.mirage.bank.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mirage.bank.core.update.AvailableUpdate
import com.mirage.bank.core.update.UpdateUiState

/** Shown on Home/Settings whenever UpdateRepository has something to say. Idle/Checking render nothing. */
@Composable
fun UpdateBanner(
    state: UpdateUiState,
    onDownload: (AvailableUpdate) -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is UpdateUiState.Available -> Card(modifier = modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Update available: v${state.update.versionName}")
                if (state.update.releaseNotes.isNotBlank()) {
                    Text(state.update.releaseNotes, modifier = Modifier.padding(top = 4.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Later") }
                    Button(onClick = { onDownload(state.update) }) { Text("Download") }
                }
            }
        }
        is UpdateUiState.Downloading -> Card(modifier = modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                Text("Downloading update...")
            }
        }
        is UpdateUiState.ReadyToInstall -> Card(modifier = modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Update downloaded")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Later") }
                    Button(onClick = onInstall) { Text("Install") }
                }
            }
        }
        is UpdateUiState.Error -> Card(modifier = modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Update check failed: ${state.message}")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            }
        }
        UpdateUiState.Idle, UpdateUiState.Checking -> Unit
    }
}
