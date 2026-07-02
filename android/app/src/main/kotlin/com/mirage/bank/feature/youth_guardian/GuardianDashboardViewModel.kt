package com.mirage.bank.feature.youth_guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.GuardianWardResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuardianDashboardUiState(
    val isLoading: Boolean = true,
    val ward: GuardianWardResponse? = null,
    val error: String? = null,
    val isActionInProgress: Boolean = false,
)

class GuardianDashboardViewModel(private val repository: GuardianRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GuardianDashboardUiState())
    val uiState: StateFlow<GuardianDashboardUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getWard()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, ward = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun freeze() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, error = null) }
            when (val result = repository.freezeWard()) {
                is ApiResult.Success -> refresh()
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
            }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun unfreeze() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, error = null) }
            when (val result = repository.unfreezeWard()) {
                is ApiResult.Success -> refresh()
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
            }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }
}
