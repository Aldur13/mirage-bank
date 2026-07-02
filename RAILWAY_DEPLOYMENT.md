# Deploying Mirage Bank to Railway

This guide explains how to deploy the entire Mirage Bank application (backend + frontend) to Railway using the web dashboard — **no CLI commands required**.

## Prerequisites

1. GitHub account with your `mirage-bank` repo pushed
2. Railway account (railway.app)
3. Neo4j Aura database with connection credentials

---

## Step 1: Deploy the Backend Service

1. Go to [railway.app](https://railway.app) and log in
2. Click **New Project** → **Deploy from GitHub repo**
3. Select your `mirage-bank` repository
4. Railway will detect `railway.json` and show service options
5. Select the **backend** service to deploy first
6. Railway will automatically:
   - Detect the Python runtime
   - Read `requirements.txt`
   - Use the build/start commands from `railway.json`

### Configure Backend Environment Variables

In the Railway dashboard for the backend service:

1. Go to **Variables** tab
2. Add these environment variables (get them from your Neo4j Aura console):

| Variable | Value |
|---|---|
| `NEO4J_URI` | `neo4j+s://xxxx.databases.neo4j.io` |
| `NEO4J_USER` | your Aura username |
| `NEO4J_PASSWORD` | your Aura password |
| `NEO4J_DATABASE` | your Aura database name (usually `neo4j`) |
| `JWT_SECRET` | Generate: `python -c "import secrets; print(secrets.token_urlsafe(48))"` |
| `CORS_ORIGINS` | `http://localhost:8913,http://localhost:4173` (will update after frontend deploys) |

3. Click **Deploy** — Railway will build and start the backend

### Verify Backend Deployment

Once deployed:
1. Go to the backend service's **Deployments** tab
2. Note the service URL (e.g., `mirage-bank-backend.railway.app`)
3. Open a new browser tab and visit: `https://mirage-bank-backend.railway.app/health`
4. You should see: `{"status":"ok"}`

---

## Step 2: Deploy the Frontend Service

1. In the same project, click **Add Service** → **Deploy from GitHub repo**
2. Select your `mirage-bank` repository again
3. Select the **frontend** service
4. Railway will detect `package.json` and use Node.js runtime

### Configure Frontend Environment Variables

In the Railway dashboard for the frontend service:

1. Go to **Variables** tab
2. Add this environment variable:

| Variable | Value |
|---|---|
| `BACKEND_URL` | Copy the backend service URL from Step 1 (e.g., `https://mirage-bank-backend.railway.app`) |

3. Click **Deploy** — Railway will build and start the frontend

### Verify Frontend Deployment

Once deployed:
1. Go to the frontend service's **Deployments** tab
2. Note the service URL (e.g., `mirage-bank-frontend.railway.app`)
3. Open a new browser tab and visit: `https://mirage-bank-frontend.railway.app`
4. You should see the Mirage Bank login page

---

## Step 3: Update Backend CORS

Now that the frontend is deployed:

1. Go back to the **backend** service
2. Go to **Variables** tab
3. Update `CORS_ORIGINS` to include the frontend URL:
   ```
   https://mirage-bank-frontend.railway.app
   ```
   (Keep localhost entries for local development if desired)
4. Railway will automatically redeploy with the updated variable

---

## Step 4: Bootstrap the First Admin User

### Option A: Using Railway Terminal (Easiest)

1. Go to the backend service in Railway
2. Click the **Terminal** tab
3. Run:
   ```bash
   python make_admin.py your@email.com
   ```
4. The backend terminal will print a 2FA code — use it in the app

### Option B: Using Local Machine

If you prefer running locally:

1. Set up your `.env` file locally with the same Aura credentials
2. From your local `mirage-bank/backend` directory:
   ```bash
   python make_admin.py your@email.com
   ```

---

## Summary

Your entire application is now live on Railway:

- **Frontend:** `https://mirage-bank-frontend.railway.app`
- **Backend:** `https://mirage-bank-backend.railway.app`
- **Database:** Neo4j Aura (cloud-hosted)

### Automatic Redeployment

Railway automatically redeploys whenever you:
- Push changes to GitHub
- Update environment variables
- Manually trigger a redeploy from the dashboard

### Custom Domain (Optional)

To add a custom domain in Railway:

1. Go to your service → **Settings** → **Domains**
2. Add your domain and configure DNS records per Railway's instructions

---

## Troubleshooting

### Backend health check fails
- Verify all Neo4j environment variables are correct
- Check backend logs in Railway dashboard for connection errors

### Frontend shows "Cannot reach backend"
- Ensure `BACKEND_URL` in frontend variables matches the actual backend service URL
- Check browser console (F12) for CORS errors

### Admin bootstrap fails
- Verify Neo4j credentials are correct
- Try using Railway Terminal instead of local machine
- Check backend logs for Neo4j connection errors

---

## Local Development

To continue developing locally:

```bash
# Terminal 1 — Backend
cd backend
python3.13 -m uvicorn main:app --reload

# Terminal 2 — Frontend
cd frontend
node server.js
```

Then open `http://localhost:3000` in your browser.
