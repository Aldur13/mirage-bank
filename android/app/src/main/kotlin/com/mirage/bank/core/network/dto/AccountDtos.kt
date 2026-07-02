package com.mirage.bank.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
    @SerialName("account_type") val accountType: String,
    val theme: String,
    @SerialName("avatar_data") val avatarData: String? = null,
    @SerialName("company_name") val companyName: String? = null,
    @SerialName("company_reg") val companyReg: String? = null,
    @SerialName("guardian_id") val guardianId: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("premium_since") val premiumSince: String? = null,
    @SerialName("is_premium_concierge") val isPremiumConcierge: Boolean = false,
)

@Serializable
data class ProfileUpdateRequest(
    val name: String? = null,
    @SerialName("avatar_data") val avatarData: String? = null,
    val theme: String? = null,
)

@Serializable
data class ProfileUpdateResponse(val message: String)

@Serializable
data class PasswordChangeRequest(
    @SerialName("old_password") val oldPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class PasswordChangeResponse(val message: String)

@Serializable
data class BalanceResponse(
    @SerialName("account_id") val accountId: String,
    @SerialName("balance_cents") val balanceCents: Long,
    val currency: String,
    val status: String,
    @SerialName("account_type") val accountType: String,
)

@Serializable
data class WithdrawRequest(
    @SerialName("amount_cents") val amountCents: Long,
)

@Serializable
data class WithdrawResponse(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("new_balance_cents") val newBalanceCents: Long,
    val currency: String,
)

@Serializable
data class TransferRequest(
    @SerialName("to_email") val toEmail: String,
    @SerialName("amount_cents") val amountCents: Long,
    val category: String? = null,
)

@Serializable
data class TransferResponse(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("new_balance_cents") val newBalanceCents: Long,
    val currency: String,
    @SerialName("to_name") val toName: String,
)

@Serializable
data class TransactionItem(
    val id: String,
    val type: String,
    @SerialName("amount_cents") val amountCents: Long,
    val timestamp: String,
    val status: String,
    val description: String,
    val direction: String,
)

@Serializable
data class TransactionsResponse(
    val transactions: List<TransactionItem>,
)
