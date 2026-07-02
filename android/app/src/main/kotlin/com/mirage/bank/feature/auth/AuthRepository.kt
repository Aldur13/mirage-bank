package com.mirage.bank.feature.auth

import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.LoginRequest
import com.mirage.bank.core.network.dto.LoginResponse
import com.mirage.bank.core.network.dto.RegisterRequest
import com.mirage.bank.core.network.dto.RegisterResponse
import com.mirage.bank.core.session.SessionManager

class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager,
) {
    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        val result = apiCall { api.login(LoginRequest(email, password)) }
        if (result is ApiResult.Success<*>) {
            sessionManager.onLoginSuccess((result.data as LoginResponse).accessToken)
        }
        return result
    }

    suspend fun register(request: RegisterRequest): ApiResult<RegisterResponse> =
        apiCall { api.register(request) }
}
