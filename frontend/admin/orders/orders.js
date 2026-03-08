/* ===== ADMIN ORDERS PAGE ===== */
let allOrders = [];
let expandedOrderId = null;

document.addEventListener('DOMContentLoaded', async () => {
  await initAdminPage('orders');
  allOrders = mockGetOrders();
  renderOrderStats();
  renderOrders(allOrders);
  bindOrderFilters();
});

function formatDateVN(isoStr) {
  if (!isoStr) return '';
  const parts = isoStr.split('-');
  if (parts.length !== 3) return isoStr;
  return parts[2] + '/' + parts[1] + '/' + parts[0];
}

function renderOrderStats() {
  const pendingCount = allOrders.filter((o) => o.status === 'pending').length;
  const incompleteCount = allOrders.filter((o) => !['delivered', 'cancelled'].includes(o.status)).length;

  document.getElementById('order-stats').innerHTML = `
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#FEF3C7">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#D97706" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
      </div>
      <div class="admin-stat-info">
        <div class="admin-stat-label">Số đơn hàng đang chờ</div>
        <div class="admin-stat-value">${pendingCount}</div>
      </div>
    </div>
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#DBEAFE">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
      </div>
      <div class="admin-stat-info">
        <div class="admin-stat-label">Số đơn hàng chưa hoàn thành</div>
        <div class="admin-stat-value">${incompleteCount}</div>
      </div>
    </div>
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#FCE7F3">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#DB2777" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 002 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0022 16z"/><path d="M3.27 6.96L12 12.01l8.73-5.05M12 22.08V12"/></svg>
      </div>
      <div class="admin-stat-info">
        <div class="admin-stat-label">Tổng số đơn hàng</div>
        <div class="admin-stat-value">${allOrders.length.toLocaleString()}</div>
      </div>
    </div>
  `;
}

function renderOrders(orders) {
  const tbody = document.getElementById('orders-tbody');
  const base = getBasePath();
  const statuses = ['pending', 'processing', 'shipping', 'delivered', 'cancelled'];
  const statusLabels = { pending: 'Đang chờ', processing: 'Đang xử lý', shipping: 'Đang giao', delivered: 'Đã giao', cancelled: 'Đã hủy' };

  tbody.innerHTML = orders.map((o) => {
    const isExpanded = expandedOrderId === o.id;
    const statusOptions = statuses.map(s =>
      `<option value="${s}" ${s === o.status ? 'selected' : ''}>${statusLabels[s]}</option>`
    ).join('');

    let expandedHTML = '';
    if (isExpanded) {
      expandedHTML = `
        <tr class="order-expanded-row">
          <td colspan="6">
            <div class="order-expanded-body">
              <div class="order-expanded-products">
                <div class="order-section-title">Sản phẩm (${o.items.length})</div>
                ${o.items.map(item => `
                  <div class="od-item">
                    <img class="od-item-img" src="${base}${item.image}" alt="${item.name}">
                    <div class="od-item-info">
                      <div class="od-item-name">${item.name}</div>
                      <div class="od-item-variant">Phân loại: ${item.color} - ${item.size} | Số lượng: ${item.quantity}</div>
                    </div>
                    <div class="od-item-right">${formatPrice(item.price * item.quantity)}</div>
                  </div>
                `).join('')}
              </div>
              <div class="order-expanded-delivery">
                <div class="order-section-title">Thông tin giao hàng</div>
                <div class="od-info-row"><span>Người nhận:</span> <span>${o.recipientName}</span></div>
                <div class="od-info-row"><span>Số ĐT:</span> <span>${o.phone}</span></div>
                <div class="od-info-row"><span>Địa chỉ:</span> <span>${o.address}</span></div>
              </div>
            </div>
          </td>
        </tr>`;
    }

    return `
      <tr class="order-row ${isExpanded ? 'expanded' : ''}" data-order-id="${o.id}" style="cursor:pointer">
        <td><strong style="color:#2563EB">#${o.id}</strong></td>
        <td><strong>${o.userEmail}</strong></td>
        <td>${formatDateVN(o.createdAt)}</td>
        <td>${formatDateVN(o.updatedAt)}</td>
        <td><strong>${formatPrice(o.total)}</strong></td>
        <td>
          <select class="status-badge-select ${o.status}" data-order-id="${o.id}" onclick="event.stopPropagation()">
            ${statusOptions}
          </select>
        </td>
      </tr>
      ${expandedHTML}`;
  }).join('');

  // Row click to expand/collapse
  tbody.querySelectorAll('.order-row').forEach(row => {
    row.addEventListener('click', () => {
      const orderId = row.dataset.orderId;
      expandedOrderId = expandedOrderId === orderId ? null : orderId;
      renderOrders(getFilteredOrders());
    });
  });

  // Status select change
  tbody.querySelectorAll('.status-badge-select').forEach(sel => {
    sel.addEventListener('change', () => {
      showToast(`Đã cập nhật trạng thái đơn #${sel.dataset.orderId}`, 'success');
      sel.className = 'status-badge-select ' + sel.value;
    });
  });
}

function getFilteredOrders() {
  const search = (document.getElementById('order-search')?.value || '').trim().toLowerCase();
  const statusFilter = document.getElementById('order-status-filter')?.value || '';
  return allOrders.filter(o => {
    if (statusFilter && o.status !== statusFilter) return false;
    if (search && !o.id.toLowerCase().includes(search) && !o.userEmail.toLowerCase().includes(search)) return false;
    return true;
  });
}

function bindOrderFilters() {
  document.getElementById('order-search')?.addEventListener('input', () => {
    renderOrders(getFilteredOrders());
  });
  document.getElementById('order-status-filter')?.addEventListener('change', () => {
    renderOrders(getFilteredOrders());
  });
}
