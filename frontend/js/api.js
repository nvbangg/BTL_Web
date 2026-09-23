(function (window) {
  function getToken() {
    return localStorage.getItem("fashon.accessToken") || "";
  }

  async function parseJsonSafely(response) {
    try {
      return await response.json();
    } catch {
      return null;
    }
  }

  function toApiError(response, payload) {
    const error = new Error(
      (payload && payload.message) || `Yêu cầu thất bại (${response.status})`
    );
    error.status = response.status;
    error.payload = payload;
    error.errors = (payload && Array.isArray(payload.errors)) ? payload.errors : [];
    return error;
  }

  async function request(path, options) {
    const settings = options || {};
    const method = settings.method || "GET";
    const requiresAuth = !!settings.auth;
    const suppressUnauthorizedEvent = !!settings.suppressUnauthorizedEvent;

    const headers = {
      Accept: "application/json"
    };

    if (settings.body !== undefined) {
      headers["Content-Type"] = "application/json";
    }

    if (requiresAuth && getToken()) {
      headers.Authorization = "Bearer " + getToken();
    }

    const response = await fetch(AppConfig.buildApiUrl(path), {
      method,
      headers,
      body: settings.body !== undefined ? JSON.stringify(settings.body) : undefined
    });

    const payload = await parseJsonSafely(response);

    if (response.status === 401 && !suppressUnauthorizedEvent) {
      window.dispatchEvent(new CustomEvent("app:unauthorized", { detail: payload }));
    }

    if (!response.ok) {
      throw toApiError(response, payload);
    }

    if (!payload || payload.success !== true) {
      throw toApiError(response, payload);
    }

    return payload.data;
  }

  async function requestFormData(path, formData, options) {
    const settings = options || {};
    const method = settings.method || "POST";
    const requiresAuth = !!settings.auth;
    const suppressUnauthorizedEvent = !!settings.suppressUnauthorizedEvent;

    const headers = {
      Accept: "application/json"
    };

    if (requiresAuth && getToken()) {
      headers.Authorization = "Bearer " + getToken();
    }

    const response = await fetch(AppConfig.buildApiUrl(path), {
      method,
      headers,
      body: formData
    });

    const payload = await parseJsonSafely(response);

    if (response.status === 401 && !suppressUnauthorizedEvent) {
      window.dispatchEvent(new CustomEvent("app:unauthorized", { detail: payload }));
    }

    if (!response.ok) {
      throw toApiError(response, payload);
    }

    if (!payload || payload.success !== true) {
      throw toApiError(response, payload);
    }

    return payload.data;
  }

  function authRegister(email, password, name) {
    return request("/api/auth/register", {
      method: "POST",
      body: { email, password, name },
      suppressUnauthorizedEvent: true
    });
  }

  function authLogin(email, password) {
    return request("/api/auth/login", {
      method: "POST",
      body: { email, password },
      suppressUnauthorizedEvent: true
    });
  }

  function authLogout() {
    return request("/api/auth/logout", {
      method: "POST",
      suppressUnauthorizedEvent: true
    });
  }

  function getMe() {
    return request("/api/users/me", { auth: true });
  }

  function updateMe(body) {
    return request("/api/users/me", {
      method: "PUT",
      auth: true,
      body
    });
  }

  function changePassword(body) {
    return request("/api/users/password", {
      method: "PUT",
      auth: true,
      body
    });
  }

  function getProducts(queryString) {
    return request("/api/products" + (queryString || ""), { suppressUnauthorizedEvent: true });
  }

  function getProductFilters() {
    return request("/api/products/filters", { suppressUnauthorizedEvent: true });
  }

  function getProductDetail(id) {
    return request("/api/products/" + id, { suppressUnauthorizedEvent: true });
  }

  function addToCart(productVariantId, quantity) {
    return request("/api/cart", {
      method: "POST",
      auth: true,
      body: { productVariantId, quantity }
    });
  }

  function getCart() {
    return request("/api/cart", { auth: true });
  }

  function updateCart(itemId, quantity) {
    return request("/api/cart/" + itemId, {
      method: "PUT",
      auth: true,
      body: { quantity }
    });
  }

  function deleteCart(itemId) {
    return request("/api/cart/" + itemId, {
      method: "DELETE",
      auth: true
    });
  }

  function createOrder(body) {
    return request("/api/orders", {
      method: "POST",
      auth: true,
      body
    });
  }

  function getOrders(queryString) {
    return request("/api/orders" + (queryString || ""), {
      auth: true
    });
  }

  function getCheckoutUrl(id) {
    return request("/api/orders/" + id + "/checkout-url", {
      auth: true
    });
  }

  function cancelOrder(id) {
    return request("/api/orders/" + id + "/cancel", {
      method: "POST",
      auth: true
    });
  }

  function getAdminStatistics(queryString) {
    return request("/api/admin/statistics" + (queryString || ""), { auth: true });
  }

  function getAdminOrders(queryString) {
    return request("/api/admin/orders" + (queryString || ""), { auth: true });
  }

  function updateAdminOrderStatus(id, status) {
    return request("/api/admin/orders/" + id, {
      method: "PUT",
      auth: true,
      body: { status }
    });
  }

  function getAdminProducts(queryString) {
    return request("/api/admin/products" + (queryString || ""), { auth: true });
  }

  function getAdminProductDetail(id) {
    return request("/api/admin/products/" + id, { auth: true });
  }

  function createAdminProduct(body) {
    return request("/api/admin/products", {
      method: "POST",
      auth: true,
      body
    });
  }

  function updateAdminProduct(id, body) {
    return request("/api/admin/products/" + id, {
      method: "PUT",
      auth: true,
      body
    });
  }

  function uploadAdminProductImage(file) {
    const formData = new FormData();
    formData.append("file", file);
    return requestFormData("/api/admin/images/upload", formData, {
      method: "POST",
      auth: true
    });
  }

  function getAdminUsers(queryString) {
    return request("/api/admin/users" + (queryString || ""), { auth: true });
  }

  function updateAdminUserRole(id, role) {
    return request("/api/admin/users/" + id, {
      method: "PUT",
      auth: true,
      body: { role }
    });
  }

  window.AppApi = {
    request,
    authRegister,
    authLogin,
    authLogout,
    getMe,
    updateMe,
    changePassword,
    getProducts,
    getProductFilters,
    getProductDetail,
    addToCart,
    getCart,
    updateCart,
    deleteCart,
    createOrder,
    getOrders,
    getCheckoutUrl,
    cancelOrder,
    getAdminStatistics,
    getAdminOrders,
    updateAdminOrderStatus,
    getAdminProducts,
    getAdminProductDetail,
    createAdminProduct,
    updateAdminProduct,
    uploadAdminProductImage,
    getAdminUsers,
    updateAdminUserRole
  };
})(window);
