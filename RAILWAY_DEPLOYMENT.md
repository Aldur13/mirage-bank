# Deploying Mirage Bank to Railway

One-click deployment — backend API + frontend served from the same service. No separate URLs, no complexity.

## Prerequisites

- GitHub account with your `mirage-bank` repo pushed
- Railway account (railway.app)
- Neo4j Aura database credentials

---

## Deploy to Railway

1. Go to [railway.app](https://railway.app) and log in
2. Click **New Project** → **Deploy from GitHub repo**
3. Select your `mirage-bank` repository
4. Click **Deploy**

That's it. Railway will auto-detect the `Procfile` at the root and:
- Install Python dependencies from `backend/requirements.txt`
- Start the backend with `start.sh`
- Serve the frontend from the same URL as the API

---

## Configure Environment Variables

Once deploying, go to your service **Variables** tab and add:

| Variable | Value |
|---|---|
| `NEO4J_URI` | `neo4j+s://xxxx.databases.neo4j.io` |
| `NEO4J_USER` | Your Aura username |
| `NEO4J_PASSWORD` | Your Aura password |
| `NEO4J_DATABASE` | Your Aura database name (usually `neo4j`) |
| `JWT_SECRET` | Generate: `python -c "import secrets; print(secrets.token_urlsafe(48))"` |

Save and redeploy. Done.

---

## Verify It's Working

Once deployed, open your Railway service URL (e.g., `https://mirage-bank.railway.app`):
- You should see the Mirage Bank login page
- Test the health check: visit `/health` → you should see `{"status":"ok"}`

---

## Create First Admin

1. Go to your Railway service **Terminal** tab
2. Run:
   ```
   python make_admin.py your@email.com
   ```
3. Copy the 2FA code printed in the terminal
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
Railway Service (mirage-bank.railway.app)
├── Backend API (FastAPI) → runs on PORT 8000
└── Frontend (Static HTML/CSS/JS) → served from /
    (frontend files hosted at the same domain)
```

No separate frontend service. Everything from one URL.
