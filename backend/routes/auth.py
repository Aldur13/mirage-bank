import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException, status

from auth import (
    create_pending_token, create_token,
    decode_pending_token,
    generate_otp, hash_otp, hash_password, verify_password,
)
from database import get_session
from email_service import send_admin_otp
from models import (
    LoginRequest, LoginResponse,
    RegisterRequest, RegisterResponse, UserResponse,
    TwoFARequest, TwoFAResponse,
    ResendOTPRequest,
)

router = APIRouter()


@router.post("/register", response_model=RegisterResponse, status_code=status.HTTP_201_CREATED)
def register(body: RegisterRequest):
    email = body.email.lower()

    # Validate account-type-specific fields
    if body.account_type == "youth" and not body.guardian_email:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Guardian email is required for youth accounts",
        )
    if body.account_type == "business" and not body.company_name:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Company name is required for business accounts",
        )

    with get_session() as session:
        existing = session.run(
            "MATCH (u:User {email: $email}) RETURN u.id", email=email
        ).single()
        if existing is not None:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="An account with this email already exists",
            )

        # For youth: look up guardian
        guardian = None
        if body.account_type == "youth":
            g = session.run(
                "MATCH (u:User {email: $email}) RETURN u.id AS id, u.role AS role",
                email=body.guardian_email.lower(),
            ).single()
            if g is None:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Guardian email not found — guardian must have a Mirage account",
                )
            guardian = dict(g)

    user_id = str(uuid.uuid4())
    account_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()
    password_hash = hash_password(body.password)

    with get_session() as session:
        if body.account_type == "business":
            org_id = str(uuid.uuid4())
            session.run(
                """
                CREATE (u:User {
                    id: $user_id, name: $name, email: $email,
                    password_hash: $password_hash, role: 'user',
                    status: 'active', account_type: 'business',
                    company_name: $company_name, company_reg: $company_reg,
                    created_at: $now
                })
                CREATE (org:BusinessOrg {
                    id: $org_id, company_name: $company_name,
                    company_reg: $company_reg, created_at: $now
                })
                CREATE (a:Account {
                    id: $account_id, balance_cents: 0, currency: 'EUR',
                    status: 'active', account_type: 'business', created_at: $now
                })
                CREATE (u)-[:MEMBER_OF {role: 'owner', joined_at: $now}]->(org)
                CREATE (org)-[:HAS_ACCOUNT]->(a)
                """,
                user_id=user_id, name=body.name.strip(), email=email,
                password_hash=password_hash,
                company_name=(body.company_name or "").strip(),
                company_reg=(body.company_reg or "").strip(),
                org_id=org_id, account_id=account_id, now=now,
            )
        elif body.account_type == "youth":
            session.run(
                """
                CREATE (u:User {
                    id: $user_id, name: $name, email: $email,
                    password_hash: $password_hash, role: 'user',
                    status: 'active', account_type: 'youth',
                    guardian_id: $guardian_id, created_at: $now
                })
                CREATE (a:Account {
                    id: $account_id, balance_cents: 0, currency: 'EUR',
                    status: 'active', account_type: 'youth', created_at: $now
                })
                CREATE (u)-[:OWNS]->(a)
                WITH u
                MATCH (g:User {id: $guardian_id})
                CREATE (g)-[:GUARDS]->(u)
                """,
                user_id=user_id, name=body.name.strip(), email=email,
                password_hash=password_hash,
                guardian_id=guardian["id"],
                account_id=account_id, now=now,
            )
        else:
            session.run(
                """
                CREATE (u:User {
                    id: $user_id, name: $name, email: $email,
                    password_hash: $password_hash, role: 'user',
                    status: 'active', account_type: 'personal', created_at: $now
                })
                CREATE (a:Account {
                    id: $account_id, balance_cents: 0, currency: 'EUR',
                    status: 'active', account_type: 'personal', created_at: $now
                })
                CREATE (u)-[:OWNS]->(a)
                """,
                user_id=user_id, name=body.name.strip(), email=email,
                password_hash=password_hash, account_id=account_id, now=now,
            )

    return RegisterResponse(
        message="Registration successful",
        user=UserResponse(
            id=user_id, name=body.name.strip(), email=email,
            role="user", status="active",
        ),
    )


@router.post("/login", response_model=LoginResponse)
def login(body: LoginRequest):
    with get_session() as session:
        result = session.run(
            "MATCH (u:User {email: $email}) RETURN u", email=body.email.lower()
        ).single()

    if result is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid email or password")

    user = dict(result["u"])

    if not verify_password(body.password, user["password_hash"]):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid email or password")

    if user["status"] == "disabled":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Account has been disabled")
    if user["status"] == "frozen":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Account is frozen. Please contact support.")

    # Admin: require 2FA before issuing full token
    if user.get("role") == "admin":
        pending_token = create_pending_token(user["id"])
        _create_otp(user["id"], user["email"])
        return LoginResponse(requires_2fa=True, pending_token=pending_token)

    token = create_token(user["id"])
    return LoginResponse(access_token=token, role=user.get("role", "user"))


@router.post("/auth/2fa", response_model=TwoFAResponse)
def verify_2fa(body: TwoFARequest):
    import jwt as pyjwt
    try:
        user_id = decode_pending_token(body.pending_token)
    except pyjwt.PyJWTError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired verification session")

    now_iso = datetime.now(timezone.utc).isoformat()
    code_hash = hash_otp(body.code)

    with get_session() as session:
        result = session.run(
            """
            MATCH (u:User {id: $user_id})-[:HAS_OTP]->(o:OTPCode)
            WHERE o.used = false AND o.expires_at > $now AND o.code_hash = $code_hash
            SET o.used = true
            RETURN u.role AS role, u.status AS status
            """,
            user_id=user_id, now=now_iso, code_hash=code_hash,
        ).single()

    if result is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired verification code")

    if result["status"] == "disabled":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Account has been disabled")

    token = create_token(user_id)
    return TwoFAResponse(access_token=token, role="admin")


@router.post("/auth/resend-2fa")
def resend_2fa(body: ResendOTPRequest):
    import jwt as pyjwt
    try:
        user_id = decode_pending_token(body.pending_token)
    except pyjwt.PyJWTError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired verification session")

    with get_session() as session:
        result = session.run(
            "MATCH (u:User {id: $user_id}) RETURN u.email AS email, u.role AS role",
            user_id=user_id,
        ).single()

    if result is None or result["role"] != "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorised")

    _create_otp(user_id, result["email"])
    return {"message": "A new code has been sent to your email"}


# ── Helpers ──────────────────────────────────────────────────────

def _create_otp(user_id: str, email: str) -> None:
    """Generate OTP, store in DB, send email."""
    from datetime import timedelta
    code = generate_otp()
    code_hash = hash_otp(code)
    otp_id = str(uuid.uuid4())
    expires_at = (datetime.now(timezone.utc) + timedelta(minutes=10)).isoformat()

    with get_session() as session:
        # Invalidate any existing unused OTPs for this user
        session.run(
            """
            MATCH (u:User {id: $user_id})-[:HAS_OTP]->(o:OTPCode)
            WHERE o.used = false
            SET o.used = true
            """,
            user_id=user_id,
        )
        session.run(
            """
            MATCH (u:User {id: $user_id})
            CREATE (o:OTPCode {
                id: $otp_id, code_hash: $code_hash,
                expires_at: $expires_at, used: false,
                created_at: $now
            })
            CREATE (u)-[:HAS_OTP]->(o)
            """,
            user_id=user_id, otp_id=otp_id, code_hash=code_hash,
            expires_at=expires_at, now=datetime.now(timezone.utc).isoformat(),
        )

    send_admin_otp(email, code)
