/* ===== UTILS ===== */
function formatPrice(n) {
  return Number(n || 0).toLocaleString('vi-VN') + 'đ';
}

function getBasePath() {
  const path = window.location.pathname;
  if (path.includes('/admin/products/') || path.includes('/admin/orders/') || path.includes('/admin/categories/')) return '../../';
  if (path.includes('/products/') || path.includes('/cart/') || path.includes('/orders/') || path.includes('/admin/')) return '../';
  return '';
}

function getAdminBase() {
  const path = window.location.pathname;
  if (path.includes('/admin/products/') || path.includes('/admin/orders/') || path.includes('/admin/categories/')) return '../';
  return '';
}

function showToast(message, type = 'info') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const icons = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span class="toast-icon">${icons[type] || icons.info}</span><span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('removing');
    toast.addEventListener('animationend', () => toast.remove());
  }, 2500);
}

function getParam(key) {
  return new URLSearchParams(window.location.search).get(key);
}

function getStatusText(status) {
  const map = { pending: 'Đang chờ', processing: 'Đang xử lý', shipping: 'Đang giao', delivered: 'Đã giao', cancelled: 'Đã hủy' };
  return map[status] || status;
}

function getProductColors(product) {
  return [...new Set((product?.variants || []).map(v => v.color))];
}

function getProductSizes(product, color) {
  return (product?.variants || []).filter(v => v.color === color).map(v => v.size);
}

function findVariant(product, color, size) {
  return (product?.variants || []).find(v => v.color === color && v.size === size);
}

function getTotalStock(product) {
  return (product?.variants || []).reduce((s, v) => s + Number(v.stock || 0), 0);
}

function getVariantPrice(variant, basePrice) {
  return Number(variant?.price || basePrice || 0);
}

function getMockData() {
  try {
    return typeof MOCK_DATA !== 'undefined' ? MOCK_DATA : null;
  } catch {
    return null;
  }
}

function mockGetCategories() {
  return JSON.parse(JSON.stringify(getMockData()?.categories || []));
}

function mockGetProducts() {
  const data = getMockData() || {};
  const page1 = data.productsPage1 || [];
  const page2 = data.productsPage2 || [];
  return JSON.parse(JSON.stringify([...page1, ...page2]));
}

function mockGetProductsByPage(page) {
  const p = Number(page || 1);
  const data = getMockData();
  if (p === 1) return JSON.parse(JSON.stringify(data?.productsPage1 || []));
  if (p === 2) return JSON.parse(JSON.stringify(data?.productsPage2 || []));
  return [];
}

function mockGetTotalPages() {
  const data = getMockData() || {};
  const total = Number(Boolean(data.productsPage1)) + Number(Boolean(data.productsPage2));
  return total || 1;
}

function mockGetUsers() {
  return JSON.parse(JSON.stringify(getMockData()?.users || []));
}

function mockGetOrders() {
  return JSON.parse(JSON.stringify(getMockData()?.orders || []));
}

function mockGetCartByUser() {
  return JSON.parse(JSON.stringify(getMockData()?.cart || {}));
}

function mockResolveCartItems(userId) {
  const products = mockGetProducts();
  const cartByUser = getMockData()?.cart || {};
  const rawItems = cartByUser[Number(userId)] || [];

  return rawItems.map((rawItem) => {
    const product = products.find((p) => Number(p.id) === Number(rawItem.productId));
    if (!product) return null;
    const variant = (product.variants || []).find((v) => Number(v.id) === Number(rawItem.variantId));
    if (!variant) return null;
    return {
      itemId: rawItem.itemId,
      productId: rawItem.productId,
      variantId: rawItem.variantId,
      quantity: rawItem.quantity,
      product: {
        id: product.id,
        name: product.name,
        basePrice: product.basePrice,
        images: product.images
      },
      variant: {
        id: variant.id,
        color: variant.color,
        size: variant.size,
        stock: variant.stock,
        price: variant.price || null
      }
    };
  }).filter(Boolean);
}

function mockGetCartCount() {
  const cartByUser = getMockData()?.cart || {};
  return Object.values(cartByUser)
    .flat()
    .reduce((sum, item) => sum + Number(item.quantity || 0), 0);
}

function renderProductCard(product) {
  const base = getBasePath();
  // Use thumbnail from API if available, else fall back to images[0]
  const imgUrl = product.thumbnail || (product.images && product.images[0]) || '';
  const price = product.price || product.basePrice || 0;
  const totalStock = product.totalStock !== undefined ? product.totalStock : getTotalStock(product);
  let badge = '';
  if (totalStock === 0) badge = '<span class="product-card-badge out-of-stock">Hết hàng</span>';
  return `
    <a href="${base}products/?id=${product.id}" class="product-card">
      ${badge}
      <img class="product-card-img" src="${base}${imgUrl}" alt="${product.name}" loading="lazy">
      <div class="product-card-body">
        <div class="product-card-price">${formatPrice(price)}</div>
        <div class="product-card-name">${product.name}</div>
      </div>
    </a>`;
}

function renderPagination(page, totalPages) {
  if (totalPages <= 1) return '';
  let html = '<div class="pagination">';
  html += `<button ${page <= 1 ? 'disabled' : ''} data-page="${page - 1}">‹</button>`;
  for (let i = 1; i <= totalPages; i++) {
    html += `<button class="${i === page ? 'active' : ''}" data-page="${i}">${i}</button>`;
  }
  html += `<button ${page >= totalPages ? 'disabled' : ''} data-page="${page + 1}">›</button>`;
  html += '</div>';
  return html;
}

function bindPagination(container, callback) {
  container.querySelectorAll('.pagination button[data-page]').forEach((btn) => {
    btn.addEventListener('click', () => {
      if (btn.disabled) return;
      callback(Number(btn.dataset.page));
    });
  });
}
