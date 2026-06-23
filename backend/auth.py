import hashlib
import secrets
from datetime import datetime, timedelta, timezone

import jwt
from passlib.context import CryptContext

from config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


# ── Passwords ────────────────────────────────────────────────────
def hash_password(password: str) -> str:
    if len(password.encode("utf-8")) > 72:
        raise ValueError("Password too long (bcrypt limit is 72 bytes)")
    return pwd_context.hash(password)


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


# ── JWT tokens ───────────────────────────────────────────────────
def create_token(user_id: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(minutes=settings.jwt_expire_minutes)
    payload = {"sub": user_id, "exp": expire, "type": "access"}
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def create_pending_token(user_id: str) -> str:
    """Short-lived JWT for admin awaiting 2FA confirmation (10-minute TTL)."""
    expire = datetime.now(timezone.utc) + timedelta(minutes=10)
    payload = {"sub": user_id, "exp": expire, "type": "2fa_pending"}
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def decode_token(token: str) -> str:
    payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
    token_type = payload.get("type")
    # Accept None for legacy tokens created before the type claim was added.
    if token_type not in ("access", None):
        raise jwt.InvalidTokenError("Not an access token")
    user_id: str = payload.get("sub")
    if user_id is None:
        raise jwt.InvalidTokenError("Missing subject")
    return user_id


def decode_pending_token(token: str) -> str:
    """Decode a 2FA pending token; raises InvalidTokenError if wrong type."""
    payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
    if payload.get("type") != "2fa_pending":
        raise jwt.InvalidTokenError("Not a pending 2FA token")
    user_id: str = payload.get("sub")
    if user_id is None:
        raise jwt.InvalidTokenError("Missing subject")
    return user_id


# ── OTP ─────────────────────────────────────────────────────────
def generate_otp() -> str:
    """Return a 6-digit OTP string (100000–999999)."""
    return str(secrets.randbelow(900000) + 100000)


def hash_otp(code: str) -> str:
    return hashlib.sha256(code.encode()).hexdigest()
