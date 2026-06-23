// Single source of truth for the backend API base URL.
// Local development (localhost / 127.0.0.1) automatically targets the local
// FastAPI server; everything else targets the deployed backend.
//
// After deploying the backend, replace YOUR-BACKEND-URL below with the real host
// (e.g. https://mirage-bank.onrender.com) and redeploy the frontend.
window.API_BASE_URL =
    (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
        ? 'http://localhost:8000'
        : 'https://YOUR-BACKEND-URL.onrender.com';
