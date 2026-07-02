// API base URL — same domain for both frontend and backend (served from same app)
window.API_BASE_URL =
    (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
        ? 'http://localhost:8000'
        : window.location.origin;
