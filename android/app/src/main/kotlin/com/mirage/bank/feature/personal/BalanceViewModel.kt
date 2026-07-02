package com.mirage.bank.feature.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.BalanceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BalanceUiState(
    val isLoading: Boolean = true,
    val balance: BalanceResponse? = null,
    val error: String? = null,
)

class BalanceViewModel(private val repository: AccountRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BalanceUiState())
    val uiState: StateFlow<BalanceUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getBalance()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, balance = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }
}
