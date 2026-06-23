const API = window.API_BASE_URL || 'http://localhost:8000';

// ── Auth token ────────────────────────────────────────────────────
function getToken()   { return localStorage.getItem('banking_token'); }
function setToken(t)  { localStorage.setItem('banking_token', t); }
function clearToken() { localStorage.removeItem('banking_token'); }

// ── API fetch ────────────────────────────────────────────────────
async function apiFetch(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const options = { method, headers };
    if (body !== null) options.body = JSON.stringify(body);

    try {
        const res = await fetch(API + path, options);
        let data;
        try { data = await res.json(); }
        catch { data = { detail: 'Unexpected server response.' }; }
        return { ok: res.ok, status: res.status, data };
    } catch {
        return { ok: false, status: 0, data: { detail: 'Cannot reach the server. Is the backend running?' } };
    }
}

// Fetch the authenticated user's profile (role + preferences, source of truth).
async function getMe() {
    const { ok, data } = await apiFetch('GET', '/me');
    return ok ? data : null;
}

// ── Theme ─────────────────────────────────────────────────────────
function getTheme() {
    return localStorage.getItem('mb_theme') || 'dark';
}

function applyTheme(theme) {
    document.body.classList.toggle('light', theme === 'light');
    localStorage.setItem('mb_theme', theme);
}

function loadTheme() {
    applyTheme(getTheme());
}

async function saveTheme(theme) {
    applyTheme(theme);
    await apiFetch('PATCH', '/profile', { theme });
}

// ── Avatar ────────────────────────────────────────────────────────
function getInitials(name) {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    return parts.length >= 2
        ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
        : parts[0].slice(0, 2).toUpperCase();
}

function renderAvatar(container, avatarData, name) {
    if (!container) return;
    if (avatarData) {
        container.innerHTML = `<img src="${avatarData}" alt="${name || ''}" />`;
    } else {
        container.textContent = getInitials(name);
        container.style.background = 'var(--grad)';
    }
}

// Apply /me data to sidebar user chip
function applyUserMeta(me) {
    if (!me) return;

    // theme sync (prefer server value)
    if (me.theme && me.theme !== getTheme()) {
        applyTheme(me.theme);
    }

    // sidebar name / email
    const nameEl  = document.getElementById('sidebar-name');
    const emailEl = document.getElementById('sidebar-email');
    if (nameEl)  nameEl.textContent  = me.name;
    if (emailEl) emailEl.textContent = me.email;

    // avatar in sidebar
    const avatarEl = document.getElementById('sidebar-avatar');
    if (avatarEl) renderAvatar(avatarEl, me.avatar_data, me.name);

    // admin link visibility
    const adminLink = document.getElementById('admin-nav-item');
    if (adminLink) adminLink.style.display = me.role === 'admin' ? '' : 'none';
}

// ── Formatting ───────────────────────────────────────────────────
function formatMoney(cents, currency = 'EUR') {
    return new Intl.NumberFormat('en-IE', { style: 'currency', currency }).format(cents / 100);
}

function formatDateTime(isoString) {
    const d = new Date(isoString);
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
         + ', ' + d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

function formatDate(isoString) {
    return new Date(isoString).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

function titleCase(s) {
    return s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
}

function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// ── API error normaliser ──────────────────────────────────────────
// FastAPI 422s return detail as an array; other errors return a string.
function handleApiError(data, fallback = 'Something went wrong.') {
    if (!data) return fallback;
    if (Array.isArray(data.detail)) {
        return data.detail
            .map(e => (e.msg || '').replace(/^value error,\s*/i, ''))
            .filter(Boolean)
            .join(' · ') || fallback;
    }
    return data.detail || fallback;
}

// ── Inline alerts ────────────────────────────────────────────────
function showError(el, message) {
    el.textContent = message;
    el.className = 'alert alert-error show';
}

function showSuccess(el, message) {
    el.textContent = message;
    el.className = 'alert alert-success show';
}

function hideAlert(el) {
    el.className = 'alert';
}

// ── Toast notifications ──────────────────────────────────────────
let _toastTimer = null;

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    clearTimeout(_toastTimer);
    toast.textContent = message;
    toast.className = 'toast-visible toast-' + type;
    _toastTimer = setTimeout(() => toast.className = '', 3500);
}

// ── Modal helpers ─────────────────────────────────────────────────
function openModal(id)  { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

function bindModalClose(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    modal.querySelectorAll('.modal-close, [data-close-modal]').forEach(btn => {
        btn.addEventListener('click', () => closeModal(modalId));
    });
    modal.addEventListener('click', e => {
        if (e.target === modal) closeModal(modalId);
    });
}

// ── Sidebar mobile toggle ─────────────────────────────────────────
function initSidebar() {
    const sidebar = document.getElementById('sidebar');
    const menuBtn = document.getElementById('menu-btn');
    if (!sidebar || !menuBtn) return;
    menuBtn.addEventListener('click', () => sidebar.classList.toggle('open'));
    // Close sidebar when clicking a nav link on mobile
    sidebar.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', () => {
            if (window.innerWidth <= 880) sidebar.classList.remove('open');
        });
    });
}

// ── Auth guard + init ─────────────────────────────────────────────
async function requireAuth(adminRequired = false) {
    if (!getToken()) { window.location.href = 'login.html'; return null; }
    const me = await getMe();
    if (!me) { clearToken(); window.location.href = 'login.html'; return null; }
    if (adminRequired && me.role !== 'admin') { window.location.href = 'dashboard.html'; return null; }
    applyUserMeta(me);
    return me;
}
