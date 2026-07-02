package com.mirage.bank.core.network

import com.mirage.bank.core.network.dto.ApiErrorBody
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
}

private val errorJson = Json { ignoreUnknownKeys = true }

/** Wraps a suspend Retrofit call, translating exceptions into an [ApiResult] the UI can render. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        val detail = body?.let {
            runCatching { errorJson.decodeFromString<ApiErrorBody>(it).detail }.getOrNull()
        } ?: e.message()
        ApiResult.HttpError(e.code(), detail ?: "Request failed (${e.code()})")
    } catch (e: IOException) {
        ApiResult.NetworkError(e)
    }
}
