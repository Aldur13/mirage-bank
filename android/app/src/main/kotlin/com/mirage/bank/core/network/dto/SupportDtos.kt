package com.mirage.bank.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TicketCreateRequest(
    val subject: String,
    val message: String,
)

@Serializable
data class TicketMessageItem(
    val id: String,
    val content: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("is_staff") val isStaff: Boolean,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TicketDetail(
    val id: String,
    val subject: String,
    val status: String,
    val priority: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val messages: List<TicketMessageItem>,
)

@Serializable
data class TicketSummary(
    val id: String,
    val subject: String,
    val status: String,
    val priority: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("message_count") val messageCount: Int,
)

@Serializable
data class TicketListResponse(
    val tickets: List<TicketSummary>,
)

@Serializable
data class TicketMessageCreate(
    val content: String,
)

@Serializable
data class TicketActionResponse(
    val message: String,
    @SerialName("ticket_id") val ticketId: String,
    val status: String,
)

@Serializable
data class AdminTicketSummary(
    val id: String,
    val subject: String,
    val status: String,
    val priority: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("message_count") val messageCount: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_email") val userEmail: String,
)

@Serializable
data class AdminTicketListResponse(
    val tickets: List<AdminTicketSummary>,
)

@Serializable
data class AdminTicketUpdateRequest(
    val status: String? = null,
    val priority: String? = null,
)
