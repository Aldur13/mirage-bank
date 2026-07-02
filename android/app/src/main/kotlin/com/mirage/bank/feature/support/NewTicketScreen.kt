package com.mirage.bank.feature.support

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.core.di.LocalAppContainer

@Composable
fun NewTicketScreen(onCreated: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: NewTicketViewModel = viewModel(
        factory = GenericViewModelFactory { NewTicketViewModel(SupportRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    if (state.createdTicketId != null) {
        LaunchedEffect(state.createdTicketId) { onCreated(state.createdTicketId!!) }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("New support ticket", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.subject,
            onValueChange = viewModel::onSubjectChange,
            label = { Text("Subject") },
            isError = state.subjectError != null,
            supportingText = {
                Text(state.subjectError ?: "${state.subject.length}/200")
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        OutlinedTextField(
            value = state.message,
            onValueChange = viewModel::onMessageChange,
            label = { Text("Message") },
            isError = state.messageError != null,
            supportingText = {
                Text(state.messageError ?: "${state.message.length}/4000 (min 10)")
            },
            modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp),
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (state.isSubmitting) "Submitting..." else "Submit")
        }
    }
}
