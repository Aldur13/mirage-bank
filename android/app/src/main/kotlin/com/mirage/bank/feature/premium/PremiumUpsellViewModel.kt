package com.mirage.bank.feature.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.PremiumStatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PremiumUpsellUiState(
    val isLoading: Boolean = true,
    val status: PremiumStatusResponse? = null,
    val isPurchasing: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val purchaseSuccessMessage: String? = null,
    val error: String? = null,
)

class PremiumUpsellViewModel(private val repository: PremiumRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PremiumUpsellUiState())
    val uiState: StateFlow<PremiumUpsellUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getPremiumStatus()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, status = result.data) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun onBuyClick() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmPurchase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, showConfirmDialog = false, error = null) }
            when (val result = repository.purchasePremium()) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isPurchasing = false, purchaseSuccessMessage = result.data.message) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isPurchasing = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isPurchasing = false, error = "Couldn't reach the server") }
            }
        }
    }
}
