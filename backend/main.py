from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from config import settings
from database import close_driver, setup_constraints, setup_treasury
from routes import account_router, admin_router, auth_router, support_router, premium_router


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

# CORS — only needed for local dev with separate frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
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
app.include_router(premium_router, tags=["Premium"])


# Serve static frontend files from the frontend directory
frontend_dir = Path(__file__).parent.parent / "frontend"
if frontend_dir.exists():
    app.mount("/", StaticFiles(directory=frontend_dir, html=True), name="static")