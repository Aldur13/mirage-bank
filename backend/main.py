from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from database import close_driver, setup_constraints, setup_treasury
from routes import account_router, admin_router, auth_router, support_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_constraints()
    setup_treasury()
    yield
    close_driver()


app = FastAPI(
    title="Mirage Bank API",
    description="Mirage Bank — Phase 4B (Full Platform)",
    version="4.0.0",
    lifespan=lifespan,
)

# Safe CORS fallback: use env value if provided, otherwise default to local + production origins.
default_origins = [
    "https://mirage-bank.vercel.app",
    "http://localhost:3000",
    "http://127.0.0.1:3000",
]

raw_origins = getattr(settings, "cors_origins", "") or ""
origins = [o.strip() for o in raw_origins.split(",") if o.strip()] or default_origins

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", tags=["Health"])
def health():
    """Lightweight liveness probe for the hosting platform."""
    return {"status": "ok"}


app.include_router(auth_router, tags=["Auth"])
app.include_router(account_router, tags=["Account"])
app.include_router(support_router, tags=["Support"])
app.include_router(admin_router, tags=["Admin"])