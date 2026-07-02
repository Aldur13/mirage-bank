package com.mirage.bank.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PremiumPurchaseResponse(
    val message: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("new_balance_cents") val newBalanceCents: Long,
    val currency: String,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("premium_since") val premiumSince: String,
)

@Serializable
data class PremiumStatusResponse(
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("premium_since") val premiumSince: String? = null,
    @SerialName("is_premium_concierge") val isPremiumConcierge: Boolean = false,
    @SerialName("price_cents") val priceCents: Long,
)

@Serializable
data class CategorySpend(
    val category: String,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("transaction_count") val transactionCount: Int,
)

@Serializable
data class MonthSpend(
    val month: String,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("transaction_count") val transactionCount: Int,
)

@Serializable
data class SpendingAnalyticsResponse(
    @SerialName("by_category") val byCategory: List<CategorySpend>,
    @SerialName("by_month") val byMonth: List<MonthSpend>,
    @SerialName("total_spent_cents") val totalSpentCents: Long,
    @SerialName("total_transactions") val totalTransactions: Int,
)

@Serializable
data class SavingsGoalCreateRequest(
    val name: String,
    @SerialName("target_amount_cents") val targetAmountCents: Long,
    @SerialName("current_amount_cents") val currentAmountCents: Long = 0,
    val deadline: String? = null,
)

@Serializable
data class SavingsGoalUpdateRequest(
    val name: String? = null,
    @SerialName("target_amount_cents") val targetAmountCents: Long? = null,
    @SerialName("current_amount_cents") val currentAmountCents: Long? = null,
    val deadline: String? = null,
    @SerialName("contribute_cents") val contributeCents: Long? = null,
)

@Serializable
data class SavingsGoalItem(
    val id: String,
    val name: String,
    @SerialName("target_amount_cents") val targetAmountCents: Long,
    @SerialName("current_amount_cents") val currentAmountCents: Long,
    val deadline: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("progress_pct") val progressPct: Double,
)

@Serializable
data class SavingsGoalListResponse(
    val goals: List<SavingsGoalItem>,
)

@Serializable
data class VirtualCardCreateRequest(
    val label: String? = null,
    @SerialName("spending_limit_cents") val spendingLimitCents: Long? = null,
)

@Serializable
data class VirtualCardUpdateRequest(
    val frozen: Boolean? = null,
    val label: String? = null,
)

@Serializable
data class VirtualCardItem(
    val id: String,
    val label: String,
    @SerialName("card_number") val cardNumber: String,
    @SerialName("card_number_full") val cardNumberFull: String? = null,
    val cvv: String? = null,
    val expiry: String,
    val frozen: Boolean,
    @SerialName("spending_limit_cents") val spendingLimitCents: Long? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class VirtualCardListResponse(
    val cards: List<VirtualCardItem>,
)

@Serializable
data class CashbackTxnItem(
    val id: String,
    @SerialName("source_transaction_id") val sourceTransactionId: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("rate_pct") val ratePct: Double,
    val timestamp: String,
    val description: String,
)

@Serializable
data class CashbackResponse(
    @SerialName("total_cashback_cents") val totalCashbackCents: Long,
    @SerialName("rate_pct") val ratePct: Double,
    val transactions: List<CashbackTxnItem>,
)

@Serializable
data class SessionInfo(
    val id: String,
    val device: String,
    val location: String,
    @SerialName("ip_address") val ipAddress: String,
    @SerialName("last_active") val lastActive: String,
    val current: Boolean,
)

@Serializable
data class Advanced2FAResponse(
    @SerialName("two_fa_enabled") val twoFaEnabled: Boolean,
    val method: String,
    val sessions: List<SessionInfo>,
    @SerialName("trusted_devices") val trustedDevices: List<String>,
)
