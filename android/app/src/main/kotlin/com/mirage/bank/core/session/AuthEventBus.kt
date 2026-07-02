package com.mirage.bank.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Emits when the backend rejects the current token (HTTP 401) so the UI can force a re-login. */
object AuthEventBus {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    suspend fun emitSessionExpired() {
        _sessionExpired.emit(Unit)
    }
}
