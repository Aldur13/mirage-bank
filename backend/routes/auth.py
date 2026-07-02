import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException, status

from auth import create_token, hash_password, verify_password
from database import get_session
from models import (
    LoginRequest, LoginResponse,
    RegisterRequest, RegisterResponse, UserResponse,
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

    token = create_token(user["id"])
    return LoginResponse(access_token=token, role=user["role"])
