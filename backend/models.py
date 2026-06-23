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


class TwoFARequest(BaseModel):
    pending_token: str
    code: str = Field(..., min_length=6, max_length=6)


class ResendOTPRequest(BaseModel):
    pending_token: str


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
    access_token: Optional[str] = None
    token_type: str = "bearer"
    role: Optional[str] = None
    requires_2fa: bool = False
    pending_token: Optional[str] = None


class TwoFAResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: str = "admin"


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
