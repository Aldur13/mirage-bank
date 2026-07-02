package com.mirage.bank.core.session

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Reads the exp/sub claims out of a JWT locally, without verifying the
 * signature -- the server is the sole source of truth for validity. This is
 * only used to decide, before making a network call, whether a stored token
 * is worth sending at all.
 */
object JwtUtils {

    private val json = Json { ignoreUnknownKeys = true }

    data class Claims(val subject: String?, val expiresAtEpochSeconds: Long?)

    fun decode(token: String): Claims? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val payload = json.parseToJsonElement(String(payloadBytes, Charsets.UTF_8)).jsonObject
            val sub = payload["sub"]?.jsonPrimitive?.content
            val exp = payload["exp"]?.jsonPrimitive?.content?.toLongOrNull()
            Claims(sub, exp)
        } catch (e: Exception) {
            null
        }
    }

    fun isExpired(token: String, nowEpochSeconds: Long): Boolean {
        val exp = decode(token)?.expiresAtEpochSeconds ?: return true
        return exp <= nowEpochSeconds
    }
}
