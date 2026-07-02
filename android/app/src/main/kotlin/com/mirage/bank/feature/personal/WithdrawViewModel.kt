package com.mirage.bank.feature.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WithdrawUiState(
    val amountInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successNewBalanceCents: Long? = null,
)

class WithdrawViewModel(private val repository: AccountRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState

    fun onAmountChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amountInput = value, error = null) }
        }
    }

    fun withdraw() {
        val cents = _uiState.value.amountInput.toDoubleOrNull()?.let { (it * 100).toLong() }
        if (cents == null || cents <= 0) {
            _uiState.update { it.copy(error = "Enter a valid amount") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.withdraw(cents)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, successNewBalanceCents = result.data.newBalanceCents)
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }
}
