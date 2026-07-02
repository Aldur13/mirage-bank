package com.mirage.bank.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.MeResponse
import com.mirage.bank.feature.personal.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val me: MeResponse? = null,
    val nameInput: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

class ProfileViewModel(private val repository: AccountRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getMe()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, me = result.data, nameInput = result.data.name)
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(nameInput = value, saved = false) }

    fun saveName() {
        viewModelScope.launch {
            when (val result = repository.updateProfile(_uiState.value.nameInput, null, null)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(saved = true) }
                    load()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
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
}
