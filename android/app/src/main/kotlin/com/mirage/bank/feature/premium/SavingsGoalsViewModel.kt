package com.mirage.bank.feature.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.SavingsGoalItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavingsGoalsUiState(
    val isLoading: Boolean = true,
    val goals: List<SavingsGoalItem> = emptyList(),
    val showCreateDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

class SavingsGoalsViewModel(private val repository: PremiumRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SavingsGoalsUiState())
    val uiState: StateFlow<SavingsGoalsUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.listGoals()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, goals = result.data.goals) }
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

    fun createGoal(name: String, targetAmountCents: Long, deadline: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (val result = repository.createGoal(name = name, targetAmountCents = targetAmountCents, deadline = deadline)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, showCreateDialog = false) }
                    refresh()
                }
                is ApiResult.HttpError -> _uiState.update { it.copy(isSubmitting = false, error = result.message) }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isSubmitting = false, error = "Couldn't reach the server") }
            }
        }
    }

    fun contribute(goalId: String, contributeCents: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            when (val result = repository.updateGoal(id = goalId, contributeCents = contributeCents)) {
                is ApiResult.Success -> refresh()
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
            }
        }
    }

    fun setCurrentAmount(goalId: String, currentAmountCents: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            when (val result = repository.updateGoal(id = goalId, currentAmountCents = currentAmountCents)) {
                is ApiResult.Success -> refresh()
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            when (val result = repository.deleteGoal(goalId)) {
                is ApiResult.Success -> refresh()
                is ApiResult.HttpError -> _uiState.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(error = "Couldn't reach the server") }
            }
        }
    }
}
