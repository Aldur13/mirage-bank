package com.mirage.bank.feature.premium

import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.Advanced2FAResponse
import com.mirage.bank.core.network.dto.CashbackResponse
import com.mirage.bank.core.network.dto.MessageResponse
import com.mirage.bank.core.network.dto.PremiumPurchaseResponse
import com.mirage.bank.core.network.dto.PremiumStatusResponse
import com.mirage.bank.core.network.dto.SavingsGoalCreateRequest
import com.mirage.bank.core.network.dto.SavingsGoalItem
import com.mirage.bank.core.network.dto.SavingsGoalListResponse
import com.mirage.bank.core.network.dto.SavingsGoalUpdateRequest
import com.mirage.bank.core.network.dto.SpendingAnalyticsResponse
import com.mirage.bank.core.network.dto.VirtualCardCreateRequest
import com.mirage.bank.core.network.dto.VirtualCardItem
import com.mirage.bank.core.network.dto.VirtualCardListResponse
import com.mirage.bank.core.network.dto.VirtualCardUpdateRequest

class PremiumRepository(private val api: ApiService) {
    suspend fun purchasePremium(): ApiResult<PremiumPurchaseResponse> = apiCall { api.purchasePremium() }
    suspend fun getPremiumStatus(): ApiResult<PremiumStatusResponse> = apiCall { api.getPremiumStatus() }

    suspend fun getSpendingAnalytics(): ApiResult<SpendingAnalyticsResponse> = apiCall { api.getSpendingAnalytics() }

    suspend fun createGoal(
        name: String,
        targetAmountCents: Long,
        currentAmountCents: Long = 0,
        deadline: String? = null,
    ): ApiResult<SavingsGoalItem> =
        apiCall { api.createGoal(SavingsGoalCreateRequest(name, targetAmountCents, currentAmountCents, deadline)) }

    suspend fun listGoals(): ApiResult<SavingsGoalListResponse> = apiCall { api.listGoals() }

    suspend fun updateGoal(
        id: String,
        name: String? = null,
        targetAmountCents: Long? = null,
        currentAmountCents: Long? = null,
        deadline: String? = null,
        contributeCents: Long? = null,
    ): ApiResult<SavingsGoalItem> =
        apiCall {
            api.updateGoal(
                id,
                SavingsGoalUpdateRequest(name, targetAmountCents, currentAmountCents, deadline, contributeCents),
            )
        }

    suspend fun deleteGoal(id: String): ApiResult<MessageResponse> = apiCall { api.deleteGoal(id) }

    suspend fun createCard(label: String?, spendingLimitCents: Long?): ApiResult<VirtualCardItem> =
        apiCall { api.createCard(VirtualCardCreateRequest(label, spendingLimitCents)) }

    suspend fun listCards(): ApiResult<VirtualCardListResponse> = apiCall { api.listCards() }

    suspend fun updateCard(id: String, frozen: Boolean? = null, label: String? = null): ApiResult<VirtualCardItem> =
        apiCall { api.updateCard(id, VirtualCardUpdateRequest(frozen, label)) }

    suspend fun deleteCard(id: String): ApiResult<MessageResponse> = apiCall { api.deleteCard(id) }

    suspend fun getCashback(): ApiResult<CashbackResponse> = apiCall { api.getCashback() }

    suspend fun getAdvanced2fa(): ApiResult<Advanced2FAResponse> = apiCall { api.getAdvanced2fa() }
}
