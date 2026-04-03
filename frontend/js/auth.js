/* ===== AUTH (JWT TOKEN) ===== */
const AUTH_STORAGE_KEY = 'currentUser';
const USER_INFO_KEY = 'userInfo';
let authPendingEmail = '';

function normalizeEmail(email) {
  return String(email || '').trim().toLowerCase();
}

function getCurrentUser() {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function setCurrentUser(user) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(user));
}

function clearCurrentUser() {
  localStorage.removeItem(USER_INFO_KEY);
  localStorage.removeItem('auth_token');
  localStorage.removeItem('refresh_token');
}

function applyAuthUiState() {
  const user = getCurrentUser();
  const loginArea = document.querySelector('.header-login-area');
  const userArea = document.querySelector('.header-user-area');
  const userName = document.querySelector('.header-user-name');
  const adminLink = document.querySelector('.admin-link');

  if (user) {
    if (loginArea) loginArea.style.display = 'none';
    if (userArea) userArea.style.display = 'flex';
    if (userName) userName.textContent = user.name || user.email;
    if (adminLink) adminLink.style.display = user.role === 'admin' ? 'flex' : 'none';
  } else {
    if (loginArea) loginArea.style.display = 'flex';
    if (userArea) userArea.style.display = 'none';
    if (adminLink) adminLink.style.display = 'none';
  }
}

function initAuth() {
  const loginBtn = document.querySelector('.btn-login');
  if (loginBtn) loginBtn.onclick = openAuth;

  setupAuthForms();
  setupHeaderUserMenu();
  applyAuthUiState();
}

function setupHeaderUserMenu() {
  const userTrigger = document.querySelector('.header-user');
  const dropdown = document.querySelector('.header-user-dropdown');
  const logoutBtn = document.querySelector('.logout-btn');

  if (userTrigger && dropdown) {
    userTrigger.onclick = (e) => {
      e.stopPropagation();
      dropdown.classList.toggle('show');
    };
  }

  if (logoutBtn) {
    logoutBtn.onclick = async () => {
      await apiLogout();
      clearCurrentUser();
      if (dropdown) dropdown.classList.remove('show');
      applyAuthUiState();
      showToast('Đã đăng xuất', 'success');
    };
  }

  document.addEventListener('click', (e) => {
    if (!dropdown) return;
    if (!e.target.closest('.header-user-area')) dropdown.classList.remove('show');
  });
}

function setupAuthForms() {
  const emailForm = document.getElementById('auth-email-form');
  const passwordForm = document.getElementById('auth-password-form');
  const registerForm = document.getElementById('auth-register-form');
  const closeBtn = document.querySelector('.auth-modal-close');
  const overlay = document.querySelector('.auth-overlay');
  const backBtns = document.querySelectorAll('.auth-back');

  if (!emailForm || !passwordForm || !registerForm || !overlay) return;

  if (closeBtn) closeBtn.onclick = closeAuth;

  overlay.onclick = (e) => {
    if (e.target.classList.contains('auth-overlay')) closeAuth();
  };

  backBtns.forEach((btn) => {
    btn.onclick = () => showAuthStep('email');
  });

  emailForm.onsubmit = async (e) => {
    e.preventDefault();
    clearFormError(emailForm);

    const email = normalizeEmail(emailForm.email.value);
    if (!email) return;

    authPendingEmail = email;
    
    // Try login first (user exists)
    const emailDisplay = document.querySelector('.auth-email-display');
    if (emailDisplay) emailDisplay.textContent = email;
    showAuthStep('password');
    passwordForm.reset();
  };

  passwordForm.onsubmit = async (e) => {
    e.preventDefault();
    clearFormError(passwordForm);

    const password = String(passwordForm.password.value || '');
    if (!password) return;

    try {
      passwordForm.querySelector('button').disabled = true;
      const response = await apiLogin(authPendingEmail, password);
      const user = {
        id: response.user.id,
        email: response.user.email,
        name: response.user.name,
        role: response.user.role
      };
      onLoginSuccess(user);
    } catch (error) {
      showFormError(passwordForm, 'Sai mật khẩu, vui lòng thử lại.');
    } finally {
      passwordForm.querySelector('button').disabled = false;
    }
  };

  registerForm.onsubmit = async (e) => {
    e.preventDefault();
    clearFormError(registerForm);

    const displayName = String(registerForm.displayName.value || '').trim();
    const password = String(registerForm.password.value || '');
    const confirm = String(registerForm.confirmPassword.value || '');

    if (!displayName) {
      showFormError(registerForm, 'Vui lòng nhập tên hiển thị.');
      return;
    }
    if (password.length < 6) {
      showFormError(registerForm, 'Mật khẩu tối thiểu 6 ký tự.');
      return;
    }
    if (password !== confirm) {
      showFormError(registerForm, 'Mật khẩu xác nhận không khớp.');
      return;
    }

    try {
      registerForm.querySelector('button').disabled = true;
      const response = await apiRegister(authPendingEmail, password, displayName);
      const user = {
        id: response.id,
        email: response.email,
        name: response.name,
        role: response.role
      };
      onLoginSuccess(user);
    } catch (error) {
      const msg = error.message.includes('Email') ? 'Email đã tồn tại.' : 'Đăng ký thất bại.';
      showFormError(registerForm, msg);
    } finally {
      registerForm.querySelector('button').disabled = false;
    }
  };
}

function showFormError(form, message) {
  const errorEl = form.querySelector('.form-error');
  if (!errorEl) return;
  errorEl.textContent = message;
  errorEl.classList.add('show');
}

function clearFormError(form) {
  const errorEl = form.querySelector('.form-error');
  if (!errorEl) return;
  errorEl.textContent = '';
  errorEl.classList.remove('show');
}

function openAuth() {
  const overlay = document.querySelector('.auth-overlay');
  if (!overlay) return;
  showAuthStep('email');
  overlay.classList.add('show');
}

function closeAuth() {
  const overlay = document.querySelector('.auth-overlay');
  if (!overlay) return;
  overlay.classList.remove('show');
}

function showAuthStep(step) {
  document.querySelectorAll('.auth-step').forEach((block) => {
    block.style.display = block.dataset.step === step ? 'block' : 'none';
  });
}

function onLoginSuccess(user) {
  const safeUser = {
    id: user.id,
    email: user.email,
    name: user.name,
    role: user.role || 'user'
  };
  setCurrentUser(safeUser);
  closeAuth();
  applyAuthUiState();
  showToast(`Xin chào, ${safeUser.name || safeUser.email}`, 'success');
}
