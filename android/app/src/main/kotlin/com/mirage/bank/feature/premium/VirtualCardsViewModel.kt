package com.mirage.bank.feature.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.VirtualCardItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VirtualCardsUiState(
    val isLoading: Boolean = true,
    val cards: List<VirtualCardItem> = emptyList(),
    val showCreateDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val revealCard: VirtualCardItem? = null,
    val error: String? = null,
)

class VirtualCardsViewModel(private val repository: PremiumRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(VirtualCardsUiState())
    val uiState: StateFlow<VirtualCardsUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.listCards()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, cards = result.data.cards) }
                is ApiResult.HttpError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createCard(label: String?, spendingLimitCents: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (val result = repository.createCard(label, spendingLimitCents)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, showCreateDialog = false, revealCard = result.data) }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isSubmitting = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isSubmitting = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun dismissReveal() {
        _uiState.update { it.copy(revealCard = null) }
    }

    fun toggleFrozen(card: VirtualCardItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            when (val result = repository.updateCard(card.id, frozen = !card.frozen)) {
                is ApiResult.Success -> refresh()
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
            }
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            when (val result = repository.deleteCard(id)) {
                is ApiResult.Success -> refresh()
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
            }
        }
    }
}
