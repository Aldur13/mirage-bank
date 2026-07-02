package com.mirage.bank.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusinessMemberItem(
    @SerialName("user_id") val userId: String,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("joined_at") val joinedAt: String,
)

@Serializable
data class BusinessMembersResponse(
    @SerialName("org_id") val orgId: String,
    @SerialName("company_name") val companyName: String,
    val members: List<BusinessMemberItem>,
)

@Serializable
data class BusinessInviteRequest(
    val email: String,
    val role: String,
)
