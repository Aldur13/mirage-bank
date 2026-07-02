package com.mirage.bank.feature.admin

import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.AdminActionsResponse
import com.mirage.bank.core.network.dto.AdminTicketListResponse
import com.mirage.bank.core.network.dto.AdminTicketUpdateRequest
import com.mirage.bank.core.network.dto.AdminTransactionsResponse
import com.mirage.bank.core.network.dto.AdminUsersResponse
import com.mirage.bank.core.network.dto.CreditRequest
import com.mirage.bank.core.network.dto.CreditResponse
import com.mirage.bank.core.network.dto.LedgerResponse
import com.mirage.bank.core.network.dto.TicketActionResponse
import com.mirage.bank.core.network.dto.TicketDetail
import com.mirage.bank.core.network.dto.TicketMessageCreate
import com.mirage.bank.core.network.dto.TicketMessageItem
import com.mirage.bank.core.network.dto.TreasuryResponse
import com.mirage.bank.core.network.dto.UserActionRequest
import com.mirage.bank.core.network.dto.UserActionResponse

class AdminRepository(private val api: ApiService) {
    suspend fun listUsers(): ApiResult<AdminUsersResponse> = apiCall { api.adminListUsers() }

    suspend fun listTransactions(): ApiResult<AdminTransactionsResponse> = apiCall { api.adminListTransactions() }

    suspend fun freeze(userId: String): ApiResult<UserActionResponse> =
        apiCall { api.adminFreeze(UserActionRequest(userId)) }

    suspend fun unfreeze(userId: String): ApiResult<UserActionResponse> =
        apiCall { api.adminUnfreeze(UserActionRequest(userId)) }

    suspend fun disable(userId: String): ApiResult<UserActionResponse> =
        apiCall { api.adminDisable(UserActionRequest(userId)) }

    suspend fun credit(userId: String, amountCents: Long, description: String?): ApiResult<CreditResponse> =
        apiCall { api.adminCredit(CreditRequest(userId, amountCents, description)) }

    suspend fun treasury(): ApiResult<TreasuryResponse> = apiCall { api.adminTreasury() }

    suspend fun ledger(): ApiResult<LedgerResponse> = apiCall { api.adminLedger() }

    suspend fun actions(): ApiResult<AdminActionsResponse> = apiCall { api.adminActions() }

    suspend fun listTickets(): ApiResult<AdminTicketListResponse> = apiCall { api.adminListTickets() }

    suspend fun getTicket(id: String): ApiResult<TicketDetail> = apiCall { api.adminGetTicket(id) }

    suspend fun updateTicket(id: String, status: String?, priority: String?): ApiResult<TicketActionResponse> =
        apiCall { api.adminUpdateTicket(id, AdminTicketUpdateRequest(status, priority)) }

    suspend fun replyTicket(id: String, content: String): ApiResult<TicketMessageItem> =
        apiCall { api.adminReplyTicket(id, TicketMessageCreate(content)) }
}
