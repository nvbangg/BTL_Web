const runtimeApiBase = window.FASHONSHOP_API_BASE_URL || window.__FASHONSHOP_API_BASE_URL__;

export const APP_CONFIG = {
  API_BASE_URL: (runtimeApiBase || "http://localhost:8080").replace(/\/+$/, ""),
  TOKEN_KEY: "fashonshop.accessToken",
  USER_KEY: "fashonshop.user",
  DEFAULT_HOME_PAGE_SIZE: 16,
  DEFAULT_LIST_PAGE_SIZE: 10
};

export const ORDER_STATUSES = ["pending", "processing", "shipped", "delivered", "cancelled"];
export const GENDERS = ["male", "female", "unisex"];

export function getApiOrigin() {
  try {
    return new URL(APP_CONFIG.API_BASE_URL).origin;
  } catch (_error) {
    return APP_CONFIG.API_BASE_URL;
  }
}
