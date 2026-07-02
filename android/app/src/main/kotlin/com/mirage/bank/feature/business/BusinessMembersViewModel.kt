package com.mirage.bank.feature.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.BusinessMembersResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessMembersUiState(
    val isLoading: Boolean = true,
    val data: BusinessMembersResponse? = null,
    val error: String? = null,
    val isInviting: Boolean = false,
    val inviteError: String? = null,
    val isRemoving: Boolean = false,
    val removeError: String? = null,
)

class BusinessMembersViewModel(private val repository: BusinessRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BusinessMembersUiState())
    val uiState: StateFlow<BusinessMembersUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getMembers()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, data = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun invite(email: String, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInviting = true, inviteError = null) }
            when (val result = repository.invite(email, role)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isInviting = false) }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isInviting = false, inviteError = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isInviting = false, inviteError = "Couldn't reach the server") }
            }
        }
    }

    fun clearInviteError() {
        _uiState.update { it.copy(inviteError = null) }
    }

    fun removeMember(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true, removeError = null) }
            when (val result = repository.removeMember(id)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRemoving = false) }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isRemoving = false, removeError = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isRemoving = false, removeError = "Couldn't reach the server") }
            }
        }
    }

    fun clearRemoveError() {
        _uiState.update { it.copy(removeError = null) }
    }
}
