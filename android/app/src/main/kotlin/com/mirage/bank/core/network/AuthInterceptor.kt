package com.mirage.bank.core.network

import com.mirage.bank.core.session.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches `Authorization: Bearer <token>` to every request except the public auth endpoints. */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    private val publicPaths = setOf("register", "login", "health")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.trimStart('/')
        val isPublic = publicPaths.any { path.endsWith(it) }

        if (isPublic) {
            return chain.proceed(request)
        }

        val token = sessionManager.accessToken.value
        val authedRequest = if (token != null) {
            request.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authedRequest)
    }
}

/** Detects a 401 from the backend and notifies the app to force a re-login; never retries. */
class SessionExpiryInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            sessionManager.logout()
            runBlocking { com.mirage.bank.core.session.AuthEventBus.emitSessionExpired() }
        }
        return response
    }
}
