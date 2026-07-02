package com.mirage.bank.feature.personal

import com.mirage.bank.core.network.ApiResult
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.network.apiCall
import com.mirage.bank.core.network.dto.BalanceResponse
import com.mirage.bank.core.network.dto.MeResponse
import com.mirage.bank.core.network.dto.PasswordChangeRequest
import com.mirage.bank.core.network.dto.PasswordChangeResponse
import com.mirage.bank.core.network.dto.ProfileUpdateRequest
import com.mirage.bank.core.network.dto.ProfileUpdateResponse
import com.mirage.bank.core.network.dto.TransactionsResponse
import com.mirage.bank.core.network.dto.TransferRequest
import com.mirage.bank.core.network.dto.TransferResponse
import com.mirage.bank.core.network.dto.WithdrawRequest
import com.mirage.bank.core.network.dto.WithdrawResponse

class AccountRepository(private val api: ApiService) {
    suspend fun getMe(): ApiResult<MeResponse> = apiCall { api.getMe() }
    suspend fun getBalance(): ApiResult<BalanceResponse> = apiCall { api.getBalance() }
    suspend fun getTransactions(): ApiResult<TransactionsResponse> = apiCall { api.getTransactions() }

    suspend fun withdraw(amountCents: Long): ApiResult<WithdrawResponse> =
        apiCall { api.withdraw(WithdrawRequest(amountCents)) }

    suspend fun transfer(toEmail: String, amountCents: Long, category: String?): ApiResult<TransferResponse> =
        apiCall { api.transfer(TransferRequest(toEmail, amountCents, category)) }

    suspend fun updateProfile(name: String?, avatarData: String?, theme: String?): ApiResult<ProfileUpdateResponse> =
        apiCall { api.updateProfile(ProfileUpdateRequest(name, avatarData, theme)) }

    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<PasswordChangeResponse> =
        apiCall { api.changePassword(PasswordChangeRequest(oldPassword, newPassword)) }
}

/** Matches backend/routes/premium.py's _CATEGORY_KEYWORDS so the transfer UI's picker mirrors real analytics buckets. */
val TRANSFER_CATEGORIES = listOf(
    "food", "transport", "shopping", "utilities", "entertainment", "health", "housing", "other",
)
