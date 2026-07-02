package com.mirage.bank.feature.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.SpendingAnalyticsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpendingAnalyticsUiState(
    val isLoading: Boolean = true,
    val analytics: SpendingAnalyticsResponse? = null,
    val error: String? = null,
)

class SpendingAnalyticsViewModel(private val repository: PremiumRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SpendingAnalyticsUiState())
    val uiState: StateFlow<SpendingAnalyticsUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getSpendingAnalytics()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, analytics = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }
}
