import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status

from database import TREASURY_ACCOUNT_ID, get_session
from dependencies import get_current_admin
from models import (
    AdminUserItem, AdminUsersResponse,
    AdminTransactionItem, AdminTransactionsResponse,
    UserActionRequest, UserActionResponse,
    CreditRequest, CreditResponse,
    TreasuryResponse, LedgerResponse,
    AdminActionItem, AdminActionsResponse,
    AdminTicketItem, AdminTicketListResponse,
    AdminTicketUpdateRequest, TicketActionResponse,
    TicketDetail, TicketMessageItem, TicketMessageCreate,
)

router = APIRouter(prefix="/admin")


def _log_admin_action(session, action_type: str, admin: dict,
                      target_user_id: str, amount_cents: int = 0):
    session.run(
        """
        MATCH (admin:User {id: $admin_id})
        CREATE (aa:AdminAction {
            id: $id, type: $type, admin_id: $admin_id,
            target_user: $target_user, amount_cents: $amount_cents, timestamp: $now
        })
        CREATE (admin)-[:PERFORMED]->(aa)
        """,
        id=str(uuid.uuid4()), type=action_type, admin_id=admin["id"],
        target_user=target_user_id, amount_cents=amount_cents,
        now=datetime.now(timezone.utc).isoformat(),
    )


# ── Users ────────────────────────────────────────────────────────

@router.get("/users", response_model=AdminUsersResponse)
def list_users(_: dict = Depends(get_current_admin)):
    with get_session() as session:
        records = session.run(
            """
            MATCH (u:User)
            OPTIONAL MATCH (u)-[:OWNS]->(a1:Account)
            OPTIONAL MATCH (u)-[:MEMBER_OF]->(org:BusinessOrg)-[:HAS_ACCOUNT]->(a2:Account)
            WITH u, coalesce(a1, a2) AS a
            RETURN u.id AS id, u.name AS name, u.email AS email,
                   u.role AS role, u.status AS status,
                   coalesce(u.account_type, 'personal') AS account_type,
                   coalesce(a.balance_cents, 0) AS balance_cents,
                   coalesce(a.currency, 'EUR') AS currency
            ORDER BY u.created_at ASC
            """
        ).data()
    return AdminUsersResponse(users=[AdminUserItem(**r) for r in records])


# ── Transactions ─────────────────────────────────────────────────

@router.get("/transactions", response_model=AdminTransactionsResponse)
def list_transactions(_: dict = Depends(get_current_admin)):
    with get_session() as session:
        records = session.run(
            """
            MATCH (t:Transaction)
            OPTIONAL MATCH (from_a:Account)-[:SENT]->(t)
            OPTIONAL MATCH (t)-[:TO]->(to_a:Account)
            OPTIONAL MATCH (from_u:User)-[:OWNS]->(from_a)
            OPTIONAL MATCH (to_u:User)-[:OWNS]->(to_a)
            RETURN t.id AS id, t.type AS type, t.amount_cents AS amount_cents,
                   t.timestamp AS timestamp, t.status AS status, t.description AS description,
                   CASE
                       WHEN from_a.id = $treasury_id THEN 'Treasury'
                       WHEN from_u IS NOT NULL        THEN from_u.name
                       ELSE '—'
                   END AS from_name,
                   CASE
                       WHEN to_a.id = $treasury_id THEN 'Treasury'
                       WHEN to_u IS NOT NULL        THEN to_u.name
                       ELSE '—'
                   END AS to_name
            ORDER BY t.timestamp DESC LIMIT 500
            """,
            treasury_id=TREASURY_ACCOUNT_ID,
        ).data()
    return AdminTransactionsResponse(transactions=[AdminTransactionItem(**r) for r in records])


# ── User status management ───────────────────────────────────────

def _set_status(target_user_id: str, admin: dict, new_status: str, action_type: str) -> dict:
    if target_user_id == admin["id"]:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                            detail="You cannot change the status of your own account")
    with get_session() as session:
        target = session.run(
            "MATCH (u:User {id: $id}) RETURN u.role AS role",
            id=target_user_id,
        ).single()
        if target is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
        if target["role"] == "admin":
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN,
                                detail="Cannot change the status of another administrator")

        # Update both User and their account(s)
        session.run(
            """
            MATCH (u:User {id: $id})
            SET u.status = $new_status
            WITH u
            OPTIONAL MATCH (u)-[:OWNS]->(a1:Account)
            OPTIONAL MATCH (u)-[:MEMBER_OF]->(org:BusinessOrg)-[:HAS_ACCOUNT]->(a2:Account)
            FOREACH (a IN CASE WHEN a1 IS NOT NULL THEN [a1] ELSE [] END | SET a.status = $new_status)
            FOREACH (a IN CASE WHEN a2 IS NOT NULL THEN [a2] ELSE [] END | SET a.status = $new_status)
            """,
            id=target_user_id, new_status=new_status,
        )
        _log_admin_action(session, action_type, admin, target_user_id)
    return {"user_id": target_user_id, "status": new_status}


@router.post("/freeze", response_model=UserActionResponse)
def freeze_user(body: UserActionRequest, admin: dict = Depends(get_current_admin)):
    return UserActionResponse(message="User frozen", **_set_status(body.user_id, admin, "frozen", "freeze"))


@router.post("/unfreeze", response_model=UserActionResponse)
def unfreeze_user(body: UserActionRequest, admin: dict = Depends(get_current_admin)):
    return UserActionResponse(message="User restored", **_set_status(body.user_id, admin, "active", "unfreeze"))


@router.post("/disable", response_model=UserActionResponse)
def disable_user(body: UserActionRequest, admin: dict = Depends(get_current_admin)):
    return UserActionResponse(message="User disabled", **_set_status(body.user_id, admin, "disabled", "disable"))


# ── Credit ───────────────────────────────────────────────────────

@router.post("/credit", response_model=CreditResponse)
def credit_user(body: CreditRequest, admin: dict = Depends(get_current_admin)):
    description = body.description.strip() or "Admin Credit"
    transaction_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    with get_session() as session:
        result = session.run(
            """
            MATCH (treasury:Account {id: $treasury_id})
            MATCH (u:User {id: $user_id})
            OPTIONAL MATCH (u)-[:OWNS]->(a1:Account)
            OPTIONAL MATCH (u)-[:MEMBER_OF]->(org:BusinessOrg)-[:HAS_ACCOUNT]->(a2:Account)
            WITH treasury, u, coalesce(a1, a2) AS a
            WHERE a IS NOT NULL AND a.status = 'active'
            SET a.balance_cents = a.balance_cents + $amount_cents,
                treasury.balance_cents = treasury.balance_cents - $amount_cents
            CREATE (t:Transaction {
                id: $transaction_id, type: 'credit', amount_cents: $amount_cents,
                timestamp: $now, status: 'completed', description: $description
            })
            CREATE (treasury)-[:SENT]->(t)
            CREATE (t)-[:TO]->(a)
            RETURN a.balance_cents AS new_balance, a.currency AS currency, u.name AS to_name
            """,
            treasury_id=TREASURY_ACCOUNT_ID, user_id=body.user_id,
            amount_cents=body.amount_cents, transaction_id=transaction_id,
            now=now, description=description,
        ).single()

        if result is None:
            raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                                detail="Target user not found or account is not active")

        _log_admin_action(session, "credit", admin, body.user_id, body.amount_cents)

    return CreditResponse(
        transaction_id=transaction_id, amount_cents=body.amount_cents,
        new_balance_cents=result["new_balance"], currency=result["currency"],
        to_name=result["to_name"], description=description,
    )


# ── Treasury / Ledger ────────────────────────────────────────────

@router.get("/treasury", response_model=TreasuryResponse)
def get_treasury(_: dict = Depends(get_current_admin)):
    with get_session() as session:
        result = session.run(
            "MATCH (a:Account {id: $treasury_id}) RETURN a.id AS account_id, a.balance_cents AS balance_cents, a.currency AS currency",
            treasury_id=TREASURY_ACCOUNT_ID,
        ).single()
    if result is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Treasury account not found")
    return TreasuryResponse(
        account_id=result["account_id"], balance_cents=result["balance_cents"],
        issued_cents=-result["balance_cents"], currency=result["currency"],
    )


@router.get("/ledger", response_model=LedgerResponse)
def get_ledger(_: dict = Depends(get_current_admin)):
    with get_session() as session:
        result = session.run(
            """
            MATCH (a:Account)
            WITH
                sum(CASE WHEN a.id = $treasury_id THEN 0 ELSE a.balance_cents END) AS user_total,
                sum(CASE WHEN a.id = $treasury_id THEN a.balance_cents ELSE 0 END) AS treasury_total,
                count(a) AS account_count
            RETURN user_total, treasury_total, account_count
            """,
            treasury_id=TREASURY_ACCOUNT_ID,
        ).single()
    user_total = result["user_total"] or 0
    treasury_total = result["treasury_total"] or 0
    total = user_total + treasury_total
    return LedgerResponse(
        user_balance_cents=user_total, treasury_balance_cents=treasury_total,
        total_cents=total, balanced=(total == 0), account_count=result["account_count"],
    )


# ── Audit log ────────────────────────────────────────────────────

@router.get("/actions", response_model=AdminActionsResponse)
def list_actions(_: dict = Depends(get_current_admin)):
    with get_session() as session:
        records = session.run(
            """
            MATCH (admin:User)-[:PERFORMED]->(aa:AdminAction)
            OPTIONAL MATCH (target:User {id: aa.target_user})
            RETURN aa.id AS id, aa.type AS type,
                   aa.admin_id AS admin_id, admin.name AS admin_name,
                   aa.target_user AS target_user,
                   coalesce(target.name, '—') AS target_name,
                   aa.amount_cents AS amount_cents, aa.timestamp AS timestamp
            ORDER BY aa.timestamp DESC LIMIT 200
            """
        ).data()
    return AdminActionsResponse(actions=[AdminActionItem(**r) for r in records])


# ── Support ticket management ────────────────────────────────────

@router.get("/support/tickets", response_model=AdminTicketListResponse)
def admin_list_tickets(_: dict = Depends(get_current_admin)):
    with get_session() as session:
        records = session.run(
            """
            MATCH (u:User)-[:OWNS_TICKET]->(tk:Ticket)
            OPTIONAL MATCH (msg:TicketMessage)-[:IN]->(tk)
            WITH tk, u, count(msg) AS message_count
            RETURN tk.id AS id, tk.subject AS subject, tk.status AS status,
                   tk.priority AS priority, tk.created_at AS created_at,
                   tk.updated_at AS updated_at, message_count,
                   u.id AS user_id, u.name AS user_name, u.email AS user_email
            ORDER BY
                CASE tk.priority WHEN 'urgent' THEN 1 WHEN 'high' THEN 2 WHEN 'normal' THEN 3 ELSE 4 END ASC,
                tk.updated_at DESC
            """
        ).data()
    return AdminTicketListResponse(tickets=[AdminTicketItem(**r) for r in records])


@router.get("/support/tickets/{ticket_id}", response_model=TicketDetail)
def admin_get_ticket(ticket_id: str, _: dict = Depends(get_current_admin)):
    with get_session() as session:
        tk = session.run(
            """
            MATCH (tk:Ticket {id: $ticket_id})
            RETURN tk.id AS id, tk.subject AS subject, tk.status AS status,
                   tk.priority AS priority, tk.created_at AS created_at, tk.updated_at AS updated_at
            """,
            ticket_id=ticket_id,
        ).single()
        if tk is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found")

        msgs = session.run(
            """
            MATCH (msg:TicketMessage)-[:IN]->(tk:Ticket {id: $ticket_id})
            MATCH (author:User {id: msg.author_id})
            RETURN msg.id AS id, msg.content AS content, msg.author_id AS author_id,
                   author.name AS author_name, msg.is_staff AS is_staff, msg.created_at AS created_at
            ORDER BY msg.created_at ASC
            """,
            ticket_id=ticket_id,
        ).data()

    return TicketDetail(
        id=tk["id"], subject=tk["subject"], status=tk["status"],
        priority=tk["priority"], created_at=tk["created_at"], updated_at=tk["updated_at"],
        messages=[TicketMessageItem(**m) for m in msgs],
    )


@router.patch("/support/tickets/{ticket_id}", response_model=TicketActionResponse)
def admin_update_ticket(ticket_id: str, body: AdminTicketUpdateRequest,
                        admin: dict = Depends(get_current_admin)):
    now = datetime.now(timezone.utc).isoformat()
    updates = {}
    if body.status is not None:
        updates["tk.status"] = body.status
    if body.priority is not None:
        updates["tk.priority"] = body.priority
    if not updates:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Nothing to update")

    set_clause = ", ".join(f"{k} = ${k.replace('.', '_')}" for k in updates)
    params = {k.replace(".", "_"): v for k, v in updates.items()}
    params["ticket_id"] = ticket_id
    params["now"] = now

    with get_session() as session:
        result = session.run(
            f"MATCH (tk:Ticket {{id: $ticket_id}}) SET {set_clause}, tk.updated_at = $now "
            "RETURN tk.id AS id, tk.status AS status",
            **params,
        ).single()
        if result is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found")

    return TicketActionResponse(message="Ticket updated", ticket_id=result["id"], status=result["status"])


@router.post("/support/tickets/{ticket_id}/messages", response_model=TicketMessageItem,
             status_code=status.HTTP_201_CREATED)
def admin_reply_ticket(ticket_id: str, body: TicketMessageCreate,
                       admin: dict = Depends(get_current_admin)):
    msg_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    with get_session() as session:
        tk = session.run("MATCH (tk:Ticket {id: $ticket_id}) RETURN tk.id", ticket_id=ticket_id).single()
        if tk is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found")
        session.run(
            """
            MATCH (tk:Ticket {id: $ticket_id})
            CREATE (msg:TicketMessage {
                id: $msg_id, content: $content,
                author_id: $author_id, is_staff: true, created_at: $now
            })
            CREATE (msg)-[:IN]->(tk)
            SET tk.updated_at = $now, tk.status = 'in_progress'
            """,
            ticket_id=ticket_id, msg_id=msg_id, content=body.content,
            author_id=admin["id"], now=now,
        )

    return TicketMessageItem(
        id=msg_id, content=body.content, author_id=admin["id"],
        author_name=admin["name"], is_staff=True, created_at=now,
    )
