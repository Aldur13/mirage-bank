package com.mirage.bank.core.network

import com.mirage.bank.core.network.dto.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Mirrors the Mirage Bank FastAPI backend 1:1 (see backend/routes/ *.py and
 * backend/models.py). Paths are relative to BuildConfig.API_BASE_URL, which
 * already ends in a slash.
 */
interface ApiService {

    // ---- Auth (no Authorization header) ----
    @POST("register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // ---- Account ----
    @GET("me")
    suspend fun getMe(): MeResponse

    @PATCH("profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): ProfileUpdateResponse

    @POST("profile/password")
    suspend fun changePassword(@Body body: PasswordChangeRequest): PasswordChangeResponse

    @GET("balance")
    suspend fun getBalance(): BalanceResponse

    @POST("withdraw")
    suspend fun withdraw(@Body body: WithdrawRequest): WithdrawResponse

    @POST("transfer")
    suspend fun transfer(@Body body: TransferRequest): TransferResponse

    @GET("transactions")
    suspend fun getTransactions(): TransactionsResponse

    // ---- Guardian ----
    @GET("guardian/ward")
    suspend fun getWard(): GuardianWardResponse

    @GET("guardian/transactions")
    suspend fun getWardTransactions(): TransactionsResponse

    @POST("guardian/freeze")
    suspend fun freezeWard(): GuardianActionResponse

    @POST("guardian/unfreeze")
    suspend fun unfreezeWard(): GuardianActionResponse

    // ---- Business ----
    @GET("business/members")
    suspend fun getMembers(): BusinessMembersResponse

    @POST("business/invite")
    suspend fun invite(@Body body: BusinessInviteRequest): MessageResponse

    @DELETE("business/members/{id}")
    suspend fun removeMember(@Path("id") id: String): MessageResponse

    // ---- Support ----
    @POST("support/tickets")
    suspend fun createTicket(@Body body: TicketCreateRequest): TicketDetail

    @GET("support/tickets")
    suspend fun listTickets(): TicketListResponse

    @GET("support/tickets/{id}")
    suspend fun getTicket(@Path("id") id: String): TicketDetail

    @POST("support/tickets/{id}/messages")
    suspend fun addTicketMessage(@Path("id") id: String, @Body body: TicketMessageCreate): TicketMessageItem

    @POST("support/tickets/{id}/close")
    suspend fun closeTicket(@Path("id") id: String): TicketActionResponse

    // ---- Premium ----
    @POST("premium/purchase")
    suspend fun purchasePremium(): PremiumPurchaseResponse

    @GET("premium/status")
    suspend fun getPremiumStatus(): PremiumStatusResponse

    @GET("premium/analytics/spending")
    suspend fun getSpendingAnalytics(): SpendingAnalyticsResponse

    @POST("premium/savings-goal")
    suspend fun createGoal(@Body body: SavingsGoalCreateRequest): SavingsGoalItem

    @GET("premium/savings-goal")
    suspend fun listGoals(): SavingsGoalListResponse

    @PATCH("premium/savings-goal/{id}")
    suspend fun updateGoal(@Path("id") id: String, @Body body: SavingsGoalUpdateRequest): SavingsGoalItem

    @DELETE("premium/savings-goal/{id}")
    suspend fun deleteGoal(@Path("id") id: String): MessageResponse

    @POST("premium/virtual-card")
    suspend fun createCard(@Body body: VirtualCardCreateRequest): VirtualCardItem

    @GET("premium/virtual-card")
    suspend fun listCards(): VirtualCardListResponse

    @PATCH("premium/virtual-card/{id}")
    suspend fun updateCard(@Path("id") id: String, @Body body: VirtualCardUpdateRequest): VirtualCardItem

    @DELETE("premium/virtual-card/{id}")
    suspend fun deleteCard(@Path("id") id: String): MessageResponse

    @GET("premium/cashback")
    suspend fun getCashback(): CashbackResponse

    @GET("premium/advanced-2fa")
    suspend fun getAdvanced2fa(): Advanced2FAResponse

    // ---- Admin ----
    @GET("admin/users")
    suspend fun adminListUsers(): AdminUsersResponse

    @GET("admin/transactions")
    suspend fun adminListTransactions(): AdminTransactionsResponse

    @POST("admin/freeze")
    suspend fun adminFreeze(@Body body: UserActionRequest): UserActionResponse

    @POST("admin/unfreeze")
    suspend fun adminUnfreeze(@Body body: UserActionRequest): UserActionResponse

    @POST("admin/disable")
    suspend fun adminDisable(@Body body: UserActionRequest): UserActionResponse

    @POST("admin/credit")
    suspend fun adminCredit(@Body body: CreditRequest): CreditResponse

    @GET("admin/treasury")
    suspend fun adminTreasury(): TreasuryResponse

    @GET("admin/ledger")
    suspend fun adminLedger(): LedgerResponse

    @GET("admin/actions")
    suspend fun adminActions(): AdminActionsResponse

    @GET("admin/support/tickets")
    suspend fun adminListTickets(): AdminTicketListResponse

    @GET("admin/support/tickets/{id}")
    suspend fun adminGetTicket(@Path("id") id: String): TicketDetail

    @PATCH("admin/support/tickets/{id}")
    suspend fun adminUpdateTicket(@Path("id") id: String, @Body body: AdminTicketUpdateRequest): TicketActionResponse

    @POST("admin/support/tickets/{id}/messages")
    suspend fun adminReplyTicket(@Path("id") id: String, @Body body: TicketMessageCreate): TicketMessageItem

    @GET("health")
    suspend fun health(): HealthResponse
}
