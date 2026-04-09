import { api } from "./api.js";
import { setSession, hasToken } from "./session.js";
import { clearFieldErrors, openModal, renderFieldErrors, setButtonLoading, showToast } from "./ui.js";

let authRef = null;

function closeAuthModal() {
  if (authRef) {
    authRef.close();
    authRef = null;
  }
}

function createAuthContent(state) {
  const email = state.email || "";
  if (state.step === "login-password") {
    return `
      <button class="modal-close-btn" data-close-modal>x</button>
      <div class="auth-head">
        <img src="${state.logoPath}" alt="logo" class="auth-logo" />
        <h2>Dang nhap</h2>
        <p>${email}</p>
      </div>
      <form id="auth-password-form" class="auth-form">
        <input type="password" name="password" placeholder="Nhap mat khau" required minlength="6" />
        <button type="submit" class="btn btn-primary btn-block">Tiep tuc</button>
      </form>
      <button id="go-back-email" class="auth-link-btn">Nhap email khac</button>
      <button id="go-register" class="auth-link-btn">Chua co tai khoan? Dang ky</button>
    `;
  }

  if (state.step === "register") {
    return `
      <button class="modal-close-btn" data-close-modal>x</button>
      <div class="auth-head">
        <img src="${state.logoPath}" alt="logo" class="auth-logo" />
        <h2>Dang ky</h2>
        <p>Tao tai khoan moi</p>
      </div>
      <form id="auth-register-form" class="auth-form">
        <input type="text" name="name" placeholder="Ten hien thi" required />
        <input type="email" name="email" placeholder="Email" value="${email}" required />
        <input type="password" name="password" placeholder="Mat khau" required minlength="6" />
        <input type="password" name="confirmPassword" placeholder="Xac nhan mat khau" required minlength="6" />
        <button type="submit" class="btn btn-primary btn-block">Dang ky</button>
      </form>
      <button id="go-login-email" class="auth-link-btn">Da co tai khoan? Dang nhap</button>
    `;
  }

  return `
    <button class="modal-close-btn" data-close-modal>x</button>
    <div class="auth-head">
      <img src="${state.logoPath}" alt="logo" class="auth-logo" />
      <h2>Dang nhap</h2>
      <p>Nhap email de tiep tuc</p>
    </div>
    <form id="auth-email-form" class="auth-form">
      <input type="email" name="email" placeholder="Nhap email" value="${email}" required />
      <button type="submit" class="btn btn-primary btn-block">Tiep tuc</button>
    </form>
    <button id="go-register" class="auth-link-btn">Chua co tai khoan? Dang ky</button>
  `;
}

function bindAuthEvents(state) {
  const modal = authRef?.modal;
  if (!modal) {
    return;
  }

  const emailForm = modal.querySelector("#auth-email-form");
  if (emailForm) {
    emailForm.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = new FormData(emailForm);
      state.email = String(data.get("email") || "").trim();
      if (!state.email) {
        showToast("Email khong duoc de trong", "error");
        return;
      }
      state.step = "login-password";
      renderAuthState(state);
    });
  }

  const passwordForm = modal.querySelector("#auth-password-form");
  if (passwordForm) {
    passwordForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      clearFieldErrors(passwordForm);
      const submit = passwordForm.querySelector("button[type=submit]");
      const password = String(new FormData(passwordForm).get("password") || "").trim();
      if (password.length < 6) {
        showToast("Mat khau toi thieu 6 ky tu", "error");
        return;
      }

      try {
        setButtonLoading(submit, true);
        const response = await api.login({ email: state.email, password });
        const token = response?.data?.accessToken;
        const user = response?.data?.user;
        if (!token || !user) {
          showToast("Dang nhap that bai", "error");
          return;
        }
        setSession(token, user);
        showToast(response.message || "Dang nhap thanh cong", "success");
        closeAuthModal();
      } catch (error) {
        renderFieldErrors(passwordForm, error.errors);
        showToast(error.message || "Dang nhap that bai", "error");
      } finally {
        setButtonLoading(submit, false);
      }
    });

    const back = modal.querySelector("#go-back-email");
    back?.addEventListener("click", () => {
      state.step = "login-email";
      renderAuthState(state);
    });
  }

  const registerForm = modal.querySelector("#auth-register-form");
  if (registerForm) {
    registerForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      clearFieldErrors(registerForm);
      const submit = registerForm.querySelector("button[type=submit]");
      const form = new FormData(registerForm);
      const name = String(form.get("name") || "").trim();
      const email = String(form.get("email") || "").trim();
      const password = String(form.get("password") || "").trim();
      const confirmPassword = String(form.get("confirmPassword") || "").trim();

      if (!name || !email) {
        showToast("Ten va email la bat buoc", "error");
        return;
      }
      if (password.length < 6) {
        showToast("Mat khau toi thieu 6 ky tu", "error");
        return;
      }
      if (password !== confirmPassword) {
        showToast("Xac nhan mat khau khong khop", "error");
        return;
      }

      try {
        setButtonLoading(submit, true);
        const response = await api.register({ name, email, password });
        const token = response?.data?.accessToken;
        const user = response?.data?.user;
        if (!token || !user) {
          showToast("Dang ky that bai", "error");
          return;
        }
        setSession(token, user);
        showToast(response.message || "Dang ky thanh cong", "success");
        closeAuthModal();
      } catch (error) {
        renderFieldErrors(registerForm, error.errors);
        showToast(error.message || "Dang ky that bai", "error");
      } finally {
        setButtonLoading(submit, false);
      }
    });
  }

  const toRegisterButtons = modal.querySelectorAll("#go-register");
  toRegisterButtons.forEach((button) => {
    button.addEventListener("click", () => {
      state.step = "register";
      renderAuthState(state);
    });
  });

  const toLoginButton = modal.querySelector("#go-login-email");
  toLoginButton?.addEventListener("click", () => {
    state.step = "login-email";
    renderAuthState(state);
  });
}

function renderAuthState(state) {
  if (!authRef?.modal) {
    return;
  }
  authRef.modal.innerHTML = createAuthContent(state);
  bindAuthEvents(state);
  authRef.modal.querySelectorAll("[data-close-modal]").forEach((button) => {
    button.addEventListener("click", closeAuthModal);
  });
}

export function openAuthModal(step = "login-email", email = "") {
  closeAuthModal();
  const basePath = document.body.dataset.basePath || "./";
  const state = {
    step,
    email,
    logoPath: `${basePath}assets/logo.svg`
  };

  authRef = openModal("", { className: "auth-modal" });
  renderAuthState(state);
}

export function openChangePasswordModal() {
  if (!hasToken()) {
    openAuthModal("login-email");
    return;
  }

  const ref = openModal(
    `
      <button class="modal-close-btn" data-close-modal>x</button>
      <div class="auth-head">
        <h2>Doi mat khau</h2>
        <p>Nhap mat khau hien tai va mat khau moi</p>
      </div>
      <form id="change-password-form" class="auth-form">
        <input type="password" name="currentPassword" placeholder="Mat khau hien tai" required minlength="6" />
        <input type="password" name="newPassword" placeholder="Mat khau moi" required minlength="6" />
        <input type="password" name="confirmPassword" placeholder="Xac nhan mat khau moi" required minlength="6" />
        <button type="submit" class="btn btn-primary btn-block">Xac nhan</button>
      </form>
    `,
    { className: "auth-modal" }
  );

  const form = ref.modal.querySelector("#change-password-form");
  form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    const submit = form.querySelector("button[type=submit]");
    const data = new FormData(form);
    const currentPassword = String(data.get("currentPassword") || "").trim();
    const newPassword = String(data.get("newPassword") || "").trim();
    const confirmPassword = String(data.get("confirmPassword") || "").trim();

    if (newPassword.length < 6) {
      showToast("Mat khau moi toi thieu 6 ky tu", "error");
      return;
    }

    if (newPassword !== confirmPassword) {
      showToast("Xac nhan mat khau moi khong khop", "error");
      return;
    }

    if (newPassword === currentPassword) {
      showToast("Mat khau moi khong duoc trung mat khau cu", "error");
      return;
    }

    try {
      setButtonLoading(submit, true);
      const response = await api.changePassword({ currentPassword, newPassword });
      showToast(response.message || "Doi mat khau thanh cong", "success");
      ref.close();
    } catch (error) {
      renderFieldErrors(form, error.errors);
      showToast(error.message || "Doi mat khau that bai", "error");
    } finally {
      setButtonLoading(submit, false);
    }
  });
}
