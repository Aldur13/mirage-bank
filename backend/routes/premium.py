import random
import string
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status

from database import TREASURY_ACCOUNT_ID, get_session
from dependencies import get_current_user
from models import (
    PREMIUM_PRICE_CENTS,
    PremiumPurchaseResponse, PremiumStatusResponse,
    CategorySpend, MonthlySpend, SpendingAnalyticsResponse,
    SavingsGoalCreateRequest, SavingsGoalUpdateRequest,
    SavingsGoalItem, SavingsGoalListResponse,
    VirtualCardCreateRequest, VirtualCardUpdateRequest,
    VirtualCardItem, VirtualCardListResponse,
    CashbackTransactionItem, CashbackResponse,
    SessionItem, Advanced2FAResponse,
)

router = APIRouter(prefix="/premium")

CASHBACK_RATE_PCT = 1.0  # 1% cashback on transfers for premium members

# Cypher snippet that resolves any user's account regardless of account_type.
# Returns `a` bound to the Account node. Mirrors routes/account.py.
_ACCOUNT_MATCH = """
    MATCH (u:User {id: $user_id})
    OPTIONAL MATCH (u)-[:OWNS]->(a1:Account)
    OPTIONAL MATCH (u)-[:MEMBER_OF]->(org:BusinessOrg)-[:HAS_ACCOUNT]->(a2:Account)
    WITH u, coalesce(a1, a2) AS a
    WHERE a IS NOT NULL
"""


def _require_premium(current_user: dict):
    if not current_user.get("is_premium"):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="This feature requires Mirage Premium. Upgrade to unlock it.",
        )


# ── Purchase & status ────────────────────────────────────────────

@router.post("/purchase", response_model=PremiumPurchaseResponse)
def purchase_premium(current_user: dict = Depends(get_current_user)):
    if current_user.get("is_premium"):
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Account is already Premium")

    transaction_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    with get_session() as session:
        result = session.run(
            f"""
            {_ACCOUNT_MATCH}
            MATCH (treasury:Account {{id: $treasury_id}})
            WHERE a.status = 'active' AND a.balance_cents >= $amount_cents
            SET a.balance_cents = a.balance_cents - $amount_cents,
                treasury.balance_cents = treasury.balance_cents + $amount_cents,
                u.is_premium = true,
                u.premium_since = $now
            CREATE (t:Transaction {{
                id: $transaction_id, type: 'premium_purchase', amount_cents: $amount_cents,
                timestamp: $now, status: 'completed', description: 'Mirage Premium upgrade'
            }})
            CREATE (a)-[:SENT]->(t)
            CREATE (t)-[:TO]->(treasury)
            RETURN a.balance_cents AS new_balance, a.currency AS currency
            """,
            user_id=current_user["id"], treasury_id=TREASURY_ACCOUNT_ID,
            amount_cents=PREMIUM_PRICE_CENTS, transaction_id=transaction_id, now=now,
        ).single()

    if result is None:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Insufficient funds or account is not active",
        )

    return PremiumPurchaseResponse(
        message="Welcome to Mirage Premium",
        transaction_id=transaction_id,
        amount_cents=PREMIUM_PRICE_CENTS,
        new_balance_cents=result["new_balance"],
        currency=result["currency"],
        is_premium=True,
        premium_since=now,
    )


@router.get("/status", response_model=PremiumStatusResponse)
def get_premium_status(current_user: dict = Depends(get_current_user)):
    return PremiumStatusResponse(
        is_premium=current_user.get("is_premium", False),
        premium_since=current_user.get("premium_since"),
        is_premium_concierge=current_user.get("is_premium_concierge", False),
        price_cents=PREMIUM_PRICE_CENTS,
    )


# ── Spending analytics ───────────────────────────────────────────

_CATEGORY_KEYWORDS = {
    "food":       ["restaurant", "cafe", "coffee", "food", "grocery", "groceries", "supermarket", "takeaway", "diner"],
    "transport":  ["uber", "taxi", "transport", "fuel", "gas", "petrol", "train", "flight", "airline", "parking"],
    "shopping":   ["amazon", "shop", "store", "mall", "retail", "purchase"],
    "utilities":  ["electric", "water", "utility", "utilities", "internet", "phone bill", "broadband"],
    "entertainment": ["cinema", "movie", "netflix", "spotify", "game", "concert", "streaming"],
    "health":     ["pharmacy", "doctor", "hospital", "clinic", "health", "medical"],
    "housing":    ["rent", "mortgage", "housing"],
    "transfer":   ["transfer to", "transfer from"],
    "withdrawal": ["withdrawal"],
}


def _categorize(description: str) -> str:
    desc = (description or "").lower()
    for category, keywords in _CATEGORY_KEYWORDS.items():
        if any(kw in desc for kw in keywords):
            return category
    return "other"


@router.get("/analytics/spending", response_model=SpendingAnalyticsResponse)
def get_spending_analytics(current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    with get_session() as session:
        records = session.run(
            f"""
            {_ACCOUNT_MATCH}
            MATCH (a)-[:SENT]->(t:Transaction)
            RETURN t.id AS id, t.amount_cents AS amount_cents,
                   t.timestamp AS timestamp, t.description AS description,
                   t.category AS category
            ORDER BY t.timestamp DESC
            """,
            user_id=current_user["id"],
        ).data()

    by_category: dict = {}
    by_month: dict = {}
    total_spent = 0

    for r in records:
        category = r.get("category") or _categorize(r.get("description", ""))
        amount = r["amount_cents"]
        total_spent += amount

        cat_bucket = by_category.setdefault(category, {"total_cents": 0, "transaction_count": 0})
        cat_bucket["total_cents"] += amount
        cat_bucket["transaction_count"] += 1

        month_key = (r["timestamp"] or "")[:7] or "unknown"
        month_bucket = by_month.setdefault(month_key, {"total_cents": 0, "transaction_count": 0})
        month_bucket["total_cents"] += amount
        month_bucket["transaction_count"] += 1

    return SpendingAnalyticsResponse(
        by_category=[
            CategorySpend(category=k, **v)
            for k, v in sorted(by_category.items(), key=lambda kv: -kv[1]["total_cents"])
        ],
        by_month=[
            MonthlySpend(month=k, **v)
            for k, v in sorted(by_month.items())
        ],
        total_spent_cents=total_spent,
        total_transactions=len(records),
    )


# ── Savings goals ────────────────────────────────────────────────

def _goal_item(r: dict) -> SavingsGoalItem:
    target = r["target_amount_cents"] or 1
    current = r["current_amount_cents"] or 0
    progress = min(100.0, round((current / target) * 100, 1)) if target else 0.0
    return SavingsGoalItem(
        id=r["id"], name=r["name"],
        target_amount_cents=r["target_amount_cents"],
        current_amount_cents=r["current_amount_cents"],
        deadline=r.get("deadline"), created_at=r["created_at"],
        progress_pct=progress,
    )


@router.post("/savings-goal", response_model=SavingsGoalItem, status_code=status.HTTP_201_CREATED)
def create_savings_goal(body: SavingsGoalCreateRequest, current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    goal_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    with get_session() as session:
        session.run(
            """
            MATCH (u:User {id: $user_id})
            CREATE (sg:SavingsGoal {
                id: $goal_id, name: $name,
                target_amount_cents: $target_amount_cents,
                current_amount_cents: $current_amount_cents,
                deadline: $deadline, created_at: $now
            })
            CREATE (u)-[:HAS_GOAL]->(sg)
            """,
            user_id=current_user["id"], goal_id=goal_id, name=body.name.strip(),
            target_amount_cents=body.target_amount_cents,
            current_amount_cents=body.current_amount_cents,
            deadline=body.deadline, now=now,
        )

    return _goal_item({
        "id": goal_id, "name": body.name.strip(),
        "target_amount_cents": body.target_amount_cents,
        "current_amount_cents": body.current_amount_cents,
        "deadline": body.deadline, "created_at": now,
    })


@router.get("/savings-goal", response_model=SavingsGoalListResponse)
def list_savings_goals(current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    with get_session() as session:
        records = session.run(
            """
            MATCH (u:User {id: $user_id})-[:HAS_GOAL]->(sg:SavingsGoal)
            RETURN sg.id AS id, sg.name AS name,
                   sg.target_amount_cents AS target_amount_cents,
                   sg.current_amount_cents AS current_amount_cents,
                   sg.deadline AS deadline, sg.created_at AS created_at
            ORDER BY sg.created_at DESC
            """,
            user_id=current_user["id"],
        ).data()

    return SavingsGoalListResponse(goals=[_goal_item(r) for r in records])


@router.patch("/savings-goal/{goal_id}", response_model=SavingsGoalItem)
def update_savings_goal(goal_id: str, body: SavingsGoalUpdateRequest, current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    with get_session() as session:
        existing = session.run(
            """
            MATCH (u:User {id: $user_id})-[:HAS_GOAL]->(sg:SavingsGoal {id: $goal_id})
            RETURN sg.current_amount_cents AS current_amount_cents
            """,
            user_id=current_user["id"], goal_id=goal_id,
        ).single()
        if existing is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Savings goal not found")

        updates = {}
        if body.name is not None:
            updates["name"] = body.name.strip()
        if body.target_amount_cents is not None:
            updates["target_amount_cents"] = body.target_amount_cents
        if body.deadline is not None:
            updates["deadline"] = body.deadline

        if body.contribute_cents is not None:
            updates["current_amount_cents"] = existing["current_amount_cents"] + body.contribute_cents
        elif body.current_amount_cents is not None:
            updates["current_amount_cents"] = body.current_amount_cents

        if not updates:
            raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Nothing to update")

        set_clauses = ", ".join(f"sg.{k} = ${k}" for k in updates)
        result = session.run(
            f"""
            MATCH (u:User {{id: $user_id}})-[:HAS_GOAL]->(sg:SavingsGoal {{id: $goal_id}})
            SET {set_clauses}
            RETURN sg.id AS id, sg.name AS name,
                   sg.target_amount_cents AS target_amount_cents,
                   sg.current_amount_cents AS current_amount_cents,
                   sg.deadline AS deadline, sg.created_at AS created_at
            """,
            user_id=current_user["id"], goal_id=goal_id, **updates,
        ).single()

    return _goal_item(dict(result))


# ── Virtual cards ────────────────────────────────────────────────

def _generate_card_number() -> str:
    # Fake but properly formatted 16-digit Visa-style number (starts with 4).
    digits = "4" + "".join(random.choices(string.digits, k=15))
    return digits


def _mask_card_number(card_number: str) -> str:
    return f"{card_number[:4]} •••• •••• {card_number[-4:]}"


def _generate_cvv() -> str:
    return "".join(random.choices(string.digits, k=3))


def _generate_expiry() -> str:
    now = datetime.now(timezone.utc)
    year = (now.year + 3) % 100
    return f"{now.month:02d}/{year:02d}"


def _card_item(r: dict, reveal: bool = False) -> VirtualCardItem:
    return VirtualCardItem(
        id=r["id"], label=r.get("label") or "",
        card_number=_mask_card_number(r["card_number"]),
        card_number_full=r["card_number"] if reveal else None,
        cvv=r["cvv"] if reveal else None,
        expiry=r["expiry"], frozen=r["frozen"],
        spending_limit_cents=r.get("spending_limit_cents"),
        created_at=r["created_at"],
    )


@router.post("/virtual-card", response_model=VirtualCardItem, status_code=status.HTTP_201_CREATED)
def create_virtual_card(body: VirtualCardCreateRequest, current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    with get_session() as session:
        existing_count = session.run(
            "MATCH (u:User {id: $user_id})-[:OWNS_CARD]->(vc:VirtualCard) RETURN count(vc) AS n",
            user_id=current_user["id"],
        ).single()["n"]
        if existing_count >= 5:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="Maximum of 5 virtual cards reached",
            )

        card_id = str(uuid.uuid4())
        card_number = _generate_card_number()
        cvv = _generate_cvv()
        expiry = _generate_expiry()
        now = datetime.now(timezone.utc).isoformat()

        session.run(
            """
            MATCH (u:User {id: $user_id})
            CREATE (vc:VirtualCard {
                id: $card_id, label: $label, card_number: $card_number,
                cvv: $cvv, expiry: $expiry, frozen: false,
                spending_limit_cents: $spending_limit_cents, created_at: $now
            })
            CREATE (u)-[:OWNS_CARD]->(vc)
            """,
            user_id=current_user["id"], card_id=card_id, label=body.label.strip(),
            card_number=card_number, cvv=cvv, expiry=expiry,
            spending_limit_cents=body.spending_limit_cents, now=now,
        )

    return _card_item({
        "id": card_id, "label": body.label.strip(), "card_number": card_number,
        "cvv": cvv, "expiry": expiry, "frozen": False,
        "spending_limit_cents": body.spending_limit_cents, "created_at": now,
    }, reveal=True)


@router.get("/virtual-card", response_model=VirtualCardListResponse)
def list_virtual_cards(current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    with get_session() as session:
        records = session.run(
            """
            MATCH (u:User {id: $user_id})-[:OWNS_CARD]->(vc:VirtualCard)
            RETURN vc.id AS id, vc.label AS label, vc.card_number AS card_number,
                   vc.cvv AS cvv, vc.expiry AS expiry, vc.frozen AS frozen,
                   vc.spending_limit_cents AS spending_limit_cents, vc.created_at AS created_at
            ORDER BY vc.created_at DESC
            """,
            user_id=current_user["id"],
        ).data()

    return VirtualCardListResponse(cards=[_card_item(r) for r in records])


@router.patch("/virtual-card/{card_id}", response_model=VirtualCardItem)
def update_virtual_card(card_id: str, body: VirtualCardUpdateRequest, current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    updates = {}
    if body.frozen is not None:
        updates["frozen"] = body.frozen
    if body.label is not None:
        updates["label"] = body.label.strip()
    if not updates:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Nothing to update")

    set_clauses = ", ".join(f"vc.{k} = ${k}" for k in updates)
    with get_session() as session:
        result = session.run(
            f"""
            MATCH (u:User {{id: $user_id}})-[:OWNS_CARD]->(vc:VirtualCard {{id: $card_id}})
            SET {set_clauses}
            RETURN vc.id AS id, vc.label AS label, vc.card_number AS card_number,
                   vc.cvv AS cvv, vc.expiry AS expiry, vc.frozen AS frozen,
                   vc.spending_limit_cents AS spending_limit_cents, vc.created_at AS created_at
            """,
            user_id=current_user["id"], card_id=card_id, **updates,
        ).single()

    if result is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Virtual card not found")

    return _card_item(dict(result))


@router.delete("/virtual-card/{card_id}")
def delete_virtual_card(card_id: str, current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    with get_session() as session:
        result = session.run(
            """
            MATCH (u:User {id: $user_id})-[:OWNS_CARD]->(vc:VirtualCard {id: $card_id})
            DETACH DELETE vc
            RETURN vc.id AS id
            """,
            user_id=current_user["id"], card_id=card_id,
        ).single()

    if result is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Virtual card not found")

    return {"message": "Virtual card deleted", "card_id": card_id}


# ── Cashback ─────────────────────────────────────────────────────

@router.get("/cashback", response_model=CashbackResponse)
def get_cashback(current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    with get_session() as session:
        records = session.run(
            """
            MATCH (u:User {id: $user_id})-[:EARNED]->(cb:CashbackTransaction)
            RETURN cb.id AS id, cb.source_transaction_id AS source_transaction_id,
                   cb.amount_cents AS amount_cents, cb.rate_pct AS rate_pct,
                   cb.timestamp AS timestamp, cb.description AS description
            ORDER BY cb.timestamp DESC
            """,
            user_id=current_user["id"],
        ).data()

    total = sum(r["amount_cents"] for r in records)

    return CashbackResponse(
        total_cashback_cents=total,
        rate_pct=CASHBACK_RATE_PCT,
        transactions=[CashbackTransactionItem(**r) for r in records],
    )


def award_cashback(session, user_id: str, source_transaction_id: str, transfer_amount_cents: int):
    """Award 1% cashback to a premium user for a completed transfer.

    Called from routes/account.py after a successful transfer when the
    sender is a premium member. Cashback accrues to a separate running
    balance (CashbackTransaction nodes) — it is not credited to the main
    account automatically.
    """
    cashback_cents = round(transfer_amount_cents * (CASHBACK_RATE_PCT / 100))
    if cashback_cents <= 0:
        return

    session.run(
        """
        MATCH (u:User {id: $user_id})
        CREATE (cb:CashbackTransaction {
            id: $id, source_transaction_id: $source_transaction_id,
            amount_cents: $amount_cents, rate_pct: $rate_pct,
            timestamp: $now, description: $description
        })
        CREATE (u)-[:EARNED]->(cb)
        """,
        user_id=user_id, id=str(uuid.uuid4()),
        source_transaction_id=source_transaction_id,
        amount_cents=cashback_cents, rate_pct=CASHBACK_RATE_PCT,
        now=datetime.now(timezone.utc).isoformat(),
        description=f"Cashback on transfer ({CASHBACK_RATE_PCT:g}%)",
    )


# ── Advanced 2FA / session controls ───────────────────────────────

@router.get("/advanced-2fa", response_model=Advanced2FAResponse)
def get_advanced_2fa(current_user: dict = Depends(get_current_user)):
    _require_premium(current_user)

    # Mock session data — no real 2FA hardware/session tracking backend yet.
    now = datetime.now(timezone.utc)
    sessions = [
        SessionItem(
            id=str(uuid.uuid4()), device="Chrome on Windows", location="Dublin, IE",
            ip_address="185.24.11." + str(random.randint(2, 250)),
            last_active=now.isoformat(), current=True,
        ),
        SessionItem(
            id=str(uuid.uuid4()), device="Mirage Bank iOS App", location="Dublin, IE",
            ip_address="185.24.11." + str(random.randint(2, 250)),
            last_active=now.isoformat(), current=False,
        ),
    ]

    return Advanced2FAResponse(
        two_fa_enabled=current_user.get("role") == "admin",
        method="authenticator_app" if current_user.get("role") == "admin" else "none",
        sessions=sessions,
        trusted_devices=["Chrome on Windows", "Mirage Bank iOS App"],
    )
