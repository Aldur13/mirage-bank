package com.mirage.bank.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class HealthResponse(val status: String)

/** Shape of FastAPI's default error body: {"detail": "..."} or {"detail": [...]}. */
@Serializable
data class ApiErrorBody(
    val detail: String? = null,
)
