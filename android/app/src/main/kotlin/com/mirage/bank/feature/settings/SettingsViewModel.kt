package com.mirage.bank.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.MeResponse
import com.mirage.bank.core.session.SessionManager
import com.mirage.bank.feature.personal.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val me: MeResponse? = null,
    val error: String? = null,
)

class SettingsViewModel(
    private val repository: AccountRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getMe()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, me = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun toggleTheme() {
        val newTheme = if (_uiState.value.me?.theme == "dark") "light" else "dark"
        viewModelScope.launch {
            repository.updateProfile(null, null, newTheme)
            load()
        }
    }

    fun logout() {
        sessionManager.logout()
    }
}
