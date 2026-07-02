package com.mirage.bank.feature.youth_guardian

import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.GuardianActionResponse
import com.mirage.bank.core.network.dto.GuardianWardResponse
import com.mirage.bank.core.network.dto.TransactionsResponse

class GuardianRepository(private val api: ApiService) {
    suspend fun getWard(): ApiResult<GuardianWardResponse> = apiCall { api.getWard() }
    suspend fun getWardTransactions(): ApiResult<TransactionsResponse> = apiCall { api.getWardTransactions() }
    suspend fun freezeWard(): ApiResult<GuardianActionResponse> = apiCall { api.freezeWard() }
    suspend fun unfreezeWard(): ApiResult<GuardianActionResponse> = apiCall { api.unfreezeWard() }
}
