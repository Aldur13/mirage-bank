from datetime import datetime, timezone

from neo4j import GraphDatabase
from config import settings

TREASURY_ACCOUNT_ID = "TREASURY_ACCOUNT"

_driver = None


def get_driver():
    global _driver
    if _driver is None:
        _driver = GraphDatabase.driver(
            settings.neo4j_uri,
            auth=(settings.neo4j_user, settings.neo4j_password),
        )
    return _driver


def close_driver():
    global _driver
    if _driver is not None:
        _driver.close()
        _driver = None


def get_session():
    return get_driver().session(database=settings.neo4j_database)


def setup_constraints():
    with get_session() as session:
        constraints = [
            ("user_email_unique",   "FOR (u:User) REQUIRE u.email IS UNIQUE"),
            ("user_id_unique",      "FOR (u:User) REQUIRE u.id IS UNIQUE"),
            ("account_id_unique",   "FOR (a:Account) REQUIRE a.id IS UNIQUE"),
            ("transaction_id_unique","FOR (t:Transaction) REQUIRE t.id IS UNIQUE"),
            ("admin_action_id_unique","FOR (aa:AdminAction) REQUIRE aa.id IS UNIQUE"),
            ("otp_id_unique",       "FOR (o:OTPCode) REQUIRE o.id IS UNIQUE"),
            ("ticket_id_unique",    "FOR (tk:Ticket) REQUIRE tk.id IS UNIQUE"),
            ("ticket_msg_id_unique","FOR (tm:TicketMessage) REQUIRE tm.id IS UNIQUE"),
            ("business_org_id_unique","FOR (bo:BusinessOrg) REQUIRE bo.id IS UNIQUE"),
            ("loan_app_id_unique",  "FOR (la:LoanApplication) REQUIRE la.id IS UNIQUE"),
        ]
        for name, rule in constraints:
            session.run(f"CREATE CONSTRAINT {name} IF NOT EXISTS {rule}")

        indexes = [
            "FOR (u:User) ON (u.email)",
            "FOR (tk:Ticket) ON (tk.status)",
        ]
        for idx_body in indexes:
            session.run(f"CREATE INDEX IF NOT EXISTS {idx_body}")


def setup_treasury():
    """Idempotently ensure the Treasury system account exists."""
    now = datetime.now(timezone.utc).isoformat()
    with get_session() as session:
        session.run(
            """
            MERGE (a:Account {id: $treasury_id})
            ON CREATE SET a.balance_cents = 0,
                          a.currency = 'EUR',
                          a.status = 'active',
                          a.is_system = true,
                          a.created_at = $now
            """,
            treasury_id=TREASURY_ACCOUNT_ID,
            now=now,
        )
