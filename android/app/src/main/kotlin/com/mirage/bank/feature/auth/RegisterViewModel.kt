package com.mirage.bank.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountTypeOption { PERSONAL, YOUTH, BUSINESS }

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val accountType: AccountTypeOption = AccountTypeOption.PERSONAL,
    val guardianEmail: String = "",
    val companyName: String = "",
    val companyReg: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onAccountTypeChange(v: AccountTypeOption) = _uiState.update { it.copy(accountType = v, error = null) }
    fun onGuardianEmailChange(v: String) = _uiState.update { it.copy(guardianEmail = v, error = null) }
    fun onCompanyNameChange(v: String) = _uiState.update { it.copy(companyName = v, error = null) }
    fun onCompanyRegChange(v: String) = _uiState.update { it.copy(companyReg = v, error = null) }

    fun register() {
        val s = _uiState.value
        if (s.name.isBlank() || s.email.isBlank() || s.password.length < 8) {
            _uiState.update { it.copy(error = "Fill in all fields — password needs at least 8 characters") }
            return
        }
        if (s.accountType == AccountTypeOption.YOUTH && s.guardianEmail.isBlank()) {
            _uiState.update { it.copy(error = "Guardian email is required for a youth account") }
            return
        }
        if (s.accountType == AccountTypeOption.BUSINESS && s.companyName.isBlank()) {
            _uiState.update { it.copy(error = "Company name is required for a business account") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = RegisterRequest(
                name = s.name,
                email = s.email,
                password = s.password,
                accountType = s.accountType.name.lowercase(),
                guardianEmail = s.guardianEmail.ifBlank { null },
                companyName = s.companyName.ifBlank { null },
                companyReg = s.companyReg.ifBlank { null },
            )
            when (val result = repository.register(request)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, error = "Couldn't reach the server. Try again.")
                }
            }
        }
    }
}
