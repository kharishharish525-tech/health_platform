/**
 * Shared fetch wrapper for the whole site.
 * Authenticated requests use the browser's same-origin session cookie.
 */
async function apiFetch(path, options = {}) {
  const headers = Object.assign(
    { "Content-Type": "application/json" },
    options.headers || {}
  );
  const res = await fetch(path, Object.assign({}, options, { headers }));
  const contentType = res.headers.get("content-type") || "";
  const isJsonResponse = contentType.includes("application/json") || contentType.includes("+json");
  const rawText = await res.text();

  if (!rawText) {
    if (!res.ok) throw new Error(res.statusText || ("Request failed: " + res.status));
    return null;
  }

  if (isJsonResponse) {
    try {
      const body = JSON.parse(rawText);
      if (!res.ok) {
        throw new Error(body?.message || res.statusText || ("Request failed: " + res.status));
      }
      return body;
    } catch (e) {
      if (!res.ok) {
        throw new Error(res.statusText || ("Request failed: " + res.status));
      }
      throw new Error("The server returned invalid JSON. Please refresh and try again.");
    }
  }

  if (!res.ok) {
    const trimmed = rawText.replace(/\s+/g, " ").trim();
    throw new Error(trimmed || res.statusText || ("Request failed: " + res.status));
  }

  return rawText;
}

document.addEventListener("DOMContentLoaded", () => {
  const nav = document.querySelector(".sidebar nav");
  if (!nav) return;

  const form = document.createElement("form");
  form.action = "/logout";
  form.method = "post";
  form.className = "sidebar-signout";
  form.innerHTML = '<button type="submit">Sign out</button>';
  nav.after(form);
});

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
