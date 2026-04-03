/* ===== CART PAGE ===== */
let cartItems = [];
let isOrderPanelExpanded = false;

document.addEventListener('DOMContentLoaded', async () => {
  await initPage();
  
  const user = getCurrentUser();
  if (!user) {
    window.location.href = '../';
    return;
  }
  
  loadCart();
  bindCartEvents();
  bindOrderEvents();
});

async function loadCart() {
  try {
    const response = await apiGetCart(1, 100);
    cartItems = (response.items || []).map((item) => ({
      ...item,
      checked: true
    }));
  } catch (error) {
    console.error('Failed to load cart', error);
    cartItems = [];
  }
  renderCartPage();
}

function getItemPrice(item) {
  if (typeof item.variant?.priceOverride === 'number' && item.variant.priceOverride > 0) {
    return item.variant.priceOverride;
  }
  return Number(item.product?.price) || 0;
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
    const imgUrl = item.product?.images?.[0]?.image || item.product?.thumbnail || '';
    return `
      <div class="cart-item" data-item-id="${item.id}">
        <div class="cart-item-check">
          <input type="checkbox" class="item-check" data-item-id="${item.id}" ${item.checked ? 'checked' : ''}>
        </div>
        <img class="cart-item-img" src="../${imgUrl}" alt="${item.product?.name}">
        <div class="cart-item-info">
          <div class="cart-item-top">
            <div class="cart-item-name">${item.product?.name}</div>
            <button type="button" class="cart-item-remove" data-remove-id="${item.id}" aria-label="Xóa sản phẩm">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 3h6"></path>
                <path d="M10 7v10"></path>
                <path d="M14 7v10"></path>
                <path d="M5 7h14"></path>
                <path d="M7 7v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V7"></path>
              </svg>
            </button>
          </div>
          <div class="cart-item-variant">Màu sắc: ${item.variant?.color} - Size: ${item.variant?.size}</div>
          <div class="cart-item-unit-price">Đơn giá: <strong>${formatVnd(price)}</strong></div>
          <div class="cart-item-bottom">
            <div class="cart-qty-row">
              <div class="cart-qty-label">Số lượng:</div>
              <div class="qty-control">
                <button type="button" class="qty-minus" data-qty-id="${item.id}">-</button>
                <span>${item.quantity}</span>
                <button type="button" class="qty-plus" data-qty-id="${item.id}">+</button>
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
    cartItems = cartItems.map((item) => (item.id === id ? { ...item, checked: check.checked } : item));
    refreshCartView();
  });

  list.addEventListener('click', async (e) => {
    const removeBtn = e.target.closest('[data-remove-id]');
    if (removeBtn) {
      const removeId = Number(removeBtn.dataset.removeId);
      try {
        await apiDeleteCartItem(removeId);
        cartItems = cartItems.filter((item) => item.id !== removeId);
        refreshCartView();
      } catch (error) {
        showToast('Lỗi khi xóa sản phẩm', 'error');
      }
      return;
    }

    const qtyBtn = e.target.closest('[data-qty-id]');
    if (!qtyBtn) return;
    const itemId = Number(qtyBtn.dataset.qtyId);
    const isPlus = qtyBtn.classList.contains('qty-plus');
    const item = cartItems.find(item => item.id === itemId);
    if (!item) return;
    
    const nextQty = isPlus ? item.quantity + 1 : Math.max(1, item.quantity - 1);
    try {
      await apiUpdateCartItem(itemId, nextQty);
      cartItems = cartItems.map((item) => (item.id !== itemId) ? item : { ...item, quantity: nextQty });
      refreshCartView();
    } catch (error) {
      showToast('Lỗi khi cập nhật giỏ hàng', 'error');
    }
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

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const recipientName = String(fd.get('recipientName') || '').trim();
    const phone = String(fd.get('phone') || '').trim();
    const address = String(fd.get('address') || '').trim();

    if (!recipientName || !phone || !address) {
      showToast('Vui lòng điền đầy đủ thông tin đặt hàng', 'warning');
      return;
    }

    try {
      form.querySelector('button[type="submit"]').disabled = true;
      await apiCreateOrder(recipientName, phone, address);
      
      const selectedIds = new Set(getSelectedItems().map((item) => item.id));
      cartItems = cartItems.filter((item) => !selectedIds.has(item.id));

      isOrderPanelExpanded = false;
      refreshCartView();
      showToast('Đặt hàng thành công', 'success');
      form.reset();
      
      // Redirect to orders page after 1s
      setTimeout(() => {
        window.location.href = '../orders/';
      }, 1000);
    } catch (error) {
      showToast('Lỗi khi đặt hàng', 'error');
    } finally {
      form.querySelector('button[type="submit"]').disabled = false;
    }
  });
}

async function prefillOrderForm(form) {
  const user = getCurrentUser();
  if (user) {
    form.recipientName.value = user.name || '';
    form.phone.value = user.phone || '';
    form.address.value = user.address || '';
  }
}
