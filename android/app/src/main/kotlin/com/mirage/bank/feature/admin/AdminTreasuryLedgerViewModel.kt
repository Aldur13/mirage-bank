package com.mirage.bank.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.dto.LedgerResponse
import com.mirage.bank.core.network.dto.TreasuryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminTreasuryLedgerUiState(
    val isLoading: Boolean = true,
    val treasury: TreasuryResponse? = null,
    val ledger: LedgerResponse? = null,
    val error: String? = null,
)

class AdminTreasuryLedgerViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminTreasuryLedgerUiState())
    val uiState: StateFlow<AdminTreasuryLedgerUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val treasuryResult = repository.treasury()
            val ledgerResult = repository.ledger()

            val treasury = (treasuryResult as? ApiResult.Success)?.data
            val ledger = (ledgerResult as? ApiResult.Success)?.data

            val error = when {
                treasuryResult is ApiResult.HttpError -> treasuryResult.message
                ledgerResult is ApiResult.HttpError -> ledgerResult.message
                treasuryResult is ApiResult.NetworkError || ledgerResult is ApiResult.NetworkError -> "Couldn't reach the server"
                else -> null
            }

            _uiState.update { it.copy(isLoading = false, treasury = treasury, ledger = ledger, error = error) }
        }
    }
}
