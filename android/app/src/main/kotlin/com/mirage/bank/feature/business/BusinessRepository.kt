package com.mirage.bank.feature.business

import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.BusinessInviteRequest
import com.mirage.bank.core.network.dto.BusinessMembersResponse
import com.mirage.bank.core.network.dto.MessageResponse

class BusinessRepository(private val api: ApiService) {
    suspend fun getMembers(): ApiResult<BusinessMembersResponse> = apiCall { api.getMembers() }

    suspend fun invite(email: String, role: String): ApiResult<MessageResponse> =
        apiCall { api.invite(BusinessInviteRequest(email, role)) }

    suspend fun removeMember(id: String): ApiResult<MessageResponse> =
        apiCall { api.removeMember(id) }
}
