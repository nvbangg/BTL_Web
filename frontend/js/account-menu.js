import { api, refreshCurrentUser } from "./api.js";
import { openAuthModal, openChangePasswordModal } from "./auth-modal.js";
import { clearSession, getUser, hasToken } from "./session.js";
import { showToast } from "./ui.js";
import { getDisplayName } from "./utils.js";

function createMenuHtml(user, basePath) {
  if (!user) {
    return `
      <button class="menu-item" data-action="login">Dang nhap</button>
      <button class="menu-item" data-action="register">Dang ky</button>
    `;
  }

  return `
    <a class="menu-item" href="${basePath}cart.html">Gio hang</a>
    <a class="menu-item" href="${basePath}orders.html">Don hang</a>
    <button class="menu-item" data-action="change-password">Doi mat khau</button>
    ${user.role === "admin" ? `<a class=\"menu-item\" href=\"${basePath}admin/index.html\">Trang Admin</a>` : ""}
    <button class="menu-item danger" data-action="logout">Dang xuat</button>
  `;
}

function closeMenu(menu) {
  menu.classList.add("hidden");
}

export async function initAccountMenu(options = {}) {
  const trigger = document.querySelector(options.triggerSelector || "#account-trigger");
  const menu = document.querySelector(options.menuSelector || "#account-menu");
  const basePath = options.basePath || document.body.dataset.basePath || "./";

  if (!trigger || !menu) {
    return;
  }

  const render = () => {
    const user = getUser();
    trigger.textContent = getDisplayName(user);
    menu.innerHTML = createMenuHtml(user, basePath);
  };

  render();

  if (hasToken()) {
    try {
      await refreshCurrentUser();
      render();
    } catch (_error) {
      // Unauthorized handled in api layer.
    }
  }

  trigger.addEventListener("click", (event) => {
    event.stopPropagation();
    menu.classList.toggle("hidden");
  });

  document.addEventListener("click", () => closeMenu(menu));
  menu.addEventListener("click", async (event) => {
    const target = event.target.closest("[data-action]");
    if (!target) {
      return;
    }

    const action = target.dataset.action;
    if (action === "login") {
      openAuthModal("login-email");
    }
    if (action === "register") {
      openAuthModal("register");
    }
    if (action === "change-password") {
      openChangePasswordModal();
    }
    if (action === "logout") {
      try {
        await api.logout();
      } catch (_error) {
        // Ignore API failure and clear local session anyway.
      }
      clearSession();
      showToast("Da dang xuat", "success");
    }
    closeMenu(menu);
  });

  window.addEventListener("app:login", render);
  window.addEventListener("app:logout", render);
  window.addEventListener("app:user-updated", render);

  window.addEventListener("app:unauthorized", (event) => {
    showToast(event.detail?.message || "Vui long dang nhap", "error");
    openAuthModal("login-email");
    render();
  });
}
