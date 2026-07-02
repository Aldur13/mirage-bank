package com.mirage.bank.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuardianWardResponse(
    @SerialName("ward_id") val wardId: String,
    @SerialName("ward_name") val wardName: String,
    @SerialName("ward_email") val wardEmail: String,
    @SerialName("ward_status") val wardStatus: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("balance_cents") val balanceCents: Long,
    val currency: String,
    @SerialName("account_status") val accountStatus: String,
)

@Serializable
data class GuardianActionResponse(
    val message: String,
    @SerialName("ward_id") val wardId: String,
    val status: String,
)
