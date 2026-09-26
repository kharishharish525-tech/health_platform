/**
 * Shared fetch wrapper for the whole site.
 * All /api/** endpoints require HTTP Basic auth (see SecurityConfig.java).
 * Default demo credentials are admin / admin123 — change ADMIN_USER /
 * ADMIN_PASS below (and in SecurityConfig.java) before real deployment.
 */
const ADMIN_USER = "admin";
const ADMIN_PASS = "admin123";
const AUTH_HEADER = "Basic " + btoa(ADMIN_USER + ":" + ADMIN_PASS);

async function apiFetch(path, options = {}) {
  const headers = Object.assign(
    { "Authorization": AUTH_HEADER, "Content-Type": "application/json" },
    options.headers || {}
  );
  const res = await fetch(path, Object.assign({}, options, { headers }));
  if (!res.ok) {
    let message = res.statusText;
    try {
      const body = await res.json();
      message = body.message || message;
    } catch (e) { /* not JSON, ignore */ }
    throw new Error(message || ("Request failed: " + res.status));
  }
  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) return res.json();
  return res.text();
}

function money(value) {
  const n = Number(value || 0);
  return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDateTime(value) {
  if (!value) return "";
  const d = new Date(value);
  if (isNaN(d.getTime())) return value;
  return d.toLocaleString();
}

function priorityBadge(priority) {
  if (!priority) return "";
  const cls = priority.toLowerCase();
  return `<span class="badge ${cls}">${priority}</span>`;
}

function statusBadge(status) {
  if (!status) return "";
  const cls = status.toLowerCase();
  return `<span class="badge ${cls}">${status}</span>`;
}

function showError(containerEl, err) {
  containerEl.innerHTML = `<div class="alert error">${err.message || err}</div>`;
}

function showSuccess(containerEl, message) {
  containerEl.innerHTML = `<div class="alert success">${message}</div>`;
}
