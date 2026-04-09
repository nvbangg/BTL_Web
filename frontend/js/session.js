import { APP_CONFIG } from "./config.js";

export function getToken() {
  return localStorage.getItem(APP_CONFIG.TOKEN_KEY) || "";
}

export function hasToken() {
  return Boolean(getToken());
}

export function getUser() {
  const text = localStorage.getItem(APP_CONFIG.USER_KEY);
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch (_error) {
    return null;
  }
}

export function setSession(token, user) {
  if (token) {
    localStorage.setItem(APP_CONFIG.TOKEN_KEY, token);
  }
  if (user) {
    localStorage.setItem(APP_CONFIG.USER_KEY, JSON.stringify(user));
  }
  window.dispatchEvent(new CustomEvent("app:login", { detail: { user } }));
}

export function updateUser(user) {
  if (!user) {
    return;
  }
  localStorage.setItem(APP_CONFIG.USER_KEY, JSON.stringify(user));
  window.dispatchEvent(new CustomEvent("app:user-updated", { detail: { user } }));
}

export function clearSession(triggerEvent = true) {
  localStorage.removeItem(APP_CONFIG.TOKEN_KEY);
  localStorage.removeItem(APP_CONFIG.USER_KEY);
  if (triggerEvent) {
    window.dispatchEvent(new Event("app:logout"));
  }
}
