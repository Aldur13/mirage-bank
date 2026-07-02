package com.mirage.bank.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.TicketDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminTicketDetailUiState(
    val isLoading: Boolean = true,
    val ticket: TicketDetail? = null,
    val error: String? = null,
    val replyText: String = "",
    val isSending: Boolean = false,
    val isUpdating: Boolean = false,
)

class AdminTicketDetailViewModel(
    private val repository: AdminRepository,
    private val ticketId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminTicketDetailUiState())
    val uiState: StateFlow<AdminTicketDetailUiState> = _uiState

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
            when (val result = repository.replyTicket(ticketId, content)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSending = false, replyText = "") }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isSending = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isSending = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun updateStatus(status: String) = updateTicket(status = status, priority = null)

    fun updatePriority(priority: String) = updateTicket(status = null, priority = priority)

    private fun updateTicket(status: String?, priority: String?) {
        if (_uiState.value.isUpdating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            when (val result = repository.updateTicket(ticketId, status, priority)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isUpdating = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isUpdating = false, error = "Couldn't reach the server") }
            }
        }
    }
}
