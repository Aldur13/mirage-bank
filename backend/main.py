from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse

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

# CORS — restrict to the origins configured for this environment
# (see config.Settings.cors_origins; override in prod via the env var).
app.add_middleware(
    CORSMiddleware,
    allow_origins=[o.strip() for o in settings.cors_origins.split(",") if o.strip()],
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


@app.get("/{full_path:path}", include_in_schema=False)
async def serve_frontend(full_path: str):
    """Serve frontend files or index.html for SPA routing. Catch-all, lowest priority."""
    if not frontend_dir.exists():
        return JSONResponse({"error": "Frontend not found"}, status_code=404)

    # Resolve before the containment check: is_relative_to() is purely
    # lexical and does not collapse "..", so an unresolved path like
    # frontend/../backend/config.py would pass it. Resolving first turns
    # the check into a real "is this inside the frontend dir?" guard and
    # blocks path traversal to files outside it.
    base = frontend_dir.resolve()
    file_path = (base / full_path).resolve()

    if file_path.is_file() and file_path.is_relative_to(base):
        return FileResponse(file_path)

    # Otherwise serve index.html (SPA routing)
    index_path = base / "index.html"
    if index_path.is_file():
        return FileResponse(index_path)

    return JSONResponse({"error": "Not found"}, status_code=404)