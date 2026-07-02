package com.mirage.bank.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserItem(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("balance_cents") val balanceCents: Long,
    val currency: String,
)

@Serializable
data class AdminUsersResponse(
    val users: List<AdminUserItem>,
)

@Serializable
data class AdminTransactionItem(
    val id: String,
    val type: String,
    @SerialName("amount_cents") val amountCents: Long,
    val timestamp: String,
    val status: String,
    val description: String,
    @SerialName("from_name") val fromName: String,
    @SerialName("to_name") val toName: String,
)

@Serializable
data class AdminTransactionsResponse(
    val transactions: List<AdminTransactionItem>,
)

@Serializable
data class UserActionRequest(
    @SerialName("user_id") val userId: String,
)

@Serializable
data class UserActionResponse(
    val message: String,
    @SerialName("user_id") val userId: String,
    val status: String,
)

@Serializable
data class CreditRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("amount_cents") val amountCents: Long,
    val description: String? = null,
)

@Serializable
data class CreditResponse(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("new_balance_cents") val newBalanceCents: Long,
    val currency: String,
    @SerialName("to_name") val toName: String,
    val description: String? = null,
)

@Serializable
data class TreasuryResponse(
    @SerialName("account_id") val accountId: String,
    @SerialName("balance_cents") val balanceCents: Long,
    @SerialName("issued_cents") val issuedCents: Long,
    val currency: String,
)

@Serializable
data class LedgerResponse(
    @SerialName("user_balance_cents") val userBalanceCents: Long,
    @SerialName("treasury_balance_cents") val treasuryBalanceCents: Long,
    @SerialName("total_cents") val totalCents: Long,
    val balanced: Boolean,
    @SerialName("account_count") val accountCount: Int,
)

@Serializable
data class AdminActionItem(
    val id: String,
    val type: String,
    @SerialName("admin_id") val adminId: String,
    @SerialName("admin_name") val adminName: String,
    @SerialName("target_user") val targetUser: String,
    @SerialName("target_name") val targetName: String,
    @SerialName("amount_cents") val amountCents: Long? = null,
    val timestamp: String,
)

@Serializable
data class AdminActionsResponse(
    val actions: List<AdminActionItem>,
)
