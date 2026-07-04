import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from auth import decode_token
from config import settings
from database import get_session

security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> dict:
    token = credentials.credentials

    try:
        user_id = decode_token(token)
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    with get_session() as session:
        result = session.run(
            "MATCH (u:User {id: $user_id}) RETURN u",
            user_id=user_id,
        ).single()

    if result is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
            headers={"WWW-Authenticate": "Bearer"},
        )

    user = dict(result["u"])

    if user["status"] == "disabled":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Account has been disabled",
        )
    if user["status"] == "frozen":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Account is frozen",
        )

    return user


def get_current_admin(current_user: dict = Depends(get_current_user)) -> dict:
    """Require an authenticated user whose role is 'admin'.

    Reuses get_current_user (so frozen/disabled admins are already rejected),
    then enforces role-based access control. Non-admins receive 403 Forbidden.

    Admin grants READ access to the admin surface. Privileged mutations
    (mint money, change user status, ticket writes) additionally require
    get_current_owner — see below.
    """
    if current_user.get("role") != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Administrator privileges required",
        )
    return current_user


def get_current_owner(current_user: dict = Depends(get_current_admin)) -> dict:
    """Require the single super-owner account.

    Even other admins are rejected — only the account whose email matches
    settings.owner_email may perform privileged mutations. Fails closed:
    if OWNER_EMAIL is unset, nobody passes (so an unconfigured deploy can't
    silently fall back to letting every admin mint money).
    """
    owner_email = (settings.owner_email or "").strip().lower()
    if not owner_email:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Owner account is not configured (set OWNER_EMAIL)",
        )
    if (current_user.get("email") or "").strip().lower() != owner_email:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="This action is restricted to the owner account",
        )
    return current_user
