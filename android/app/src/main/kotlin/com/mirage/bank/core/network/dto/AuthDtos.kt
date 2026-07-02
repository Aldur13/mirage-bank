package com.mirage.bank.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("guardian_email") val guardianEmail: String? = null,
    @SerialName("company_name") val companyName: String? = null,
    @SerialName("company_reg") val companyReg: String? = null,
)

@Serializable
data class RegisteredUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
)

@Serializable
data class RegisterResponse(
    val message: String,
    val user: RegisteredUser,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val role: String,
)
