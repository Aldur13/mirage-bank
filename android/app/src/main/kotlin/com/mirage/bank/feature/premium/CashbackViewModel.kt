package com.mirage.bank.feature.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.CashbackResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CashbackUiState(
    val isLoading: Boolean = true,
    val cashback: CashbackResponse? = null,
    val error: String? = null,
)

class CashbackViewModel(private val repository: PremiumRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CashbackUiState())
    val uiState: StateFlow<CashbackUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getCashback()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, cashback = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }
}
