// Single source of truth for the backend API base URL.
// For Railway deployment: set BACKEND_URL environment variable on the frontend service
// For local development: automatically targets localhost:8000
window.API_BASE_URL =
    (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
        ? 'http://localhost:8000'
        : (window.__BACKEND_URL__ || 'https://YOUR-BACKEND-URL.railway.app');
