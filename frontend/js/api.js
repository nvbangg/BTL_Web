import { APP_CONFIG } from "./config.js";
import { clearSession, getToken, updateUser } from "./session.js";
import { buildQuery } from "./utils.js";

async function parseJson(response) {
  try {
    return await response.json();
  } catch (_error) {
    return null;
  }
}

function createUrl(path, query) {
  const cleanPath = path.startsWith("/") ? path : `/${path}`;
  return `${APP_CONFIG.API_BASE_URL}${cleanPath}${buildQuery(query)}`;
}

function createError(response, payload, fallbackMessage) {
  return {
    status: response.status,
    message: payload?.message || fallbackMessage,
    errors: payload?.errors || [],
    payload
  };
}

export async function apiRequest(path, options = {}) {
  const {
    method = "GET",
    query,
    body,
    auth = false,
    skipUnauthorizedHandler = false
  } = options;

  const headers = {
    Accept: "application/json"
  };

  let payloadBody;
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payloadBody = JSON.stringify(body);
  }

  if (auth) {
    const token = getToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  const response = await fetch(createUrl(path, query), {
    method,
    headers,
    body: payloadBody
  });

  const payload = await parseJson(response);

  if (!response.ok) {
    if (response.status === 401 && !skipUnauthorizedHandler) {
      clearSession(false);
      window.dispatchEvent(
        new CustomEvent("app:unauthorized", {
          detail: {
            message:
              payload?.message || "Phien dang nhap het han. Vui long dang nhap lai."
          }
        })
      );
    }
    throw createError(response, payload, "Co loi xay ra, vui long thu lai.");
  }

  if (payload && payload.success === false) {
    throw {
      status: response.status,
      message: payload.message || "Co loi xay ra",
      errors: payload.errors || [],
      payload
    };
  }

  return payload;
}

export const api = {
  register: (data) => apiRequest("/api/auth/register", { method: "POST", body: data, skipUnauthorizedHandler: true }),
  login: (data) => apiRequest("/api/auth/login", { method: "POST", body: data, skipUnauthorizedHandler: true }),
  logout: () => apiRequest("/api/auth/logout", { method: "POST", auth: true }),

  getMe: () => apiRequest("/api/users/me", { auth: true }),
  updateMe: (data) => apiRequest("/api/users/me", { method: "PUT", auth: true, body: data }),
  changePassword: (data) => apiRequest("/api/users/password", { method: "PUT", auth: true, body: data }),

  getProducts: (query) => apiRequest("/api/products", { query }),
  getProductFilters: () => apiRequest("/api/products/filters"),
  getProductDetail: (id) => apiRequest(`/api/products/${id}`),

  addToCart: (data) => apiRequest("/api/cart", { method: "POST", auth: true, body: data }),
  getCart: () => apiRequest("/api/cart", { auth: true }),
  updateCartItem: (itemId, data) => apiRequest(`/api/cart/${itemId}`, { method: "PUT", auth: true, body: data }),
  removeCartItem: (itemId) => apiRequest(`/api/cart/${itemId}`, { method: "DELETE", auth: true }),

  createOrder: (data) => apiRequest("/api/orders", { method: "POST", auth: true, body: data }),
  getOrders: (query) => apiRequest("/api/orders", { auth: true, query }),

  getAdminStatistics: () => apiRequest("/api/admin/statistics", { auth: true }),
  getAdminOrders: (query) => apiRequest("/api/admin/orders", { auth: true, query }),
  updateAdminOrder: (id, data) => apiRequest(`/api/admin/orders/${id}`, { method: "PUT", auth: true, body: data }),

  getAdminProducts: (query) => apiRequest("/api/admin/products", { auth: true, query }),
  getAdminProductDetail: (id) => apiRequest(`/api/admin/products/${id}`, { auth: true }),
  createAdminProduct: (data) => apiRequest("/api/admin/products", { method: "POST", auth: true, body: data }),
  updateAdminProduct: (id, data) => apiRequest(`/api/admin/products/${id}`, { method: "PUT", auth: true, body: data }),

  getAdminUsers: (query) => apiRequest("/api/admin/users", { auth: true, query }),
  updateAdminUserRole: (id, data) => apiRequest(`/api/admin/users/${id}`, { method: "PUT", auth: true, body: data })
};

export async function refreshCurrentUser() {
  const response = await api.getMe();
  if (response?.data) {
    updateUser(response.data);
  }
  return response?.data || null;
}
