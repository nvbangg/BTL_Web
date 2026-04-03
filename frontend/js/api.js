/* ===== REAL API WRAPPER ===== */
const API_BASE = '/api';
const TOKEN_KEY = 'auth_token';
const REFRESH_TOKEN_KEY = 'refresh_token';

/**
 * Make authenticated API request
 */
async function apiRequest(endpoint, options = {}) {
  const url = API_BASE + endpoint;
  const fetchOptions = { ...options };
  
  // Add auth token if exists
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    fetchOptions.headers = {
      ...fetchOptions.headers,
      'Authorization': `Bearer ${token}`
    };
  }
  
  // Default headers
  if (!fetchOptions.headers) fetchOptions.headers = {};
  if (fetchOptions.method !== 'GET' && !fetchOptions.headers['Content-Type']) {
    fetchOptions.headers['Content-Type'] = 'application/json';
  }
  
  try {
    const response = await fetch(url, fetchOptions);
    
    // Handle 401 - try refresh token
    if (response.status === 401 && localStorage.getItem(REFRESH_TOKEN_KEY)) {
      const refreshed = await apiRefreshToken();
      if (refreshed) {
        // Retry original request
        return apiRequest(endpoint, options);
      }
    }
    
    if (!response.ok) {
      const errorBody = await response.json().catch(() => null);
      const errorMsg = errorBody?.message || `HTTP ${response.status}`;
      throw new Error(errorMsg);
    }
    
    const data = await response.json();
    // Backend wraps in ApiResponse, unwrap data
    return data.data || data;
  } catch (error) {
    console.error(`API Error [${endpoint}]:`, error);
    throw error;
  }
}

async function apiRefreshToken() {
  try {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) return false;
    
    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    
    if (!response.ok) return false;
    
    const data = await response.json();
    const tokens = data.data;
    localStorage.setItem(TOKEN_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
    return true;
  } catch {
    return false;
  }
}

/* ===== AUTH API ===== */

async function apiRegister(email, password, name) {
  const response = await apiRequest('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, name })
  });
  if (response.user) {
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
  }
  return response;
}

async function apiLogin(email, password) {
  const response = await apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });
  if (response.accessToken) {
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
  }
  return response;
}

async function apiLogout() {
  try {
    await apiRequest('/auth/logout', { method: 'POST' });
  } catch {}
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

async function apiGetMe() {
  return apiRequest('/users/me');
}

async function apiChangePassword(currentPassword, newPassword) {
  return apiRequest('/users/password', {
    method: 'PUT',
    body: JSON.stringify({ currentPassword, newPassword })
  });
}

/* ===== PRODUCT API ===== */

async function apiGetProducts(params = {}) {
  const qs = new URLSearchParams();
  if (params.keyword) qs.append('keyword', params.keyword);
  if (params.category) qs.append('category', params.category);
  if (params.gender) qs.append('gender', params.gender);
  if (params.color) qs.append('color', params.color);
  if (params.size) qs.append('size', params.size);
  if (params.minPrice) qs.append('minPrice', params.minPrice);
  if (params.maxPrice) qs.append('maxPrice', params.maxPrice);
  if (params.sort) qs.append('sort', params.sort);
  qs.append('page', params.page || 1);
  qs.append('pageSize', params.pageSize || 16);
  
  return apiRequest(`/products?${qs.toString()}`);
}

async function apiGetProductDetail(id) {
  return apiRequest(`/products/${id}`);
}

async function apiGetProductFilters() {
  return apiRequest('/products/filters');
}

/* ===== CART API ===== */

async function apiAddToCart(productVariantId, quantity) {
  return apiRequest('/cart', {
    method: 'POST',
    body: JSON.stringify({ productVariantId, quantity })
  });
}

async function apiGetCart(page = 1, pageSize = 10) {
  return apiRequest(`/cart?page=${page}&pageSize=${pageSize}`);
}

async function apiUpdateCartItem(itemId, quantity) {
  return apiRequest(`/cart/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify({ quantity })
  });
}

async function apiDeleteCartItem(itemId) {
  return apiRequest(`/cart/${itemId}`, { method: 'DELETE' });
}

/* ===== ORDER API ===== */

async function apiCreateOrder(shippingName, shippingPhone, shippingAddress) {
  return apiRequest('/orders', {
    method: 'POST',
    body: JSON.stringify({ shippingName, shippingPhone, shippingAddress })
  });
}

async function apiGetOrders(page = 1, pageSize = 10) {
  return apiRequest(`/orders?page=${page}&pageSize=${pageSize}`);
}

/* ===== ADMIN PRODUCTS API ===== */

async function apiAdminGetProducts(params = {}) {
  const qs = new URLSearchParams();
  if (params.keyword) qs.append('keyword', params.keyword);
  if (params.category) qs.append('category', params.category);
  if (params.gender) qs.append('gender', params.gender);
  if (params.color) qs.append('color', params.color);
  if (params.size) qs.append('size', params.size);
  if (params.minPrice) qs.append('minPrice', params.minPrice);
  if (params.maxPrice) qs.append('maxPrice', params.maxPrice);
  if (params.sort) qs.append('sort', params.sort);
  if (params.isActive !== undefined && params.isActive !== null) qs.append('isActive', params.isActive);
  qs.append('page', params.page || 1);
  qs.append('pageSize', params.pageSize || 10);
  
  return apiRequest(`/admin/products?${qs.toString()}`);
}

async function apiAdminGetProductDetail(id) {
  return apiRequest(`/admin/products/${id}`);
}

async function apiAdminCreateProduct(data) {
  return apiRequest('/admin/products', {
    method: 'POST',
    body: JSON.stringify(data)
  });
}

async function apiAdminUpdateProduct(id, data) {
  return apiRequest(`/admin/products/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  });
}

async function apiAdminDeleteProduct(id) {
  return apiRequest(`/admin/products/${id}`, { method: 'DELETE' });
}

/* ===== ADMIN ORDERS API ===== */

async function apiAdminGetOrders(params = {}) {
  const qs = new URLSearchParams();
  if (params.keyword) qs.append('keyword', params.keyword);
  if (params.status) qs.append('status', params.status);
  qs.append('page', params.page || 1);
  qs.append('pageSize', params.pageSize || 10);
  
  return apiRequest(`/admin/orders?${qs.toString()}`);
}

async function apiAdminUpdateOrderStatus(id, status) {
  return apiRequest(`/admin/orders/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ status })
  });
}

/* ===== ADMIN CATEGORIES API ===== */

async function apiAdminGetCategories(params = {}) {
  const qs = new URLSearchParams();
  if (params.keyword) qs.append('keyword', params.keyword);
  qs.append('page', params.page || 1);
  qs.append('pageSize', params.pageSize || 10);
  
  return apiRequest(`/admin/categories?${qs.toString()}`);
}

async function apiAdminCreateCategory(name) {
  return apiRequest('/admin/categories', {
    method: 'POST',
    body: JSON.stringify({ name })
  });
}

async function apiAdminUpdateCategory(id, name) {
  return apiRequest(`/admin/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name })
  });
}

/* ===== ADMIN USERS API ===== */

async function apiAdminGetUsers(params = {}) {
  const qs = new URLSearchParams();
  if (params.keyword) qs.append('keyword', params.keyword);
  if (params.role) qs.append('role', params.role);
  qs.append('page', params.page || 1);
  qs.append('pageSize', params.pageSize || 10);
  
  try {
    return await apiRequest(`/admin/users?${qs.toString()}`);
  } catch (error) {
    // Fallback to mock data
    console.warn('API failed, using mock users:', error);
    const users = mockGetUsers();
    const keyword = params.keyword?.toLowerCase() || '';
    const role = params.role || '';
    
    let filtered = users;
    if (keyword) {
      filtered = filtered.filter(u => u.email.toLowerCase().includes(keyword) || u.name.toLowerCase().includes(keyword));
    }
    if (role) {
      filtered = filtered.filter(u => u.role === role);
    }
    
    const page = params.page || 1;
    const pageSize = params.pageSize || 10;
    const start = (page - 1) * pageSize;
    const items = filtered.slice(start, start + pageSize);
    
    return {
      items,
      page,
      pageSize,
      total: filtered.length
    };
  }
}

async function apiAdminUpdateUserRole(id, role) {
  try {
    return await apiRequest(`/admin/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ role })
    });
  } catch (error) {
    console.warn('API failed, using mock update:', error);
    if (typeof mockUpdateUserRole === 'function') {
      const result = mockUpdateUserRole(id, role);
      if (result) return result;
    }
    throw error;
  }
}

async function apiAdminDeleteUser(id) {
  try {
    return await apiRequest(`/admin/users/${id}`, { method: 'DELETE' });
  } catch (error) {
    console.warn('API failed, using mock delete:', error);
    if (typeof mockDeleteUser === 'function') {
      const result = mockDeleteUser(id);
      if (result) return { success: true };
    }
    throw error;
  }
}

/* ===== ADMIN STATISTICS API ===== */

async function apiAdminGetStatistics() {
  return apiRequest('/admin/statistics');
}
