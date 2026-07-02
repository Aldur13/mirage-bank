package com.mirage.bank.core.session

import com.mirage.bank.core.network.dto.MeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single in-memory source of truth for "am I logged in and as whom", backed
 * by [SecureTokenStore] for persistence across process death. There is no
 * refresh token -- once the 60-minute access token expires, [logout] runs
 * and the user re-authenticates from Login.
 */
class SessionManager(private val tokenStore: SecureTokenStore) {

    private val _accessToken = MutableStateFlow<String?>(tokenStore.readToken())
    val accessToken: StateFlow<String?> = _accessToken

    private val _currentUser = MutableStateFlow<MeResponse?>(null)
    val currentUser: StateFlow<MeResponse?> = _currentUser

    /** Cached once per login from probing GET /guardian/ward and GET /business/members. */
    private val _isGuardian = MutableStateFlow(false)
    val isGuardian: StateFlow<Boolean> = _isGuardian

    private val _isBusinessOwner = MutableStateFlow(false)
    val isBusinessOwner: StateFlow<Boolean> = _isBusinessOwner

    fun hasValidLocalToken(): Boolean {
        val token = _accessToken.value ?: return false
        return !JwtUtils.isExpired(token, System.currentTimeMillis() / 1000)
    }

    fun onLoginSuccess(token: String) {
        tokenStore.saveToken(token)
        _accessToken.value = token
    }

    fun updateCurrentUser(me: MeResponse) {
        _currentUser.value = me
    }

    fun updateRoleProbes(isGuardian: Boolean, isBusinessOwner: Boolean) {
        _isGuardian.value = isGuardian
        _isBusinessOwner.value = isBusinessOwner
    }

    fun logout() {
        tokenStore.clear()
        _accessToken.value = null
        _currentUser.value = null
        _isGuardian.value = false
        _isBusinessOwner.value = false
    }
}
