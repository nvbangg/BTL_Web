/* ===== AUTH (DIRECT MOCK CHECK) ===== */
const AUTH_STORAGE_KEY = 'mockCurrentUser';
let authPendingEmail = '';

function getMockUsersDirectly() {
  if (typeof MOCK_DATA === 'undefined' || !MOCK_DATA) return [];
  return Array.isArray(MOCK_DATA.users) ? MOCK_DATA.users : [];
}

function normalizeEmail(email) {
  return String(email || '').trim().toLowerCase();
}

function getCurrentUser() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function setCurrentUser(user) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
}

function clearCurrentUser() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
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
    if (userName) userName.textContent = user.displayName || user.email;
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
    logoutBtn.onclick = () => {
      clearCurrentUser();
      if (dropdown) dropdown.classList.remove('show');
      applyAuthUiState();
      showToast('Da dang xuat', 'success');
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

  emailForm.onsubmit = (e) => {
    e.preventDefault();
    clearFormError(emailForm);

    const email = normalizeEmail(emailForm.email.value);
    if (!email) return;

    authPendingEmail = email;
    const existingUser = getMockUsersDirectly().find((u) => normalizeEmail(u.email) === email);

    if (existingUser) {
      const emailDisplay = document.querySelector('.auth-email-display');
      if (emailDisplay) emailDisplay.textContent = email;
      showAuthStep('password');
      passwordForm.reset();
      return;
    }

    registerForm.email.value = email;
    showAuthStep('register');
  };

  passwordForm.onsubmit = (e) => {
    e.preventDefault();
    clearFormError(passwordForm);

    const password = String(passwordForm.password.value || '');
    const matchedUser = getMockUsersDirectly().find((u) => (
      normalizeEmail(u.email) === authPendingEmail && String(u.password) === password
    ));

    if (!matchedUser) {
      showFormError(passwordForm, 'Sai mat khau, vui long thu lai.');
      return;
    }

    onLoginSuccess(matchedUser);
  };

  registerForm.onsubmit = (e) => {
    e.preventDefault();
    clearFormError(registerForm);

    const displayName = String(registerForm.displayName.value || '').trim();
    const password = String(registerForm.password.value || '');
    const confirm = String(registerForm.confirmPassword.value || '');

    if (!displayName) {
      showFormError(registerForm, 'Vui long nhap ten hien thi.');
      return;
    }
    if (password.length < 6) {
      showFormError(registerForm, 'Mat khau toi thieu 6 ky tu.');
      return;
    }
    if (password !== confirm) {
      showFormError(registerForm, 'Mat khau xac nhan khong khop.');
      return;
    }

    showFormError(registerForm, 'Email chua ton tai trong mock users. Hay dung tai khoan co san trong api-responses.js');
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
    displayName: user.displayName,
    role: user.role || 'user'
  };
  setCurrentUser(safeUser);
  closeAuth();
  applyAuthUiState();
  showToast(`Xin chao, ${safeUser.displayName || safeUser.email}`, 'success');
}
