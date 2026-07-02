package com.mirage.bank.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.TicketDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TicketDetailUiState(
    val isLoading: Boolean = true,
    val ticket: TicketDetail? = null,
    val error: String? = null,
    val replyText: String = "",
    val isSending: Boolean = false,
    val isClosing: Boolean = false,
) {
    val canReply: Boolean
        get() = ticket != null && ticket.status !in CLOSED_TICKET_STATUSES
}

class TicketDetailViewModel(private val repository: SupportRepository, private val ticketId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getTicket(ticketId)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, ticket = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun onReplyTextChange(value: String) {
        _uiState.update { it.copy(replyText = value) }
    }

    fun sendReply() {
        val content = _uiState.value.replyText.trim()
        if (content.isEmpty() || _uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            when (val result = repository.addTicketMessage(ticketId, content)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSending = false, replyText = "") }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isSending = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isSending = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun closeTicket() {
        if (_uiState.value.isClosing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClosing = true, error = null) }
            when (val result = repository.closeTicket(ticketId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isClosing = false) }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isClosing = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isClosing = false, error = "Couldn't reach the server") }
            }
        }
    }
}
