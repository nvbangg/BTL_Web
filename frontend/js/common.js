(function (window, document) {
  const TOKEN_KEY = "fashon.accessToken";
  const USER_KEY = "fashon.user";

  const STATUS_TEXT = {
    pending: "Đang chờ",
    processing: "Đang xử lý",
    shipped: "Đang giao",
    delivered: "Đã giao",
    cancelled: "Đã hủy"
  };

  const STATUS_CLASS = {
    pending: "pending",
    processing: "processing",
    shipped: "shipping",
    delivered: "delivered",
    cancelled: "cancelled"
  };

  const GENDER_TEXT = {
    male: "Nam",
    female: "Nữ",
    unisex: "Unisex",
    nam: "Nam",
    nữ: "Nữ",
    nu: "Nữ"
  };

  const ICON_STYLESHEET_URL = "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css";
  const ICON_MAP = {
    search: "bi-search",
    user: "bi-person",
    account: "bi-person-fill",
    userCircle: "bi-person-circle",
    chevronDown: "bi-chevron-down",
    chevronUp: "bi-chevron-up",
    chevronLeft: "bi-chevron-left",
    chevronRight: "bi-chevron-right",
    cart: "bi-cart3",
    orders: "bi-box-seam",
    lock: "bi-lock",
    admin: "bi-grid",
    logout: "bi-box-arrow-right",
    chart: "bi-bar-chart",
    users: "bi-people",
    bag: "bi-handbag",
    truck: "bi-truck",
    shield: "bi-shield-check",
    rotate: "bi-arrow-repeat",
    mapPin: "bi-geo-alt",
    phone: "bi-telephone",
    mail: "bi-envelope",
    facebook: "bi-facebook",
    messenger: "bi-messenger",
    tiktok: "bi-tiktok",
    chat: "bi-chat-left-text",
    close: "bi-x-lg",
    eye: "bi-eye",
    eyeOff: "bi-eye-slash",
    arrowRight: "bi-arrow-right",
    filter: "bi-funnel",
    plus: "bi-plus-lg",
    pencil: "bi-pencil",
    trash: "bi-trash",
    calendar: "bi-calendar2-week",
    wallet: "bi-wallet2",
    checkCircle: "bi-check-circle",
    alertCircle: "bi-exclamation-circle",
    clock: "bi-clock",
    box: "bi-box"
  };

  let iconCssInjected = false;

  let unauthorizedBound = false;

  function ensureIconLibrary() {
    if (iconCssInjected) return;

    if (document.querySelector('link[data-icon-lib="bootstrap-icons"]')) {
      iconCssInjected = true;
      return;
    }

    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = ICON_STYLESHEET_URL;
    link.setAttribute("data-icon-lib", "bootstrap-icons");
    document.head.appendChild(link);
    iconCssInjected = true;
  }

  function icon(name, extraClass) {
    const iconClass = ICON_MAP[name] || name || "bi-circle";
    const classes = "bi " + iconClass + (extraClass ? " " + extraClass : "");
    return '<i class="' + classes + '" aria-hidden="true"></i>';
  }

  function socialIcon(name) {
    const icons = {
      facebook: '' +
        '<svg class="social-icon-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18" style="display:block;opacity:1;visibility:visible" aria-hidden="true" focusable="false">' +
        '  <path fill="currentColor" d="M13.5 22V12.95h3l.45-3.53H13.5V7.17c0-1.02.28-1.71 1.74-1.71H17V2.31c-.3-.04-1.33-.13-2.54-.13-2.52 0-4.24 1.54-4.24 4.37v2.87H7.37v3.53h2.85V22h3.28z" />' +
        '</svg>',
      messenger: '' +
        '<svg class="social-icon-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18" style="display:block;opacity:1;visibility:visible" aria-hidden="true" focusable="false">' +
        '  <path fill="currentColor" d="M12 2C6.48 2 2 6.03 2 11c0 2.83 1.45 5.36 3.7 7.01V22l3.4-1.87c.9.25 1.87.38 2.9.38 5.52 0 10-4.03 10-9S17.52 2 12 2zm1.07 11.33-2.55-2.72-4.91 2.72 5.45-5.78 2.58 2.72 4.86-2.72-5.43 5.78z" />' +
        '</svg>',
      tiktok: '' +
        '<svg class="social-icon-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18" style="display:block;opacity:1;visibility:visible" aria-hidden="true" focusable="false">' +
        '  <path fill="currentColor" d="M14.5 2h2.65c.22 1.78 1.24 3.18 2.85 3.75v2.77c-1.5-.05-2.86-.57-4-1.45v6.07c0 3.44-2.56 6.11-6.09 6.11A6.1 6.1 0 0 1 3.8 13.2a6.08 6.08 0 0 1 6.11-6.05c.37 0 .72.03 1.07.1v2.9a3.2 3.2 0 0 0-1.07-.18A3.23 3.23 0 0 0 6.7 13.2a3.23 3.23 0 0 0 3.21 3.23A3.25 3.25 0 0 0 13.14 13V2h1.36z" />' +
        '</svg>'
    };

    return icons[name] || icon(name);
  }

  function escapeHtml(value) {
    return String(value || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/\"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatPrice(value) {
    return Number(value || 0).toLocaleString("vi-VN") + "đ";
  }

  function formatDate(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.toLocaleDateString("vi-VN");
  }

  function getStatusText(status) {
    return STATUS_TEXT[status] || status;
  }

  function getStatusClass(status) {
    return STATUS_CLASS[status] || "pending";
  }

  function getGenderText(gender) {
    const normalized = String(gender || "").trim().toLowerCase();
    if (!normalized) return "";
    return GENDER_TEXT[normalized] || gender;
  }

  function getQueryParams() {
    return new URLSearchParams(window.location.search);
  }

  function updateQuery(updates) {
    const params = getQueryParams();
    Object.keys(updates || {}).forEach(function (key) {
      const value = updates[key];
      if (value === undefined || value === null || value === "") {
        params.delete(key);
      } else {
        params.set(key, String(value));
      }
    });
    const query = params.toString();
    window.location.href = window.location.pathname + (query ? "?" + query : "");
  }

  function showToast(message, type) {
    const toastType = type || "info";
    let container = document.querySelector(".toast-container");
    if (!container) {
      container = document.createElement("div");
      container.className = "toast-container";
      document.body.appendChild(container);
    }

    const iconMap = {
      success: icon("checkCircle"),
      error: icon("close"),
      warning: icon("alertCircle"),
      info: icon("chat")
    };

    const toast = document.createElement("div");
    toast.className = "toast " + toastType;
    toast.innerHTML =
      '<span class="toast-icon">' + (iconMap[toastType] || iconMap.info) + "</span>" +
      "<span>" + escapeHtml(message) + "</span>";

    container.appendChild(toast);
    setTimeout(function () {
      toast.classList.add("removing");
      setTimeout(function () {
        if (toast.parentElement) {
          toast.parentElement.removeChild(toast);
        }
      }, 280);
    }, 2600);
  }

  function renderPagination(container, page, pageSize, total, onChange) {
    if (!container) return;
    const totalPages = Math.max(1, Math.ceil(Number(total || 0) / Number(pageSize || 1)));
    if (totalPages <= 1) {
      container.innerHTML = "";
      return;
    }

    const current = Number(page || 1);
    let html = '<div class="pagination">';
    html += '<button ' + (current <= 1 ? "disabled" : "") + ' data-page="' + (current - 1) + '">' + icon("chevronLeft") + "</button>";
    for (let i = 1; i <= totalPages; i += 1) {
      html += '<button class="' + (i === current ? "active" : "") + '" data-page="' + i + '">' + i + "</button>";
    }
    html += '<button ' + (current >= totalPages ? "disabled" : "") + ' data-page="' + (current + 1) + '">' + icon("chevronRight") + "</button>";
    html += "</div>";

    container.innerHTML = html;
    Array.from(container.querySelectorAll("button[data-page]")).forEach(function (button) {
      button.addEventListener("click", function () {
        if (button.disabled) return;
        const nextPage = Number(button.getAttribute("data-page"));
        if (typeof onChange === "function") {
          onChange(nextPage);
        }
      });
    });
  }

  function getToken() {
    return localStorage.getItem(TOKEN_KEY) || "";
  }

  function getUser() {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  function getDisplayUsername(user) {
    const source = user || getUser();
    if (!source) return "";
    if (source.email && source.email.includes("@")) {
      return source.email.split("@")[0];
    }
    return source.name || "Tài khoản";
  }

  function saveSession(data) {
    if (!data || !data.accessToken) return;
    localStorage.setItem(TOKEN_KEY, data.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify({
      id: data.id,
      email: data.email,
      name: data.name,
      role: data.role
    }));
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function renderHeader(options) {
    ensureIconLibrary();

    const config = options || {};
    const showSearch = config.showSearch !== false;
    const keyword = config.keyword || "";
    const root = document.getElementById("app-header");
    if (!root) return;

    root.innerHTML = '' +
      '<header class="header">' +
      '  <div class="container">' +
      '    <a href="index.html" class="header-logo" id="header-logo-link" aria-label="Fashon Shop">' +
      '      <img class="header-logo-image" src="' + escapeHtml(AppConfig.buildImageUrl("logo-and-text.svg")) + '" alt="Fashon Shop" onerror="this.style.display=\'none\'">' +
      '    </a>' +
      (showSearch
        ? '    <form class="header-search" id="header-search-form">' +
          '      <span class="header-search-icon">' + icon("search") + "</span>" +
          '      <input type="text" id="header-search-input" placeholder="Tìm kiếm quần áo..." value="' + escapeHtml(keyword) + '">' +
          '      <button type="submit" class="header-search-btn">' + icon("search") + '<span>Tìm kiếm</span></button>' +
          '    </form>'
        : '    <div class="header-spacer"></div>') +
      '    <div class="header-actions">' +
      '      <div class="header-login-area" id="header-login-area">' +
      '        <button class="btn-login" id="btn-open-login" type="button" aria-label="Đăng nhập"><span>Đăng nhập</span></button>' +
      '      </div>' +
      '      <div class="header-user-area" id="header-user-area" style="display:none">' +
      '        <button class="header-user" id="header-user-trigger" type="button" aria-label="Tài khoản">' +
      '          <span class="header-user-avatar">' + icon("account") + '</span>' +
      '          <span class="header-user-name" id="header-user-name"></span>' +
      '          <span class="header-user-caret">' + icon("chevronDown") + '</span>' +
      '        </button>' +
      '        <div class="header-user-dropdown" id="header-user-dropdown">' +
      '          <a href="cart.html" class="cart-link">' + icon("cart") + '<span>Giỏ hàng</span></a>' +
      '          <a href="orders.html">' + icon("orders") + '<span>Đơn hàng</span></a>' +
      '          <button type="button" id="btn-open-change-password">' + icon("lock") + '<span>Đổi mật khẩu</span></button>' +
      '          <a href="admin.html" class="admin-link" id="header-admin-link" style="display:none">' + icon("admin") + '<span>Trang Admin</span></a>' +
      '          <div class="dropdown-divider"></div>' +
      '          <button class="logout-btn" type="button" id="btn-header-logout">' + icon("logout") + '<span>Đăng xuất</span></button>' +
      '        </div>' +
      '      </div>' +
      '    </div>' +
      '  </div>' +
      '</header>';

    const searchForm = document.getElementById("header-search-form");
    if (searchForm) {
      searchForm.addEventListener("submit", function (event) {
        event.preventDefault();
        const input = document.getElementById("header-search-input");
        const value = (input && input.value ? input.value : "").trim();
        window.location.href = "index.html" + (value ? "?keyword=" + encodeURIComponent(value) : "");
      });
    }

    const openLogin = document.getElementById("btn-open-login");
    if (openLogin) {
      openLogin.addEventListener("click", function () {
        openAuth("login");
      });
    }

    const userTrigger = document.getElementById("header-user-trigger");
    const dropdown = document.getElementById("header-user-dropdown");
    if (userTrigger && dropdown) {
      userTrigger.addEventListener("click", function (event) {
        event.stopPropagation();
        dropdown.classList.toggle("show");
      });
      document.addEventListener("click", function () {
        dropdown.classList.remove("show");
      });
    }

    const logoutButton = document.getElementById("btn-header-logout");
    if (logoutButton) {
      logoutButton.addEventListener("click", async function () {
        try {
          await AppApi.authLogout();
        } catch {
          // API logout only returns message, ignore network fail and clear local session.
        }
        clearSession();
        refreshAuthUi();
        showToast("Đã đăng xuất", "success");
      });
    }

    const changePasswordButton = document.getElementById("btn-open-change-password");
    if (changePasswordButton) {
      changePasswordButton.addEventListener("click", function () {
        openAuth("change-password");
      });
    }

    refreshAuthUi();
  }

  function renderWhyChoose() {
    const root = document.getElementById("app-why");
    if (!root) return;
    root.innerHTML = '' +
      '<section class="why-choose">' +
      '  <div class="container">' +
      '    <div class="why-choose-box">' +
      '      <h3 class="why-choose-title">Tại sao chọn Fashon Shop?</h3>' +
      '      <p class="why-choose-desc">Fashon Shop cam kết mang đến những bộ sưu tập thời trang cao cấp từ các thương hiệu nổi tiếng.</p>' +
      '      <p class="why-choose-desc">Mỗi sản phẩm được lựa chọn kỹ lưỡng, đảm bảo cả về chất liệu, thiết kế, mang lại cho bạn sự sang trọng.</p>' +
      '      <p class="why-choose-desc">Hãy cùng khám phá và định hình phong cách cá nhân đẳng cấp nhất của riêng bạn tại Fashon Shop.</p>' +
      '      <div class="why-choose-features">' +
      '        <div class="why-feature"><div class="why-feature-icon">' + icon("truck") + '</div><div class="why-feature-text"><strong>Giao hàng toàn quốc</strong><span>Miễn phí đơn từ 500k</span></div></div>' +
      '        <div class="why-feature"><div class="why-feature-icon">' + icon("shield") + '</div><div class="why-feature-text"><strong>Chất lượng đảm bảo</strong><span>Sản phẩm đúng mô tả</span></div></div>' +
      '        <div class="why-feature"><div class="why-feature-icon">' + icon("rotate") + '</div><div class="why-feature-text"><strong>Đổi trả dễ dàng</strong><span>Hỗ trợ đổi size trong 7 ngày</span></div></div>' +
      '      </div>' +
      '    </div>' +
      '  </div>' +
      '</section>';
  }

  function renderFooter() {
    const root = document.getElementById("app-footer");
    if (!root) return;
    const socialLink = "https://github.com/nvbangg";
    root.innerHTML = '' +
      '<footer class="footer">' +
      '  <div class="container">' +
      '    <div class="footer-grid">' +
      '      <div class="footer-brand"><h3>Fashon Shop</h3><p>© 2026 Bản quyền thuộc về Fashon Shop.</p><p>Phát triển bởi: Nguyễn Văn Bằng</p></div>' +
      '      <div class="footer-col"><h4>Thông tin liên hệ</h4><p class="footer-inline"><span>' + icon("mapPin") + '</span><span>Học viện Công nghệ Bưu chính Viễn thông, Hà Nội</span></p><p class="footer-inline"><span>' + icon("phone") + '</span><span>0987.654.321</span></p><p class="footer-inline"><span>' + icon("mail") + '</span><span>contact@fashonshop.vn</span></p></div>' +
      '      <div class="footer-col"><h4>Kết nối với chúng tôi</h4><div class="footer-connect-lines"><p class="footer-inline"><span>' + icon("facebook") + '</span><span>Facebook: <a class="footer-connect-link" href="' + socialLink + '" target="_blank" rel="noreferrer">facebbook.com/fashonshop</a></span></p><p class="footer-inline"><span>' + icon("tiktok") + '</span><span>TikTok: <a class="footer-connect-link" href="' + socialLink + '" target="_blank" rel="noreferrer">tiktok.com/fahonshop</a></span></p></div></div>' +
      '    </div>' +
      '  </div>' +
      '</footer>';
  }

  function renderAuthModal() {
    const root = document.getElementById("app-auth");
    if (!root) return;

    root.innerHTML = '' +
      '<div class="auth-overlay" id="auth-overlay">' +
      '  <div class="auth-modal">' +
      '    <div class="auth-modal-head">' +
      '      <div class="auth-brand">' +
      '        <img src="' + escapeHtml(AppConfig.buildImageUrl("logo.svg")) + '" alt="Fashon Shop" onerror="this.style.display=\'none\'">' +
      '        <span>Fashon Shop</span>' +
      '      </div>' +
      '      <button class="auth-modal-close" type="button" id="auth-close-btn">' + icon("close") + "</button>" +
      '    </div>' +
      '    <div class="auth-step" data-step="login" style="display:block">' +
      '      <h2>Đăng nhập</h2>' +
      '      <p class="subtitle">Nhập email và mật khẩu để đăng nhập.</p>' +
      '      <form id="auth-login-form">' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("mail") + '</span>' +
      '            <input type="email" name="email" placeholder="Nhập email" required>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("lock") + '</span>' +
      '            <input type="password" id="auth-login-password" name="password" placeholder="Nhập mật khẩu" required>' +
      '            <button type="button" class="auth-toggle-password" data-toggle-password="auth-login-password" aria-label="Hiện mật khẩu">' + icon("eye") + '</button>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-error"></div>' +
      '        <button type="submit" class="btn btn-yellow btn-block btn-lg btn-with-icon"><span>Tiếp tục</span>' + icon("arrowRight") + '</button>' +
      '      </form>' +
      '      <p class="auth-switch">Chưa có tài khoản?<button type="button" id="btn-switch-to-register" class="btn-link">Đăng ký</button></p>' +
      '    </div>' +
      '    <div class="auth-step" data-step="register" style="display:none">' +
      '      <h2>Đăng ký</h2>' +
      '      <p class="subtitle">Nhập thông tin để tạo tài khoản mới.</p>' +
      '      <form id="auth-register-form">' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("mail") + '</span>' +
      '            <input type="email" name="email" placeholder="Nhập email" required>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("user") + '</span>' +
      '            <input type="text" name="name" placeholder="Nhập tên hiển thị" required>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("lock") + '</span>' +
      '            <input type="password" id="auth-register-password" name="password" placeholder="Nhập mật khẩu" required>' +
      '            <button type="button" class="auth-toggle-password" data-toggle-password="auth-register-password" aria-label="Hiện mật khẩu">' + icon("eye") + '</button>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("lock") + '</span>' +
      '            <input type="password" id="auth-register-confirm" name="confirmPassword" placeholder="Xác nhận lại mật khẩu" required>' +
      '            <button type="button" class="auth-toggle-password" data-toggle-password="auth-register-confirm" aria-label="Hiện mật khẩu">' + icon("eye") + '</button>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-error"></div>' +
      '        <button type="submit" class="btn btn-yellow btn-block btn-lg btn-with-icon"><span>Đăng ký</span>' + icon("arrowRight") + '</button>' +
      '      </form>' +
      '      <p class="auth-switch">Đã có tài khoản?<button type="button" id="btn-switch-to-login" class="btn-link">Đăng nhập</button></p>' +
      '    </div>' +
      '    <div class="auth-step" data-step="change-password" style="display:none">' +
      '      <h2>Đổi mật khẩu</h2>' +
      '      <p class="subtitle">Nhập mật khẩu hiện tại và mật khẩu mới của bạn.</p>' +
      '      <form id="auth-change-password-form">' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("lock") + '</span>' +
      '            <input type="password" id="auth-current-password" name="currentPassword" placeholder="Mật khẩu hiện tại" required>' +
      '            <button type="button" class="auth-toggle-password" data-toggle-password="auth-current-password" aria-label="Hiện mật khẩu">' + icon("eye") + '</button>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("lock") + '</span>' +
      '            <input type="password" id="auth-new-password" name="newPassword" placeholder="Mật khẩu mới" required>' +
      '            <button type="button" class="auth-toggle-password" data-toggle-password="auth-new-password" aria-label="Hiện mật khẩu">' + icon("eye") + '</button>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-group">' +
      '          <div class="auth-input-wrap">' +
      '            <span class="auth-input-icon">' + icon("lock") + '</span>' +
      '            <input type="password" id="auth-confirm-password" name="confirmPassword" placeholder="Xác nhận mật khẩu mới" required>' +
      '            <button type="button" class="auth-toggle-password" data-toggle-password="auth-confirm-password" aria-label="Hiện mật khẩu">' + icon("eye") + '</button>' +
      '          </div>' +
      '        </div>' +
      '        <div class="form-error"></div>' +
      '        <button type="submit" class="btn btn-yellow btn-block btn-lg btn-with-icon"><span>Xác nhận</span>' + icon("checkCircle") + '</button>' +
      '      </form>' +
      '    </div>' +
      '  </div>' +
      '</div>';

    bindAuthModalEvents();
  }

  function setAuthStep(step) {
    Array.from(document.querySelectorAll(".auth-step")).forEach(function (node) {
      node.style.display = node.getAttribute("data-step") === step ? "block" : "none";
    });
  }

  function openAuth(step) {
    const targetStep = step || "login";
    const overlay = document.getElementById("auth-overlay");
    if (!overlay) return;
    setAuthStep(targetStep);
    overlay.classList.add("show");
  }

  function closeAuth() {
    const overlay = document.getElementById("auth-overlay");
    if (!overlay) return;
    overlay.classList.remove("show");
  }

  function setFormError(form, message) {
    if (!form) return;
    const error = form.querySelector(".form-error");
    if (!error) return;
    error.textContent = message || "";
    if (message) {
      error.classList.add("show");
    } else {
      error.classList.remove("show");
    }
  }

  async function bootstrapAuth() {
    const token = getToken();
    if (!token) {
      refreshAuthUi();
      return null;
    }

    try {
      const me = await AppApi.getMe();
      localStorage.setItem(USER_KEY, JSON.stringify(me));
      refreshAuthUi();
      return me;
    } catch {
      clearSession();
      refreshAuthUi();
      return null;
    }
  }

  function refreshAuthUi() {
    const loginArea = document.getElementById("header-login-area");
    const userArea = document.getElementById("header-user-area");
    const userName = document.getElementById("header-user-name");
    const adminLink = document.getElementById("header-admin-link");
    const user = getUser();

    if (!user) {
      if (loginArea) loginArea.style.display = "flex";
      if (userArea) userArea.style.display = "none";
      return;
    }

    if (loginArea) loginArea.style.display = "none";
    if (userArea) userArea.style.display = "flex";
    if (userName) userName.textContent = getDisplayUsername(user);
    if (adminLink) adminLink.style.display = user.role === "admin" ? "flex" : "none";
  }

  function bindAuthModalEvents() {
    const overlay = document.getElementById("auth-overlay");
    const closeButton = document.getElementById("auth-close-btn");

    if (closeButton) {
      closeButton.addEventListener("click", closeAuth);
    }

    if (overlay) {
      overlay.addEventListener("click", function (event) {
        if (event.target === overlay) {
          closeAuth();
        }
      });
    }

    Array.from(document.querySelectorAll(".auth-toggle-password")).forEach(function (button) {
      button.addEventListener("click", function () {
        const inputId = button.getAttribute("data-toggle-password");
        const input = inputId ? document.getElementById(inputId) : null;
        if (!input) return;

        const isPassword = input.type === "password";
        input.type = isPassword ? "text" : "password";
        button.innerHTML = isPassword ? icon("eyeOff") : icon("eye");
      });
    });

    const switchToRegisterButton = document.getElementById("btn-switch-to-register");
    if (switchToRegisterButton) {
      switchToRegisterButton.addEventListener("click", function () {
        setAuthStep("register");
      });
    }

    const switchToLoginButton = document.getElementById("btn-switch-to-login");
    if (switchToLoginButton) {
      switchToLoginButton.addEventListener("click", function () {
        setAuthStep("login");
      });
    }

    const loginForm = document.getElementById("auth-login-form");
    if (loginForm) {
      loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        setFormError(loginForm, "");
        const formData = new FormData(loginForm);
        const email = String(formData.get("email") || "").trim().toLowerCase();
        const password = String(formData.get("password") || "");

        if (!email) {
          setFormError(loginForm, "Email không hợp lệ");
          return;
        }

        if (!password) {
          setFormError(loginForm, "Mật khẩu là bắt buộc");
          return;
        }

        try {
          const loginData = await AppApi.authLogin(email, password);
          saveSession(loginData);
          refreshAuthUi();
          closeAuth();
          document.dispatchEvent(new CustomEvent("app:auth-changed"));
          showToast("Đăng nhập thành công", "success");
        } catch (error) {
          const firstError = error.errors && error.errors.length ? error.errors[0].message : "Đăng nhập thất bại";
          setFormError(loginForm, firstError);
        }
      });
    }

    const registerForm = document.getElementById("auth-register-form");
    if (registerForm) {
      registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        setFormError(registerForm, "");

        const formData = new FormData(registerForm);
        const email = String(formData.get("email") || "").trim().toLowerCase();
        const name = String(formData.get("name") || "").trim();
        const password = String(formData.get("password") || "");
        const confirmPassword = String(formData.get("confirmPassword") || "");

        if (!email) {
          setFormError(registerForm, "Email không hợp lệ");
          return;
        }

        if (!name) {
          setFormError(registerForm, "Tên hiển thị là bắt buộc");
          return;
        }

        if (password.length < 6) {
          setFormError(registerForm, "Mật khẩu phải có ít nhất 6 ký tự");
          return;
        }

        if (password !== confirmPassword) {
          setFormError(registerForm, "Xác nhận mật khẩu không khớp");
          return;
        }

        try {
          const registerData = await AppApi.authRegister(email, password, name);
          saveSession(registerData);
          refreshAuthUi();
          closeAuth();
          document.dispatchEvent(new CustomEvent("app:auth-changed"));
          showToast("Đăng ký thành công", "success");
        } catch (error) {
          const firstError = error.errors && error.errors.length ? error.errors[0].message : "Đăng ký thất bại";
          setFormError(registerForm, firstError);
        }
      });
    }

    const changePasswordForm = document.getElementById("auth-change-password-form");
    if (changePasswordForm) {
      changePasswordForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        setFormError(changePasswordForm, "");

        const formData = new FormData(changePasswordForm);
        const currentPassword = String(formData.get("currentPassword") || "");
        const newPassword = String(formData.get("newPassword") || "");
        const confirmPassword = String(formData.get("confirmPassword") || "");

        if (newPassword.length < 6) {
          setFormError(changePasswordForm, "Mật khẩu mới phải có ít nhất 6 ký tự");
          return;
        }
        if (newPassword !== confirmPassword) {
          setFormError(changePasswordForm, "Xác nhận mật khẩu mới không khớp");
          return;
        }

        try {
          await AppApi.changePassword({ currentPassword: currentPassword, newPassword: newPassword });
          closeAuth();
          showToast("Cập nhật mật khẩu thành công", "success");
        } catch (error) {
          const firstError = error.errors && error.errors.length ? error.errors[0].message : "Đổi mật khẩu thất bại";
          setFormError(changePasswordForm, firstError);
        }
      });
    }
  }

  function renderAdminSidebar(activeTab) {
    const root = document.getElementById("app-admin-sidebar");
    if (!root) return;
    const accountName = escapeHtml(getDisplayUsername(getUser()) || "Tài khoản");

    root.innerHTML = '' +
      '<aside class="admin-sidebar">' +
      '  <nav class="admin-sidebar-nav">' +
      '    <a href="admin.html" class="admin-nav-link ' + (activeTab === "statistics" ? "active" : "") + '"><span class="admin-nav-icon">' + icon("chart") + '</span><span class="admin-nav-text">Xem thống kê</span></a>' +
      '    <a href="admin-orders.html" class="admin-nav-link ' + (activeTab === "orders" ? "active" : "") + '"><span class="admin-nav-icon">' + icon("orders") + '</span><span class="admin-nav-text">Quản lý đơn hàng</span><span class="admin-nav-badge" id="admin-order-badge" style="display:none"></span></a>' +
      '    <a href="admin-products.html" class="admin-nav-link ' + (activeTab === "products" ? "active" : "") + '"><span class="admin-nav-icon">' + icon("bag") + '</span><span class="admin-nav-text">Quản lý sản phẩm</span></a>' +
      '    <a href="admin-users.html" class="admin-nav-link ' + (activeTab === "users" ? "active" : "") + '"><span class="admin-nav-icon">' + icon("users") + '</span><span class="admin-nav-text">Quản lý người dùng</span></a>' +
      '    <div class="sidebar-divider"></div>' +
      '    <div class="admin-account">' +
      '      <button type="button" class="admin-account-trigger" id="admin-account-trigger">' +
      '        <span class="admin-account-avatar">' + icon("userCircle") + '</span>' +
      '        <span class="admin-account-name">' + accountName + '</span>' +
      '        <span class="admin-account-caret">' + icon("chevronDown") + '</span>' +
      '      </button>' +
      '      <div class="admin-account-dropdown" id="admin-account-dropdown">' +
      '        <a href="index.html" class="admin-account-item">' + icon("user") + '<span>Trang người dùng</span></a>' +
      '        <button type="button" class="admin-account-item" id="admin-change-password">' + icon("lock") + '<span>Đổi mật khẩu</span></button>' +
      '        <button type="button" class="admin-account-item admin-account-logout" id="admin-logout">' + icon("logout") + '<span>Đăng xuất</span></button>' +
      '      </div>' +
      '    </div>' +
      '  </nav>' +
      '</aside>';

    const accountTrigger = document.getElementById("admin-account-trigger");
    const accountDropdown = document.getElementById("admin-account-dropdown");
    if (accountTrigger && accountDropdown) {
      accountTrigger.addEventListener("click", function (event) {
        event.stopPropagation();
        accountDropdown.classList.toggle("show");
      });

      document.addEventListener("click", function (event) {
        if (!event.target.closest(".admin-account")) {
          accountDropdown.classList.remove("show");
        }
      });
    }

    const changePasswordButton = document.getElementById("admin-change-password");
    if (changePasswordButton) {
      changePasswordButton.addEventListener("click", function () {
        openAuth("change-password");
      });
    }

    const logoutButton = document.getElementById("admin-logout");
    if (logoutButton) {
      logoutButton.addEventListener("click", async function () {
        try {
          await AppApi.authLogout();
        } catch {
          // ignore
        }
        clearSession();
        window.location.href = "index.html";
      });
    }
  }

  async function refreshAdminSidebarBadge() {
    const badge = document.getElementById("admin-order-badge");
    if (!badge) return;

    try {
      const data = await AppApi.getAdminOrders("?status=pending&page=1&pageSize=1");
      const pendingCount = Number(data.totalPendingOrders || data.total || 0);
      if (pendingCount > 0) {
        badge.textContent = pendingCount > 99 ? "99+" : String(pendingCount);
        badge.style.display = "inline-flex";
      } else {
        badge.style.display = "none";
      }
    } catch {
      badge.style.display = "none";
    }
  }

  async function mountPublicPage(options) {
    ensureIconLibrary();

    const config = options || {};
    const keyword = getQueryParams().get("keyword") || "";

    document.body.classList.add("public-page");
    document.body.style.backgroundImage = "url('" + AppConfig.buildImageUrl("background.png") + "')";

    renderHeader({ showSearch: config.showSearch !== false, keyword: keyword });
    renderAuthModal();

    if (config.withWhyChoose) {
      renderWhyChoose();
    }
    if (config.withFooter) {
      renderFooter();
    }

    if (!unauthorizedBound) {
      unauthorizedBound = true;
      window.addEventListener("app:unauthorized", function () {
        clearSession();
        refreshAuthUi();
        showToast("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", "warning");
        openAuth("login");
      });
    }

    await bootstrapAuth();
  }

  async function mountAdminPage(options) {
    ensureIconLibrary();

    const config = options || {};

    document.body.classList.remove("public-page");
    document.body.classList.add("admin-page");
    document.body.style.backgroundImage = "";

    const headerRoot = document.getElementById("app-header");
    if (headerRoot) headerRoot.innerHTML = "";
    renderAuthModal();
    renderAdminSidebar(config.activeTab || "statistics");

    if (!unauthorizedBound) {
      unauthorizedBound = true;
      window.addEventListener("app:unauthorized", function () {
        clearSession();
        refreshAuthUi();
        showToast("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", "warning");
        openAuth("login");
      });
    }

    const user = await bootstrapAuth();
    if (!user) {
      openAuth("login");
      return null;
    }

    if (user.role !== "admin") {
      showToast("Bạn không có quyền truy cập trang admin", "error");
      window.location.href = "index.html";
      return null;
    }

    refreshAdminSidebarBadge();

    return user;
  }

  function ensureLoggedIn() {
    const user = getUser();
    if (!user) {
      showToast("Vui lòng đăng nhập để tiếp tục", "warning");
      openAuth("login");
      return false;
    }
    return true;
  }

  function getApiErrorMessage(error, fallback) {
    if (error && error.errors && error.errors.length && error.errors[0].message) {
      return error.errors[0].message;
    }
    if (error && error.message) {
      return error.message;
    }
    return fallback || "Đã có lỗi xảy ra";
  }

  ensureIconLibrary();

  window.App = {
    mountPublicPage,
    mountAdminPage,
    ensureLoggedIn,
    getToken,
    getUser,
    getDisplayUsername,
    saveSession,
    clearSession,
    refreshAuthUi,
    openAuth,
    closeAuth,
    setAuthStep,
    formatPrice,
    formatDate,
    getStatusText,
    getStatusClass,
    getGenderText,
    getQueryParams,
    updateQuery,
    showToast,
    escapeHtml,
    icon,
    renderPagination,
    getApiErrorMessage
  };
})(window, document);
