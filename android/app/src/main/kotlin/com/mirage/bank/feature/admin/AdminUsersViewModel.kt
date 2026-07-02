package com.mirage.bank.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.AdminUserItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val isLoading: Boolean = true,
    val users: List<AdminUserItem> = emptyList(),
    val filter: String = "",
    val error: String? = null,
    val actionInProgressUserId: String? = null,
) {
    val filteredUsers: List<AdminUserItem>
        get() = if (filter.isBlank()) {
            users
        } else {
            users.filter {
                it.name.contains(filter, ignoreCase = true) || it.email.contains(filter, ignoreCase = true)
            }
        }
}

class AdminUsersViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.listUsers()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, users = result.data.users) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun onFilterChange(value: String) {
        _uiState.update { it.copy(filter = value) }
    }

    fun freeze(userId: String) = runAction(userId) { repository.freeze(userId) }

    fun unfreeze(userId: String) = runAction(userId) { repository.unfreeze(userId) }

    fun disable(userId: String) = runAction(userId) { repository.disable(userId) }

    fun credit(userId: String, amountCents: Long, description: String?) =
        runAction(userId) { repository.credit(userId, amountCents, description) }

    private fun runAction(userId: String, block: suspend () -> ApiResult<*>) {
        if (_uiState.value.actionInProgressUserId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgressUserId = userId, error = null) }
            when (val result = block()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(actionInProgressUserId = null) }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(actionInProgressUserId = null, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(actionInProgressUserId = null, error = "Couldn't reach the server")
                }
            }
        }
    }
}
