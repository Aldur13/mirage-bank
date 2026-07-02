package com.mirage.bank.feature.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferUiState(
    val toEmail: String = "",
    val amountInput: String = "",
    val category: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

class TransferViewModel(private val repository: AccountRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState

    fun onEmailChange(value: String) = _uiState.update { it.copy(toEmail = value, error = null) }
    fun onCategoryChange(value: String?) = _uiState.update { it.copy(category = value) }
    fun onAmountChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amountInput = value, error = null) }
        }
    }

    fun transfer() {
        val s = _uiState.value
        val cents = s.amountInput.toDoubleOrNull()?.let { (it * 100).toLong() }
        if (s.toEmail.isBlank() || cents == null || cents <= 0) {
            _uiState.update { it.copy(error = "Enter a recipient email and a valid amount") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.transfer(s.toEmail, cents, s.category)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, successMessage = "Sent to ${result.data.toName}")
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }
}
