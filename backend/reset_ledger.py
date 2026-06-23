"""Reset the ledger to a clean canonical state (Phase 4).

Destroys ALL transaction history, zeroes every user account balance and the
treasury, and removes the admin audit log. User and Account nodes (and their
OWNS relationships, credentials, roles, statuses) are preserved.

After running, the closed-loop invariant holds:
    sum(user balances) + treasury balance == 0

Usage (from backend/, venv active):
    python reset_ledger.py            # prompts for confirmation
    python reset_ledger.py --yes      # skip confirmation
"""
import sys

from database import close_driver, get_session, TREASURY_ACCOUNT_ID


def reset(skip_confirm: bool = False) -> None:
    if not skip_confirm:
        print("This will DELETE all transactions, admin actions, and zero all "
              "balances.\nUser accounts and credentials are preserved.")
        if input("Type 'RESET' to continue: ").strip() != "RESET":
            print("Aborted.")
            return

    with get_session() as session:
        tx = session.run("MATCH (t:Transaction) DETACH DELETE t RETURN count(t) AS c").single()["c"]
        aa = session.run("MATCH (a:AdminAction) DETACH DELETE a RETURN count(a) AS c").single()["c"]
        accts = session.run(
            "MATCH (a:Account) SET a.balance_cents = 0 RETURN count(a) AS c"
        ).single()["c"]

        # Verify the invariant
        check = session.run(
            """
            MATCH (a:Account)
            WITH sum(CASE WHEN a.id = $tid THEN 0 ELSE a.balance_cents END) AS users,
                 sum(CASE WHEN a.id = $tid THEN a.balance_cents ELSE 0 END) AS treasury
            RETURN users, treasury, users + treasury AS total
            """,
            tid=TREASURY_ACCOUNT_ID,
        ).single()

    print(f"Deleted {tx} transactions, {aa} admin actions.")
    print(f"Zeroed {accts} account balances.")
    print(f"Invariant: user_total={check['users']} + treasury={check['treasury']} "
          f"= {check['total']} (balanced={check['total'] == 0})")


if __name__ == "__main__":
    try:
        reset(skip_confirm="--yes" in sys.argv)
    finally:
        close_driver()
