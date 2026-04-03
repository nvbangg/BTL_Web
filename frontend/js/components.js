/* ===== SHARED COMPONENTS ===== */

async function loadPartial(selector, file) {
  const el = document.querySelector(selector);
  if (!el) return;
  const resp = await fetch(getBasePath() + file);
  if (!resp.ok) return;
  el.innerHTML = await resp.text();
}

function isAdminPage() {
  return window.location.pathname.includes('/admin/');
}

async function initPage() {
  const partials = [
    loadPartial('#header', 'partials/header.html'),
    loadPartial('#auth', 'partials/auth.html')
  ];
  if (!isAdminPage()) partials.push(loadPartial('#footer', 'partials/footer.html'));
  if (document.getElementById('why-choose')) partials.push(loadPartial('#why-choose', 'partials/why-choose.html'));
  await Promise.all(partials);
  initHeader();
  initAuth();
}

async function initAdminPage(activeTab) {
  await Promise.all([
    loadPartial('#header', 'partials/header.html'),
    loadPartial('#admin-sidebar', 'partials/admin-sidebar.html'),
    loadPartial('#auth', 'partials/auth.html')
  ]);
  initHeader();
  initAuth();
  initAdminSidebar(activeTab);
}

function initHeader() {
  const base = getBasePath();

  const logoLink = document.getElementById('header-logo-link');
  if (logoLink) logoLink.href = base;
  const cartLink = document.getElementById('header-cart-link');
  if (cartLink) cartLink.href = base + 'cart/';
  const ordersLink = document.querySelector('.orders-link');
  if (ordersLink) ordersLink.href = base + 'orders/';
  const adminLink = document.querySelector('.admin-link');
  if (adminLink) adminLink.href = base + 'admin/';

  const loginArea = document.querySelector('.header-login-area');
  const userArea = document.querySelector('.header-user-area');
  if (loginArea) loginArea.style.display = 'flex';
  if (userArea) userArea.style.display = 'none';

  const searchForm = document.querySelector('.header-search');
  if (searchForm) {
    searchForm.onsubmit = (e) => {
      e.preventDefault();
      const kw = searchForm.querySelector('input')?.value.trim() || '';
      window.location.href = getBasePath() + '?keyword=' + encodeURIComponent(kw);
    };
    const kw = getParam('keyword');
    if (kw) {
      const input = searchForm.querySelector('input');
      if (input) input.value = kw;
    }
  }

  updateCartBadge();
}

function updateCartBadge() {
  const cartLink = document.getElementById('header-cart-link');
  if (!cartLink) return;
  const label = cartLink.querySelector('.menu-label');
  if (label) label.textContent = 'Giỏ hàng';
}

function initAdminSidebar(activeTab) {
  const base = getBasePath();
  const adminBase = getAdminBase();
  const links = document.querySelectorAll('.admin-nav-link');
  const routes = {
    statistics: adminBase,
    orders: adminBase + 'orders/',
    products: adminBase + 'products/',
    categories: adminBase + 'categories/',
    users: adminBase + 'users/',
    home: base
  };

  links.forEach((link) => {
    const tab = link.dataset.tab;
    if (routes[tab]) link.href = routes[tab];
    if (tab === activeTab) link.classList.add('active');
    else link.classList.remove('active');
  });

  const pendingCount = mockGetOrders().filter(o => o.status === 'pending').length;
  const badge = document.getElementById('pending-badge');
  if (badge) badge.textContent = pendingCount > 0 ? pendingCount : '';
}
