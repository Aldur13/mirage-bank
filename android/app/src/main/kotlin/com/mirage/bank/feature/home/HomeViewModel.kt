package com.mirage.bank.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.MeResponse
import com.mirage.bank.core.session.SessionManager
import com.mirage.bank.core.update.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val me: MeResponse? = null,
    val isGuardian: Boolean = false,
    val isBusinessOwner: Boolean = false,
    val sessionExpiredOrError: String? = null,
)

/**
 * There's no is_guardian/is_business_owner flag on GET /me -- both are
 * relational facts the backend infers. We determine them the same way the
 * web frontend implicitly does: probe GET /guardian/ward and
 * GET /business/members once after login (404/403 = not applicable) and
 * cache the booleans in SessionManager for the rest of the session.
 */
class HomeViewModel(
    private val api: ApiService,
    private val sessionManager: SessionManager,
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    val updateState = updateRepository.state

    init {
        load()
        viewModelScope.launch { updateRepository.checkOnLaunchIfDue() }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionExpiredOrError = null) }

            when (val meResult = apiCall { api.getMe() }) {
                is ApiResult.Success<*> -> sessionManager.updateCurrentUser(meResult.data as MeResponse)
                is ApiResult.HttpError -> {
                    _uiState.update { it.copy(isLoading = false, sessionExpiredOrError = meResult.message) }
                    return@launch
                }
                is ApiResult.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, sessionExpiredOrError = "Couldn't reach the server") }
                    return@launch
                }
            }

            val isGuardian = apiCall { api.getWard() } is ApiResult.Success<*>
            val isBusinessOwner = apiCall { api.getMembers() } is ApiResult.Success<*>
            sessionManager.updateRoleProbes(isGuardian, isBusinessOwner)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    me = sessionManager.currentUser.value,
                    isGuardian = isGuardian,
                    isBusinessOwner = isBusinessOwner,
                )
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch { updateRepository.checkNow() }
    }
}
