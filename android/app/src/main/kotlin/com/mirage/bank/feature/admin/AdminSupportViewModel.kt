package com.mirage.bank.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.AdminTicketSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminSupportUiState(
    val isLoading: Boolean = true,
    val tickets: List<AdminTicketSummary> = emptyList(),
    val error: String? = null,
)

class AdminSupportViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminSupportUiState())
    val uiState: StateFlow<AdminSupportUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.listTickets()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, tickets = result.data.tickets) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }
}
