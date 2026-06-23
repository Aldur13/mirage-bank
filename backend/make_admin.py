"""Promote an existing registered user to the 'admin' role.

Usage (from the backend/ directory, with the venv active):

    python make_admin.py user@example.com

The user must already be registered. Run this once to bootstrap the first
administrator; thereafter admins are managed through the app.
"""
import sys

from database import close_driver, get_session


def make_admin(email: str) -> None:
    email = email.strip().lower()
    with get_session() as session:
        result = session.run(
            """
            MATCH (u:User {email: $email})
            SET u.role = 'admin'
            RETURN u.id AS id, u.name AS name, u.email AS email, u.role AS role
            """,
            email=email,
        ).single()

    if result is None:
        print(f"[ERROR] No user found with email '{email}'. Register them first.")
        sys.exit(1)

    print(f"[OK] {result['name']} <{result['email']}> is now an administrator "
          f"(role={result['role']}).")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python make_admin.py <email>")
        sys.exit(2)
    try:
        make_admin(sys.argv[1])
    finally:
        close_driver()
