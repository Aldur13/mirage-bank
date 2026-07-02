package com.mirage.bank.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUBJECT_MIN = 3
private const val SUBJECT_MAX = 200
private const val MESSAGE_MIN = 10
private const val MESSAGE_MAX = 4000

data class NewTicketUiState(
    val subject: String = "",
    val message: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdTicketId: String? = null,
) {
    val subjectError: String?
        get() = when {
            subject.isEmpty() -> null
            subject.length < SUBJECT_MIN -> "Subject must be at least $SUBJECT_MIN characters"
            subject.length > SUBJECT_MAX -> "Subject must be at most $SUBJECT_MAX characters"
            else -> null
        }

    val messageError: String?
        get() = when {
            message.isEmpty() -> null
            message.length < MESSAGE_MIN -> "Message must be at least $MESSAGE_MIN characters"
            message.length > MESSAGE_MAX -> "Message must be at most $MESSAGE_MAX characters"
            else -> null
        }

    val canSubmit: Boolean
        get() = !isSubmitting &&
            subject.length in SUBJECT_MIN..SUBJECT_MAX &&
            message.length in MESSAGE_MIN..MESSAGE_MAX
}

class NewTicketViewModel(private val repository: SupportRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NewTicketUiState())
    val uiState: StateFlow<NewTicketUiState> = _uiState

    fun onSubjectChange(value: String) {
        _uiState.update { it.copy(subject = value, error = null) }
    }

    fun onMessageChange(value: String) {
        _uiState.update { it.copy(message = value, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (val result = repository.createTicket(state.subject, state.message)) {
                is ApiResult.Success -> _uiState.update { it.copy(isSubmitting = false, createdTicketId = result.data.id) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isSubmitting = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isSubmitting = false, error = "Couldn't reach the server") }
            }
        }
    }
}
