package com.mirage.bank.feature.youth_guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.TransactionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuardianWardTransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionItem> = emptyList(),
    val error: String? = null,
)

class GuardianWardTransactionsViewModel(private val repository: GuardianRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GuardianWardTransactionsUiState())
    val uiState: StateFlow<GuardianWardTransactionsUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getWardTransactions()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, transactions = result.data.transactions)
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }
}
