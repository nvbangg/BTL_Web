/* ===== CART PAGE ===== */
let cartItems = [];
let isOrderPanelExpanded = false;

document.addEventListener('DOMContentLoaded', async () => {
  await initPage();
  loadCart();
  bindCartEvents();
  bindOrderEvents();
});

function loadCart() {
  cartItems = resolveCartItemsDirectly().map((item) => ({
    ...item,
    checked: true
  }));
  renderCartPage();
}

function getCurrentMockUserId() {
  try {
    const raw = localStorage.getItem('mockCurrentUser');
    const user = raw ? JSON.parse(raw) : null;
    if (user && user.id) return Number(user.id);
  } catch {
    // Fallback below.
  }
  const data = typeof MOCK_DATA !== 'undefined' ? MOCK_DATA : null;
  const cartKeys = Object.keys((data && data.cart) || {});
  return Number(cartKeys[0] || 0);
}

function resolveCartItemsDirectly() {
  const data = (typeof MOCK_DATA !== 'undefined' && MOCK_DATA) ? MOCK_DATA : null;
  if (!data) return [];

  const userId = getCurrentMockUserId();
  if (!userId) return [];

  const rawItems = Array.isArray(data.cart?.[userId]) ? data.cart[userId] : [];
  if (!rawItems.length) return [];

  const allProducts = [
    ...(Array.isArray(data.productsPage1) ? data.productsPage1 : []),
    ...(Array.isArray(data.productsPage2) ? data.productsPage2 : [])
  ];

  return rawItems
    .map((item) => {
      const product = allProducts.find((p) => Number(p.id) === Number(item.productId));
      if (!product) return null;

      const variant = (product.variants || []).find((v) => Number(v.id) === Number(item.variantId));
      if (!variant) return null;

      return {
        itemId: Number(item.itemId),
        product,
        variant,
        quantity: Math.max(1, Number(item.quantity) || 1)
      };
    })
    .filter(Boolean);
}

function getItemPrice(item) {
  const variantPrice = Number(item?.variant?.price);
  if (Number.isFinite(variantPrice) && variantPrice > 0) return variantPrice;
  return Number(item?.product?.basePrice) || 0;
}

function formatVnd(value) {
  return `${Number(value || 0).toLocaleString('vi-VN')}đ`;
}

function renderCartPage() {
  const layout = document.getElementById('cart-layout');
  const empty = document.getElementById('cart-empty');
  if (!layout || !empty) return;

  if (cartItems.length === 0) {
    isOrderPanelExpanded = false;
    layout.style.display = 'none';
    empty.style.display = 'flex';
    return;
  }

  layout.style.display = 'grid';
  empty.style.display = 'none';

  renderSelectAll();
  renderCartItems();
  renderSummary();
}

function renderSelectAll() {
  const selectAll = document.getElementById('select-all');
  const label = document.getElementById('select-all-label');
  if (!selectAll || !label) return;

  const total = cartItems.length;
  const checked = cartItems.filter((item) => item.checked).length;
  selectAll.checked = total > 0 && checked === total;
  label.textContent = `Chọn tất cả (${total} sản phẩm)`;
}

function renderCartItems() {
  const list = document.getElementById('cart-list');
  if (!list) return;

  list.innerHTML = cartItems.map((item) => {
    const price = getItemPrice(item);
    const subtotal = price * item.quantity;
    return `
      <div class="cart-item" data-item-id="${item.itemId}">
        <div class="cart-item-check">
          <input type="checkbox" class="item-check" data-item-id="${item.itemId}" ${item.checked ? 'checked' : ''}>
        </div>
        <img class="cart-item-img" src="../${item.product.images[0]}" alt="${item.product.name}">
        <div class="cart-item-info">
          <div class="cart-item-top">
            <div class="cart-item-name">${item.product.name}</div>
            <button type="button" class="cart-item-remove" data-remove-id="${item.itemId}" aria-label="Xóa sản phẩm">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 3h6"></path>
                <path d="M10 7v10"></path>
                <path d="M14 7v10"></path>
                <path d="M5 7h14"></path>
                <path d="M7 7v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V7"></path>
              </svg>
            </button>
          </div>
          <div class="cart-item-variant">Màu sắc: ${item.variant.color} - Size: ${item.variant.size}</div>
          <div class="cart-item-unit-price">Đơn giá: <strong>${formatVnd(price)}</strong></div>
          <div class="cart-item-bottom">
            <div class="cart-qty-row">
              <div class="cart-qty-label">Số lượng:</div>
              <div class="qty-control">
                <button type="button" class="qty-minus" data-qty-id="${item.itemId}">-</button>
                <span>${item.quantity}</span>
                <button type="button" class="qty-plus" data-qty-id="${item.itemId}">+</button>
              </div>
            </div>
            <div class="cart-item-subtotal">Số tiền: ${formatVnd(subtotal)}</div>
          </div>
        </div>
      </div>`;
  }).join('');
}

function getSelectedItems() {
  return cartItems.filter((item) => item.checked);
}

function renderSummary() {
  const total = getSelectedItems().reduce((sum, item) => sum + getItemPrice(item) * item.quantity, 0);
  const priceEl = document.getElementById('cart-summary-price');
  const btn = document.getElementById('btn-place-order');
  const btnLabel = document.querySelector('#btn-place-order .btn-checkout-label');
  const summary = document.getElementById('cart-summary');
  const panel = document.getElementById('inline-order-panel');

  if (priceEl) priceEl.textContent = formatVnd(total);
  if (btn) btn.disabled = getSelectedItems().length === 0;

  if (panel) panel.style.display = isOrderPanelExpanded ? 'block' : 'none';
  if (summary) {
    if (isOrderPanelExpanded) summary.classList.add('is-expanded');
    else summary.classList.remove('is-expanded');
  }
  if (btnLabel) {
    btnLabel.textContent = isOrderPanelExpanded ? 'Xác nhận đặt hàng' : 'Đặt hàng';
  }
}

function refreshCartView() {
  if (cartItems.length === 0) {
    renderCartPage();
    return;
  }
  renderSelectAll();
  renderCartItems();
  renderSummary();
}

function bindCartEvents() {
  const selectAll = document.getElementById('select-all');
  if (selectAll) {
    selectAll.addEventListener('change', () => {
      cartItems = cartItems.map((item) => ({ ...item, checked: selectAll.checked }));
      refreshCartView();
    });
  }

  const list = document.getElementById('cart-list');
  if (!list) return;

  list.addEventListener('change', (e) => {
    const check = e.target.closest('.item-check');
    if (!check) return;
    const id = Number(check.dataset.itemId);
    cartItems = cartItems.map((item) => (item.itemId === id ? { ...item, checked: check.checked } : item));
    refreshCartView();
  });

  list.addEventListener('click', (e) => {
    const removeBtn = e.target.closest('[data-remove-id]');
    if (removeBtn) {
      const removeId = Number(removeBtn.dataset.removeId);
      cartItems = cartItems.filter((item) => item.itemId !== removeId);
      refreshCartView();
      return;
    }

    const qtyBtn = e.target.closest('[data-qty-id]');
    if (!qtyBtn) return;
    const itemId = Number(qtyBtn.dataset.qtyId);
    const isPlus = qtyBtn.classList.contains('qty-plus');
    cartItems = cartItems.map((item) => {
      if (item.itemId !== itemId) return item;
      const nextQty = isPlus ? item.quantity + 1 : Math.max(1, item.quantity - 1);
      return { ...item, quantity: nextQty };
    });
    refreshCartView();
  });
}

function bindOrderEvents() {
  const actionBtn = document.getElementById('btn-place-order');
  const panel = document.getElementById('inline-order-panel');
  const form = document.getElementById('order-form');
  if (!actionBtn || !panel || !form) return;

  actionBtn.addEventListener('click', () => {
    if (getSelectedItems().length === 0) {
      showToast('Vui lòng chọn ít nhất 1 sản phẩm', 'warning');
      return;
    }

    if (!isOrderPanelExpanded) {
      isOrderPanelExpanded = true;
      prefillOrderForm(form);
      renderSummary();
      panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      return;
    }

    form.requestSubmit();
  });

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const recipientName = String(fd.get('recipientName') || '').trim();
    const phone = String(fd.get('phone') || '').trim();
    const address = String(fd.get('address') || '').trim();

    if (!recipientName || !phone || !address) {
      showToast('Vui lòng điền đầy đủ thông tin đặt hàng', 'warning');
      return;
    }

    const selectedIds = new Set(getSelectedItems().map((item) => item.itemId));
    cartItems = cartItems.filter((item) => !selectedIds.has(item.itemId));

    isOrderPanelExpanded = false;
    refreshCartView();
    showToast('Đặt hàng thành công', 'success');
    form.reset();
  });
}

function prefillOrderForm(form) {
  let user = null;
  try {
    const raw = localStorage.getItem('mockCurrentUser');
    user = raw ? JSON.parse(raw) : null;
  } catch {
    user = null;
  }

  const users = (typeof MOCK_DATA !== 'undefined' && Array.isArray(MOCK_DATA.users)) ? MOCK_DATA.users : [];
  const profile = users.find((u) => Number(u.id) === Number(user?.id)) || users[0] || {};
  form.recipientName.value = profile.displayName || '';
  form.phone.value = profile.phone || '';
  form.address.value = profile.address || '';
}
