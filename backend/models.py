from typing import List, Optional
from pydantic import BaseModel, EmailStr, Field


# ── Auth ─────────────────────────────────────────────────────────

class RegisterRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    email: EmailStr
    password: str = Field(..., min_length=8, max_length=72)
    account_type: str = Field("personal", pattern=r"^(personal|youth|business)$")
    # Youth
    guardian_email: Optional[EmailStr] = None
    # Business
    company_name: Optional[str] = Field(None, max_length=200)
    company_reg: Optional[str] = Field(None, max_length=100)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=1)


class UserResponse(BaseModel):
    id: str
    name: str
    email: str
    role: str
    status: str


class RegisterResponse(BaseModel):
    message: str
    user: UserResponse


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: str


class MeResponse(BaseModel):
    id: str
    name: str
    email: str
    role: str
    status: str
    account_type: str = "personal"
    theme: str = "dark"
    avatar_data: Optional[str] = None
    company_name: Optional[str] = None
    company_reg: Optional[str] = None
    guardian_id: Optional[str] = None
    is_premium: bool = False
    premium_since: Optional[str] = None
    is_premium_concierge: bool = False


# ── Profile ──────────────────────────────────────────────────────

class ProfileUpdateRequest(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    avatar_data: Optional[str] = None   # base64 data URI; "" clears avatar
    theme: Optional[str] = Field(None, pattern=r"^(dark|light)$")


class ProfileUpdateResponse(BaseModel):
    message: str


class PasswordChangeRequest(BaseModel):
    old_password: str = Field(..., min_length=1)
    new_password: str = Field(..., min_length=8, max_length=72)


class PasswordChangeResponse(BaseModel):
    message: str


# ── Account ──────────────────────────────────────────────────────

class BalanceResponse(BaseModel):
    account_id: str
    balance_cents: int
    currency: str
    status: str
    account_type: str = "personal"


class WithdrawRequest(BaseModel):
    amount_cents: int = Field(..., gt=0)


class WithdrawResponse(BaseModel):
    transaction_id: str
    amount_cents: int
    new_balance_cents: int
    currency: str


class TransferRequest(BaseModel):
    to_email: EmailStr
    amount_cents: int = Field(..., gt=0)
    category: Optional[str] = Field(None, max_length=30)


class TransferResponse(BaseModel):
    transaction_id: str
    amount_cents: int
    new_balance_cents: int
    currency: str
    to_name: str


class TransactionItem(BaseModel):
    id: str
    type: str
    amount_cents: int
    timestamp: str
    status: str
    description: str
    direction: str


class TransactionsResponse(BaseModel):
    transactions: List[TransactionItem]


# ── Guardian ─────────────────────────────────────────────────────

class GuardianWardResponse(BaseModel):
    ward_id: str
    ward_name: str
    ward_email: str
    ward_status: str
    account_id: str
    balance_cents: int
    currency: str
    account_status: str


class GuardianActionResponse(BaseModel):
    message: str
    ward_id: str
    status: str


# ── Business ─────────────────────────────────────────────────────

class BusinessInviteRequest(BaseModel):
    email: EmailStr
    role: str = Field("employee", pattern=r"^(manager|employee)$")


class BusinessMemberItem(BaseModel):
    user_id: str
    name: str
    email: str
    role: str
    joined_at: str


class BusinessMembersResponse(BaseModel):
    org_id: str
    company_name: str
    members: List[BusinessMemberItem]


class BusinessRemoveMemberRequest(BaseModel):
    user_id: str


# ── Support tickets ──────────────────────────────────────────────

class TicketCreateRequest(BaseModel):
    subject: str = Field(..., min_length=3, max_length=200)
    message: str = Field(..., min_length=10, max_length=4000)


class TicketMessageCreate(BaseModel):
    content: str = Field(..., min_length=1, max_length=4000)


class TicketMessageItem(BaseModel):
    id: str
    content: str
    author_id: str
    author_name: str
    is_staff: bool
    created_at: str


class TicketItem(BaseModel):
    id: str
    subject: str
    status: str
    priority: str
    created_at: str
    updated_at: str
    message_count: int = 0


class TicketDetail(BaseModel):
    id: str
    subject: str
    status: str
    priority: str
    created_at: str
    updated_at: str
    messages: List[TicketMessageItem]


class TicketListResponse(BaseModel):
    tickets: List[TicketItem]


class AdminTicketItem(TicketItem):
    user_id: str
    user_name: str
    user_email: str


class AdminTicketListResponse(BaseModel):
    tickets: List[AdminTicketItem]


class AdminTicketUpdateRequest(BaseModel):
    status: Optional[str] = Field(None, pattern=r"^(open|in_progress|waiting|resolved|closed)$")
    priority: Optional[str] = Field(None, pattern=r"^(low|normal|high|urgent)$")


class TicketActionResponse(BaseModel):
    message: str
    ticket_id: str
    status: str


# ── Admin ────────────────────────────────────────────────────────

class AdminUserItem(BaseModel):
    id: str
    name: str
    email: str
    role: str
    status: str
    account_type: str = "personal"
    balance_cents: int
    currency: str


class AdminUsersResponse(BaseModel):
    users: List[AdminUserItem]


class AdminTransactionItem(BaseModel):
    id: str
    type: str
    amount_cents: int
    timestamp: str
    status: str
    description: str
    from_name: str
    to_name: str


class AdminTransactionsResponse(BaseModel):
    transactions: List[AdminTransactionItem]


class UserActionRequest(BaseModel):
    user_id: str = Field(..., min_length=1)


class UserActionResponse(BaseModel):
    message: str
    user_id: str
    status: str


class CreditRequest(BaseModel):
    user_id: str = Field(..., min_length=1)
    amount_cents: int = Field(..., gt=0)
    description: str = Field("", max_length=200)


class CreditResponse(BaseModel):
    transaction_id: str
    amount_cents: int
    new_balance_cents: int
    currency: str
    to_name: str
    description: str


class TreasuryResponse(BaseModel):
    account_id: str
    balance_cents: int
    issued_cents: int
    currency: str


class LedgerResponse(BaseModel):
    user_balance_cents: int
    treasury_balance_cents: int
    total_cents: int
    balanced: bool
    account_count: int


class AdminActionItem(BaseModel):
    id: str
    type: str
    admin_id: str
    admin_name: str
    target_user: str
    target_name: str
    amount_cents: int
    timestamp: str


class AdminActionsResponse(BaseModel):
    actions: List[AdminActionItem]


# ── Loan (scaffold — Phase 5) ────────────────────────────────────

class LoanApplicationRequest(BaseModel):
    loan_type: str = Field(..., pattern=r"^(personal|business)$")
    amount_cents: int = Field(..., gt=0)
    term_months: int = Field(..., ge=3, le=360)
    purpose: str = Field("", max_length=500)


class LoanApplicationResponse(BaseModel):
    application_id: str
    status: str
    message: str


# ── Premium ──────────────────────────────────────────────────────

PREMIUM_PRICE_CENTS = 100_000  # $1000.00 / €1000.00


class PremiumPurchaseResponse(BaseModel):
    message: str
    transaction_id: str
    amount_cents: int
    new_balance_cents: int
    currency: str
    is_premium: bool
    premium_since: str


class PremiumStatusResponse(BaseModel):
    is_premium: bool
    premium_since: Optional[str] = None
    is_premium_concierge: bool = False
    price_cents: int = PREMIUM_PRICE_CENTS


# ── Spending analytics ───────────────────────────────────────────

class CategorySpend(BaseModel):
    category: str
    total_cents: int
    transaction_count: int


class MonthlySpend(BaseModel):
    month: str  # "2026-07"
    total_cents: int
    transaction_count: int


class SpendingAnalyticsResponse(BaseModel):
    by_category: List[CategorySpend]
    by_month: List[MonthlySpend]
    total_spent_cents: int
    total_transactions: int


# ── Savings goals ────────────────────────────────────────────────

class SavingsGoalCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    target_amount_cents: int = Field(..., gt=0)
    current_amount_cents: int = Field(0, ge=0)
    deadline: Optional[str] = None


class SavingsGoalUpdateRequest(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    target_amount_cents: Optional[int] = Field(None, gt=0)
    current_amount_cents: Optional[int] = Field(None, ge=0)
    deadline: Optional[str] = None
    # Adds this many cents to current_amount_cents (contribute towards goal).
    contribute_cents: Optional[int] = Field(None, gt=0)


class SavingsGoalItem(BaseModel):
    id: str
    name: str
    target_amount_cents: int
    current_amount_cents: int
    deadline: Optional[str] = None
    created_at: str
    progress_pct: float


class SavingsGoalListResponse(BaseModel):
    goals: List[SavingsGoalItem]


# ── Virtual cards ────────────────────────────────────────────────

class VirtualCardCreateRequest(BaseModel):
    label: str = Field("", max_length=60)
    spending_limit_cents: Optional[int] = Field(None, gt=0)


class VirtualCardUpdateRequest(BaseModel):
    frozen: Optional[bool] = None
    label: Optional[str] = Field(None, max_length=60)


class VirtualCardItem(BaseModel):
    id: str
    label: str
    card_number: str        # masked, e.g. "4291 •••• •••• 7183"
    card_number_full: Optional[str] = None  # only returned on creation
    cvv: Optional[str] = None               # only returned on creation
    expiry: str              # MM/YY
    frozen: bool
    spending_limit_cents: Optional[int] = None
    created_at: str


class VirtualCardListResponse(BaseModel):
    cards: List[VirtualCardItem]


# ── Cashback ─────────────────────────────────────────────────────

class CashbackTransactionItem(BaseModel):
    id: str
    source_transaction_id: str
    amount_cents: int
    rate_pct: float
    timestamp: str
    description: str


class CashbackResponse(BaseModel):
    total_cashback_cents: int
    rate_pct: float
    transactions: List[CashbackTransactionItem]


# ── Advanced 2FA / sessions ──────────────────────────────────────

class SessionItem(BaseModel):
    id: str
    device: str
    location: str
    ip_address: str
    last_active: str
    current: bool = False


class Advanced2FAResponse(BaseModel):
    two_fa_enabled: bool
    method: str
    sessions: List[SessionItem]
    trusted_devices: List[str]
