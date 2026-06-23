from .auth import router as auth_router
from .account import router as account_router
from .admin import router as admin_router
from .support import router as support_router

__all__ = ["auth_router", "account_router", "admin_router", "support_router"]
