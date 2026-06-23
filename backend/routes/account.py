import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status

from auth import hash_password, verify_password
from database import TREASURY_ACCOUNT_ID, get_session
from dependencies import get_current_user
from models import (
    MeResponse,
    BalanceResponse,
    WithdrawRequest, WithdrawResponse,
    TransferRequest, TransferResponse,
    TransactionItem, TransactionsResponse,
    ProfileUpdateRequest, ProfileUpdateResponse,
    PasswordChangeRequest, PasswordChangeResponse,
    GuardianWardResponse, GuardianActionResponse,
    BusinessInviteRequest, BusinessMembersResponse, BusinessMemberItem,
    BusinessRemoveMemberRequest,
)

router = APIRouter()

# Cypher snippet that resolves any user's account regardless of account_type.
# Returns `a` bound to the Account node.
_ACCOUNT_MATCH = """
    MATCH (u:User {id: $user_id})
    OPTIONAL MATCH (u)-[:OWNS]->(a1:Account)
    OPTIONAL MATCH (u)-[:MEMBER_OF]->(org:BusinessOrg)-[:HAS_ACCOUNT]->(a2:Account)
    WITH u, coalesce(a1, a2) AS a
    WHERE a IS NOT NULL
"""


# ── Profile ──────────────────────────────────────────────────────

@router.get("/me", response_model=MeResponse)
def get_me(current_user: dict = Depends(get_current_user)):
    return MeResponse(
        id=current_user["id"],
        name=current_user["name"],
        email=current_user["email"],
        role=current_user.get("role", "user"),
        status=current_user["status"],
        account_type=current_user.get("account_type", "personal"),
        theme=current_user.get("theme", "dark"),
        avatar_data=current_user.get("avatar_data"),
        company_name=current_user.get("company_name"),
        company_reg=current_user.get("company_reg"),
        guardian_id=current_user.get("guardian_id"),
    )


@router.patch("/profile", response_model=ProfileUpdateResponse)
def update_profile(body: ProfileUpdateRequest, current_user: dict = Depends(get_current_user)):
    updates = {}
    if body.name is not None:
        updates["name"] = body.name.strip()
    if body.avatar_data is not None:
        # Empty string clears the avatar
        updates["avatar_data"] = body.avatar_data if body.avatar_data else None
    if body.theme is not None:
        updates["theme"] = body.theme

    if not updates:
        return ProfileUpdateResponse(message="Nothing to update")

    set_clauses = ", ".join(f"u.{k} = ${k}" for k in updates)
    with get_session() as session:
        session.run(
            f"MATCH (u:User {{id: $user_id}}) SET {set_clauses}",
            user_id=current_user["id"], **updates,
        )
    return ProfileUpdateResponse(message="Profile updated")


@router.post("/profile/password", response_model=PasswordChangeResponse)
def change_password(body: PasswordChangeRequest, current_user: dict = Depends(get_current_user)):
    if not verify_password(body.old_password, current_user["password_hash"]):
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Current password is incorrect")

    new_hash = hash_password(body.new_password)
    with get_session() as session:
        session.run(
            "MATCH (u:User {id: $user_id}) SET u.password_hash = $new_hash",
            user_id=current_user["id"], new_hash=new_hash,
        )
    return PasswordChangeResponse(message="Password changed successfully")


# ── Balance & Transactions ───────────────────────────────────────

@router.get("/balance", response_model=BalanceResponse)
def get_balance(current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        result = session.run(
            f"""
            {_ACCOUNT_MATCH}
            RETURN a.id AS account_id, a.balance_cents AS balance_cents,
                   a.currency AS currency, a.status AS status,
                   a.account_type AS account_type
            """,
            user_id=current_user["id"],
        ).single()

    if result is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Account not found")

    return BalanceResponse(
        account_id=result["account_id"],
        balance_cents=result["balance_cents"],
        currency=result["currency"],
        status=result["status"],
        account_type=result.get("account_type") or current_user.get("account_type", "personal"),
    )


@router.post("/withdraw", response_model=WithdrawResponse)
def withdraw(body: WithdrawRequest, current_user: dict = Depends(get_current_user)):
    transaction_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    with get_session() as session:
        result = session.run(
            f"""
            {_ACCOUNT_MATCH}
            MATCH (treasury:Account {{id: $treasury_id}})
            WHERE a.status = 'active' AND a.balance_cents >= $amount_cents
            SET a.balance_cents = a.balance_cents - $amount_cents,
                treasury.balance_cents = treasury.balance_cents + $amount_cents
            CREATE (t:Transaction {{
                id: $transaction_id, type: 'withdrawal', amount_cents: $amount_cents,
                timestamp: $now, status: 'completed', description: 'Withdrawal'
            }})
            CREATE (a)-[:SENT]->(t)
            CREATE (t)-[:TO]->(treasury)
            RETURN a.balance_cents AS new_balance, a.currency AS currency
            """,
            user_id=current_user["id"], treasury_id=TREASURY_ACCOUNT_ID,
            amount_cents=body.amount_cents, transaction_id=transaction_id, now=now,
        ).single()

    if result is None:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                            detail="Insufficient funds or account is not active")

    return WithdrawResponse(
        transaction_id=transaction_id, amount_cents=body.amount_cents,
        new_balance_cents=result["new_balance"], currency=result["currency"],
    )


@router.post("/transfer", response_model=TransferResponse)
def transfer(body: TransferRequest, current_user: dict = Depends(get_current_user)):
    if body.to_email.lower() == current_user["email"]:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                            detail="Cannot transfer to your own account")

    transaction_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    with get_session() as session:
        result = session.run(
            f"""
            MATCH (sender:User {{id: $user_id}})
            OPTIONAL MATCH (sender)-[:OWNS]->(from_a1:Account)
            OPTIONAL MATCH (sender)-[:MEMBER_OF]->(org:BusinessOrg)-[:HAS_ACCOUNT]->(from_a2:Account)
            WITH sender, coalesce(from_a1, from_a2) AS from_a
            WHERE from_a IS NOT NULL
            MATCH (to_u:User {{email: $to_email}})
            OPTIONAL MATCH (to_u)-[:OWNS]->(to_a1:Account)
            OPTIONAL MATCH (to_u)-[:MEMBER_OF]->(to_org:BusinessOrg)-[:HAS_ACCOUNT]->(to_a2:Account)
            WITH sender, from_a, to_u, coalesce(to_a1, to_a2) AS to_a
            WHERE to_a IS NOT NULL
              AND from_a.status = 'active' AND to_a.status = 'active'
              AND from_a.balance_cents >= $amount_cents
            SET from_a.balance_cents = from_a.balance_cents - $amount_cents,
                to_a.balance_cents   = to_a.balance_cents   + $amount_cents
            CREATE (t:Transaction {{
                id: $transaction_id, type: 'transfer', amount_cents: $amount_cents,
                timestamp: $now, status: 'completed', description: 'Transfer'
            }})
            CREATE (from_a)-[:SENT]->(t)
            CREATE (t)-[:TO]->(to_a)
            RETURN from_a.balance_cents AS new_balance, from_a.currency AS currency, to_u.name AS to_name
            """,
            user_id=current_user["id"], to_email=body.to_email.lower(),
            amount_cents=body.amount_cents, transaction_id=transaction_id, now=now,
        ).single()

    if result is None:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                            detail="Insufficient funds, recipient not found, or an account is not active")

    return TransferResponse(
        transaction_id=transaction_id, amount_cents=body.amount_cents,
        new_balance_cents=result["new_balance"], currency=result["currency"],
        to_name=result["to_name"],
    )


@router.get("/transactions", response_model=TransactionsResponse)
def get_transactions(current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        records = session.run(
            """
            MATCH (u:User {id: $user_id})
            OPTIONAL MATCH (u)-[:OWNS]->(a1:Account)
            OPTIONAL MATCH (u)-[:MEMBER_OF]->(org:BusinessOrg)-[:HAS_ACCOUNT]->(a2:Account)
            WITH coalesce(a1, a2) AS a WHERE a IS NOT NULL
            MATCH (t:Transaction)
            WHERE (a)-[:SENT]->(t) OR ((t)-[:TO]->(a) AND NOT (a)-[:SENT]->(t))
            WITH a, t,
                 CASE WHEN (a)-[:SENT]->(t) THEN 'debit' ELSE 'credit' END AS direction
            OPTIONAL MATCH (from_a:Account)-[:SENT]->(t)-[:TO]->(to_a:Account)
                WHERE t.type = 'transfer'
            OPTIONAL MATCH (from_u:User)-[:OWNS]->(from_a)
            OPTIONAL MATCH (to_u:User)-[:OWNS]->(to_a)
            RETURN t.id AS id, t.type AS type, t.amount_cents AS amount_cents,
                   t.timestamp AS timestamp, t.status AS status,
                   CASE
                       WHEN t.type = 'transfer' AND direction = 'debit'  THEN 'Transfer to '   + to_u.name
                       WHEN t.type = 'transfer' AND direction = 'credit' THEN 'Transfer from ' + from_u.name
                       ELSE t.description
                   END AS description,
                   direction
            ORDER BY t.timestamp DESC
            LIMIT 100
            """,
            user_id=current_user["id"],
        ).data()

    return TransactionsResponse(
        transactions=[TransactionItem(**r) for r in records]
    )


# ── Guardian endpoints ───────────────────────────────────────────

def _get_ward(guardian_id: str, session):
    """Return ward user + account or None."""
    return session.run(
        """
        MATCH (g:User {id: $guardian_id})-[:GUARDS]->(w:User)-[:OWNS]->(a:Account)
        RETURN w.id AS ward_id, w.name AS ward_name, w.email AS ward_email,
               w.status AS ward_status, a.id AS account_id,
               a.balance_cents AS balance_cents, a.currency AS currency,
               a.status AS account_status
        """,
        guardian_id=guardian_id,
    ).single()


@router.get("/guardian/ward", response_model=GuardianWardResponse)
def guardian_get_ward(current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        result = _get_ward(current_user["id"], session)
    if result is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No linked ward account found")
    return GuardianWardResponse(**dict(result))


@router.get("/guardian/transactions", response_model=TransactionsResponse)
def guardian_get_transactions(current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        ward = _get_ward(current_user["id"], session)
        if ward is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No linked ward account found")

        records = session.run(
            """
            MATCH (w:User {id: $ward_id})-[:OWNS]->(a:Account)
            MATCH (t:Transaction)
            WHERE (a)-[:SENT]->(t) OR ((t)-[:TO]->(a) AND NOT (a)-[:SENT]->(t))
            WITH a, t, CASE WHEN (a)-[:SENT]->(t) THEN 'debit' ELSE 'credit' END AS direction
            OPTIONAL MATCH (from_a:Account)-[:SENT]->(t)-[:TO]->(to_a:Account)
                WHERE t.type = 'transfer'
            OPTIONAL MATCH (from_u:User)-[:OWNS]->(from_a)
            OPTIONAL MATCH (to_u:User)-[:OWNS]->(to_a)
            RETURN t.id AS id, t.type AS type, t.amount_cents AS amount_cents,
                   t.timestamp AS timestamp, t.status AS status,
                   CASE
                       WHEN t.type = 'transfer' AND direction = 'debit'  THEN 'Transfer to '   + to_u.name
                       WHEN t.type = 'transfer' AND direction = 'credit' THEN 'Transfer from ' + from_u.name
                       ELSE t.description
                   END AS description, direction
            ORDER BY t.timestamp DESC LIMIT 100
            """,
            ward_id=ward["ward_id"],
        ).data()

    return TransactionsResponse(transactions=[TransactionItem(**r) for r in records])


def _guardian_set_status(guardian_id: str, new_status: str) -> dict:
    with get_session() as session:
        ward = _get_ward(guardian_id, session)
        if ward is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No linked ward account found")
        session.run(
            """
            MATCH (w:User {id: $ward_id})-[:OWNS]->(a:Account)
            SET w.status = $new_status, a.status = $new_status
            """,
            ward_id=ward["ward_id"], new_status=new_status,
        )
    return {"ward_id": ward["ward_id"], "status": new_status}


@router.post("/guardian/freeze", response_model=GuardianActionResponse)
def guardian_freeze(current_user: dict = Depends(get_current_user)):
    r = _guardian_set_status(current_user["id"], "frozen")
    return GuardianActionResponse(message="Ward account frozen", **r)


@router.post("/guardian/unfreeze", response_model=GuardianActionResponse)
def guardian_unfreeze(current_user: dict = Depends(get_current_user)):
    r = _guardian_set_status(current_user["id"], "active")
    return GuardianActionResponse(message="Ward account restored", **r)


# ── Business endpoints ───────────────────────────────────────────

def _require_business_owner(current_user: dict, session):
    """Return (org_id, company_name) if user is owner of a BusinessOrg, else raise 403."""
    result = session.run(
        """
        MATCH (u:User {id: $user_id})-[m:MEMBER_OF {role: 'owner'}]->(org:BusinessOrg)
        RETURN org.id AS org_id, org.company_name AS company_name
        """,
        user_id=current_user["id"],
    ).single()
    if result is None:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN,
                            detail="Business owner account required")
    return dict(result)


@router.get("/business/members", response_model=BusinessMembersResponse)
def business_members(current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        org = _require_business_owner(current_user, session)
        records = session.run(
            """
            MATCH (u:User)-[m:MEMBER_OF]->(org:BusinessOrg {id: $org_id})
            RETURN u.id AS user_id, u.name AS name, u.email AS email,
                   m.role AS role, m.joined_at AS joined_at
            ORDER BY m.role DESC, u.name ASC
            """,
            org_id=org["org_id"],
        ).data()
    return BusinessMembersResponse(
        org_id=org["org_id"],
        company_name=org["company_name"],
        members=[BusinessMemberItem(**r) for r in records],
    )


@router.post("/business/invite")
def business_invite(body: BusinessInviteRequest, current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        org = _require_business_owner(current_user, session)
        target = session.run(
            "MATCH (u:User {email: $email}) RETURN u.id AS id, u.name AS name",
            email=body.email.lower(),
        ).single()
        if target is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
        existing = session.run(
            "MATCH (u:User {id: $uid})-[:MEMBER_OF]->(org:BusinessOrg {id: $org_id}) RETURN u.id",
            uid=target["id"], org_id=org["org_id"],
        ).single()
        if existing:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="User is already a member")
        session.run(
            """
            MATCH (u:User {id: $uid}), (org:BusinessOrg {id: $org_id})
            CREATE (u)-[:MEMBER_OF {role: $role, joined_at: $now}]->(org)
            """,
            uid=target["id"], org_id=org["org_id"],
            role=body.role, now=datetime.now(timezone.utc).isoformat(),
        )
    return {"message": f"{target['name']} added as {body.role}"}


@router.delete("/business/members/{member_id}")
def business_remove_member(member_id: str, current_user: dict = Depends(get_current_user)):
    if member_id == current_user["id"]:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                            detail="Owner cannot remove themselves")
    with get_session() as session:
        org = _require_business_owner(current_user, session)
        result = session.run(
            """
            MATCH (u:User {id: $uid})-[m:MEMBER_OF]->(org:BusinessOrg {id: $org_id})
            WHERE m.role <> 'owner'
            DELETE m
            RETURN u.name AS name
            """,
            uid=member_id, org_id=org["org_id"],
        ).single()
        if result is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Member not found")
    return {"message": f"{result['name']} removed from business"}
