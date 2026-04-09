import { getApiOrigin } from "./config.js";

export function parseQuery(search = window.location.search) {
  const params = new URLSearchParams(search);
  const obj = {};
  for (const [key, value] of params.entries()) {
    obj[key] = value;
  }
  return obj;
}

export function buildQuery(params = {}) {
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    usp.set(key, String(value));
  });
  const query = usp.toString();
  return query ? `?${query}` : "";
}

export function formatMoney(value) {
  const amount = Number(value || 0);
  return `${new Intl.NumberFormat("vi-VN").format(amount)}đ`;
}

export function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).format(date);
}

export function escapeHtml(text = "") {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

export function debounce(fn, wait = 300) {
  let timeout;
  return (...args) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => fn(...args), wait);
  };
}

export function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

export function isVietnamPhone(value) {
  return /^0\d{9}$/.test(String(value || "").trim());
}

export function getDisplayName(user) {
  if (!user) {
    return "Dang nhap";
  }
  const name = String(user.name || "").trim();
  if (name) {
    return name;
  }
  const email = String(user.email || "");
  return email.split("@")[0] || "Tai khoan";
}

export function resolveImageUrl(path) {
  if (!path) {
    return "./assets/background.png";
  }
  const raw = String(path).trim();
  if (/^https?:\/\//i.test(raw)) {
    return raw;
  }
  if (raw.startsWith("/")) {
    return `${getApiOrigin()}${raw}`;
  }
  return `${getApiOrigin()}/assets/${raw}`;
}

export function setUrlParams(params) {
  const query = buildQuery(params);
  const next = `${window.location.pathname}${query}`;
  window.history.replaceState({}, "", next);
}

export function createStatusLabel(status) {
  const map = {
    pending: "Dang cho",
    processing: "Dang xu ly",
    shipped: "Dang giao",
    delivered: "Da giao",
    cancelled: "Da huy"
  };
  return map[status] || status || "-";
}
