/* ===== TRANG ĐƠN HÀNG ===== */
document.addEventListener('DOMContentLoaded', async () => {
  await initPage();
  loadOrders();
});

function formatDateVN(isoStr) {
  if (!isoStr) return '';
  const parts = isoStr.split('-');
  if (parts.length !== 3) return isoStr;
  return parts[2] + '/' + parts[1] + '/' + parts[0];
}

function loadOrders() {
  const orders = mockGetOrders();
  const list = document.getElementById('orders-list');
  const empty = document.getElementById('orders-empty');

  if (!orders.length) {
    list.innerHTML = '';
    empty.style.display = 'flex';
    return;
  }

  empty.style.display = 'none';
  list.innerHTML = orders.map((order) => `
    <div class="order-card">
      <div class="order-card-header">
        <span class="order-card-id">#${order.id}</span>
        <span class="order-card-date">${formatDateVN(order.createdAt)}</span>
        <span class="order-card-date">${formatDateVN(order.updatedAt)}</span>
        <span class="order-card-total">${formatPrice(order.total)}</span>
        <span class="status-badge ${order.status}">${getStatusText(order.status)}</span>
      </div>
      <div class="order-card-body">
        <div class="order-card-products">
          <div class="order-section-title">Sản phẩm (${order.items.length})</div>
          ${order.items.map((item) => `
            <div class="order-item">
              <img class="order-item-img" src="../${item.image}" alt="${item.name}">
              <div class="order-item-info">
                <div class="order-item-name">${item.name}</div>
                <div class="order-item-variant">Phân loại: ${item.color} - ${item.size} | Số lượng: ${item.quantity}</div>
              </div>
              <div class="order-item-price">${formatPrice(item.price * item.quantity)}</div>
            </div>
          `).join('')}
        </div>
        <div class="order-card-delivery">
          <div class="order-section-title">Thông tin giao hàng</div>
          <div class="order-delivery-row"><span class="order-delivery-label">Người nhận:</span> <span>${order.recipientName}</span></div>
          <div class="order-delivery-row"><span class="order-delivery-label">Số ĐT:</span> <span>${order.phone}</span></div>
          <div class="order-delivery-row"><span class="order-delivery-label">Địa chỉ:</span> <span>${order.address}</span></div>
        </div>
      </div>
    </div>
  `).join('');
}
