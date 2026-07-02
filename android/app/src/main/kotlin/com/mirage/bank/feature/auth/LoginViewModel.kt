package com.mirage.bank.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** Shown after a few seconds of loading -- Render's free tier can take ~50s to wake from idle. */
    val isWakingServer: Boolean = false,
    val error: String? = null,
    val loginSucceeded: Boolean = false,
)

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email and password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isWakingServer = false) }
            val wakeJob = launch {
                delay(4000)
                _uiState.update { it.copy(isWakingServer = true) }
            }
            when (val result = repository.login(state.email, state.password)) {
                is ApiResult.Success -> {
                    wakeJob.cancel()
                    _uiState.update { it.copy(isLoading = false, isWakingServer = false, loginSucceeded = true) }
                }
                is ApiResult.HttpError -> {
                    wakeJob.cancel()
                    _uiState.update { it.copy(isLoading = false, isWakingServer = false, error = result.message) }
                }
                is ApiResult.NetworkError -> {
                    wakeJob.cancel()
                    _uiState.update {
                        it.copy(isLoading = false, isWakingServer = false, error = "Couldn't reach the server. Check your connection and try again.")
                    }
                }
            }
        }
    }
}
