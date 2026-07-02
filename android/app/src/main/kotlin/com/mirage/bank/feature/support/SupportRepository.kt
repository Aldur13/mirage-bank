package com.mirage.bank.feature.support

import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.TicketActionResponse
import com.mirage.bank.core.network.dto.TicketCreateRequest
import com.mirage.bank.core.network.dto.TicketDetail
import com.mirage.bank.core.network.dto.TicketListResponse
import com.mirage.bank.core.network.dto.TicketMessageCreate
import com.mirage.bank.core.network.dto.TicketMessageItem

class SupportRepository(private val api: ApiService) {
    suspend fun listTickets(): ApiResult<TicketListResponse> = apiCall { api.listTickets() }

    suspend fun getTicket(id: String): ApiResult<TicketDetail> = apiCall { api.getTicket(id) }

    suspend fun createTicket(subject: String, message: String): ApiResult<TicketDetail> =
        apiCall { api.createTicket(TicketCreateRequest(subject, message)) }

    suspend fun addTicketMessage(id: String, content: String): ApiResult<TicketMessageItem> =
        apiCall { api.addTicketMessage(id, TicketMessageCreate(content)) }

    suspend fun closeTicket(id: String): ApiResult<TicketActionResponse> = apiCall { api.closeTicket(id) }
}

/** Statuses after which the ticket owner can no longer reply. */
val CLOSED_TICKET_STATUSES = setOf("resolved", "closed")
