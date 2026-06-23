# Mirage Bank

A full-stack digital banking demo — FastAPI backend, Neo4j Aura database, and a pure static HTML/CSS/JS frontend.

## Stack

| Layer | Technology |
|---|---|
| Frontend | Static HTML/CSS/JS — deploy on **Vercel** |
| Backend | Python 3.13 + FastAPI + Uvicorn — deploy on **Render** or **Railway** |
| Database | Neo4j Aura (cloud-hosted graph DB) |
| Auth | JWT (HS256, 60-min TTL) + bcrypt passwords |

---

## Local development

**Requirements:** Python 3.13, packages in `requirements.txt`, a Neo4j Aura instance, a `.env` file.

```
# Copy and fill in the example env
cp .env.example .env

# Start backend (terminal 1)
cd backend
python3.13 -m uvicorn main:app --reload

# Start frontend (terminal 2)
py -m http.server 8913 --directory frontend
```

Open **http://localhost:8913**.

To create the first admin user:
```
cd backend
python3.13 make_admin.py your@email.com
```
Admin login triggers a 2FA code that prints to the **backend terminal** (no SMTP configured by default).

---

## Deploying to production

### Step 0 — clean repo (required)

The project must live in its own git repository before deploying. Copy the contents of `banking-system/` into a fresh repo and push to GitHub:

```
mkdir mirage-bank && cd mirage-bank
git init
# copy backend/ frontend/ requirements.txt .gitignore .env.example here
git add .
git commit -m "initial"
git remote add origin https://github.com/YOU/mirage-bank.git
git push -u origin main
```

Confirm `.env` is **not** tracked (`git status` should not list it).

### Step 1 — rotate secrets

Before going public, generate fresh values for:

```bash
# New JWT secret
python -c "import secrets; print(secrets.token_urlsafe(48))"

# Neo4j Aura password — rotate in the Aura console, then update .env
```

### Step 2 — deploy the backend (Render)

1. **New → Web Service** → connect your GitHub repo → **Root Directory: `backend`**
2. **Build command:** `pip install -r requirements.txt`
3. **Start command:** `uvicorn main:app --host 0.0.0.0 --port $PORT`
4. **Environment variables** (set in the Render dashboard, never commit these):

| Variable | Value |
|---|---|
| `NEO4J_URI` | `neo4j+s://xxxx.databases.neo4j.io` |
| `NEO4J_USER` | your Aura username |
| `NEO4J_PASSWORD` | your Aura password |
| `NEO4J_DATABASE` | your Aura database name |
| `JWT_SECRET` | the value you generated above |
| `CORS_ORIGINS` | `https://YOUR-APP.vercel.app` (set after step 4) |

5. Deploy. Note the URL — e.g. `https://mirage-bank.onrender.com`.
6. Verify: `GET https://mirage-bank.onrender.com/health` → `{"status":"ok"}`

> **Railway alternative:** New Project → deploy from repo → service root `backend` → same env vars. No cold-start spindown (free tier).

> **Free Render note:** the service spins down after ~15 min idle; first request takes ~50s to wake up.

### Step 3 — set the backend URL in the frontend

Edit `frontend/config.js`:

```js
window.API_BASE_URL =
    (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
        ? 'http://localhost:8000'
        : 'https://mirage-bank.onrender.com';  // ← your real Render URL
```

Commit and push.

### Step 4 — deploy the frontend (Vercel)

1. **Import** your GitHub repo on vercel.com
2. **Root Directory:** `frontend`
3. **Framework Preset:** Other (no build step — pure static)
4. Deploy. Note the URL — e.g. `https://mirage-bank.vercel.app`

### Step 5 — wire CORS

Go back to Render → update `CORS_ORIGINS` to your actual Vercel URL:

```
CORS_ORIGINS=https://mirage-bank.vercel.app
```

Redeploy the backend.

### Step 6 — bootstrap an admin

Register a user through the live Vercel site, then run `make_admin.py` locally pointing at the same Aura DB:

```bash
cd backend
python3.13 make_admin.py your@email.com
```

Or use a Render one-off shell if you don't have the credentials locally.

---

## Environment variables reference

See `.env.example` for the full list. Never commit `.env`.

| Variable | Required | Description |
|---|---|---|
| `NEO4J_URI` | ✓ | Aura connection URI |
| `NEO4J_USER` | ✓ | Aura username |
| `NEO4J_PASSWORD` | ✓ | Aura password |
| `NEO4J_DATABASE` | ✓ | Aura database name |
| `JWT_SECRET` | ✓ | Random secret for signing tokens |
| `CORS_ORIGINS` | — | Comma-separated allowed origins (defaults to localhost) |
| `SMTP_HOST` | — | SMTP server (omit to print OTP codes to the console) |

---

## API routes

```
GET  /health
POST /register    POST /login      GET  /me
GET  /balance     POST /withdraw   POST /transfer    GET /transactions
PATCH /profile    POST /profile/password
GET  /guardian/ward    POST /guardian/freeze    POST /guardian/unfreeze
GET  /business/members  POST /business/invite   DELETE /business/members/{id}
POST /support/tickets   GET /support/tickets    GET /support/tickets/{id}
POST /support/tickets/{id}/messages   POST /support/tickets/{id}/close
GET  /admin/users   POST /admin/freeze   POST /admin/unfreeze   POST /admin/disable
POST /admin/credit  GET  /admin/transactions  GET /admin/treasury
GET  /admin/support/tickets   PATCH /admin/support/tickets/{id}
```

---

*Mirage Bank is a fictional demo project. No real banking services are provided.*
