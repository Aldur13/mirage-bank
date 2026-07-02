from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

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


@app.get("/{full_path:path}", include_in_schema=False)
async def serve_frontend(full_path: str):
    """Serve frontend files or index.html for SPA routing. Catch-all, lowest priority."""
    if not frontend_dir.exists():
        return {"error": "Frontend not found"}, 404

    file_path = frontend_dir / full_path

    # If it's a file that exists, serve it
    if file_path.is_file() and file_path.is_relative_to(frontend_dir):
        return FileResponse(file_path)

    # Otherwise serve index.html (SPA routing)
    index_path = frontend_dir / "index.html"
    if index_path.exists():
        return FileResponse(index_path)

    return {"error": "Not found"}, 404