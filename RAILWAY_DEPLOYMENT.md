# Deploying Mirage Bank to Railway

Docker-based deployment — one service, one URL, no hassle.

## Prerequisites

- GitHub account with your `mirage-bank` repo pushed
- Railway account (railway.app)
- Neo4j Aura database credentials

---

## Deploy to Railway

1. Go to [railway.app](https://railway.app) and log in
2. Click **New Project** → **Deploy from GitHub repo**
3. Select your `mirage-bank` repository
4. Railway will detect the `Dockerfile` and build automatically
5. Click **Deploy**

Done. Railway will:
- Build the Docker image with Python 3.13
- Install all dependencies
- Start the app on port 8000
- Serve both API and frontend from the same URL

---

## Configure Environment Variables

Once deployed, go to your service **Variables** tab and add:

| Variable | Value |
|---|---|
| `NEO4J_URI` | `neo4j+s://xxxx.databases.neo4j.io` |
| `NEO4J_USER` | Your Aura username |
| `NEO4J_PASSWORD` | Your Aura password |
| `NEO4J_DATABASE` | Your Aura database name (usually `neo4j`) |
| `JWT_SECRET` | Generate: `python -c "import secrets; print(secrets.token_urlsafe(48))"` |

Save — Railway will redeploy automatically.

---

## Verify It's Working

Once deployed, open your Railway service URL (e.g., `https://mirage-bank.railway.app`):
- You should see the Mirage Bank login page
- Test the health check: visit `/health` → you should see `{"status":"ok"}`

---

## Create First Admin

1. Go to your Railway service **Shell** tab
2. Run:
   ```
   python backend/make_admin.py your@email.com
   ```
3. Copy the 2FA code from the output
4. Use it to log in via the web app

---

## Local Development

```bash
# Terminal 1 — Backend API
cd backend
python -m uvicorn main:app --reload

# Then open http://localhost:8000 in your browser
```

The app is served from `http://localhost:8000` — frontend is included.

---

## Architecture

```
Docker Container (Railway)
├── Python 3.13
├── Backend API (FastAPI) → runs on PORT 8000
└── Frontend (Static HTML/CSS/JS) → served from /
```

Single service, one URL. That's it.
