// Premium tier schema — constraints/indexes are also created idempotently
// by database.setup_constraints() at app startup. This file documents the
// schema for reference and manual environments.

CREATE CONSTRAINT savings_goal_id IF NOT EXISTS
FOR (sg:SavingsGoal)
REQUIRE sg.id IS UNIQUE;

CREATE CONSTRAINT virtual_card_id IF NOT EXISTS
FOR (vc:VirtualCard)
REQUIRE vc.id IS UNIQUE;

CREATE CONSTRAINT cashback_tx_id IF NOT EXISTS
FOR (cb:CashbackTransaction)
REQUIRE cb.id IS UNIQUE;

CREATE INDEX user_is_premium IF NOT EXISTS
FOR (u:User)
ON (u.is_premium);

CREATE INDEX transaction_category IF NOT EXISTS
FOR (t:Transaction)
ON (t.category);

// Backfill defaults on existing users
MATCH (u:User)
SET u.is_premium = coalesce(u.is_premium, false),
    u.is_premium_concierge = coalesce(u.is_premium_concierge, false);

// Schema reference (properties, not enforced by Neo4j — informational only):
//
// User:
//   is_premium: boolean
//   premium_since: ISO datetime string
//   is_premium_concierge: boolean
//
// (:User)-[:OWNS]->(:VirtualCard { id, label, card_number, cvv, expiry,
//   frozen, spending_limit_cents, created_at })
//
// (:User)-[:HAS_GOAL]->(:SavingsGoal { id, name, target_amount_cents,
//   current_amount_cents, deadline, created_at })
//
// (:Account)-[:SENT]->(:Transaction { category }) — category is an optional
//   property on existing Transaction nodes (food, transport, shopping, ...).
//
// (:User)-[:EARNED]->(:CashbackTransaction { id, source_transaction_id,
//   amount_cents, rate_pct, timestamp, description })
