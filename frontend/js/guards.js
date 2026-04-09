import { api } from "./api.js";
import { clearSession, hasToken, updateUser } from "./session.js";
import { showToast } from "./ui.js";

export async function requireUser(options = {}) {
  const basePath = options.basePath || document.body.dataset.basePath || "./";

  if (!hasToken()) {
    if (options.redirect !== false) {
      window.location.href = `${basePath}index.html?login=1`;
    }
    return null;
  }

  try {
    const response = await api.getMe();
    if (response?.data) {
      updateUser(response.data);
    }
    return response?.data || null;
  } catch (error) {
    clearSession();
    showToast(error.message || "Can dang nhap de tiep tuc", "error");
    if (options.redirect !== false) {
      window.location.href = `${basePath}index.html?login=1`;
    }
    return null;
  }
}

export async function requireAdmin(options = {}) {
  const basePath = options.basePath || document.body.dataset.basePath || "../";
  const user = await requireUser({ basePath, redirect: false });
  if (!user) {
    window.location.href = `${basePath}index.html?login=1`;
    return null;
  }
  if (user.role !== "admin") {
    showToast("Ban khong co quyen vao trang admin", "error");
    window.location.href = `${basePath}index.html`;
    return null;
  }
  return user;
}
