/* ===== ORDERS PAGE ===== */
document.addEventListener('DOMContentLoaded', async () => {
  await initPage();

  const user = getCurrentUser();
  if (!user) {
    window.location.href = '../';
    return;
  }

  loadOrders();
});

function formatDateVN(isoStr) {
  if (!isoStr) return '';
  try {
    const date = new Date(isoStr);
    return date.toLocaleDateString('vi-VN');
  } catch {
    return isoStr;
  }
}

async function loadOrders() {
  const list = document.getElementById('orders-list');
  const empty = document.getElementById('orders-empty');

  try {
    const response = await apiGetOrders(1, 100);
    const orders = response.items || [];

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
          <span class="order-card-total">${formatPrice(order.totalPrice)}</span>
          <span class="status-badge ${order.status}">${getStatusText(order.status)}</span>
        </div>
      </div>
    `).join('');
  } catch (error) {
    console.error('Failed to load orders', error);
    list.innerHTML = '';
    empty.style.display = 'flex';
  }
}
