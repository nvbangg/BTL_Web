let orderState = {
  keyword: "",
  status: "",
  page: 1,
  pageSize: 10,
  expandedId: null,
  data: null
};

document.addEventListener("DOMContentLoaded", async function () {
  document.addEventListener("app:auth-changed", function () {
    window.location.reload();
  });

  const user = await App.mountAdminPage({ activeTab: "orders" });
  if (!user) return;

  initOrderState();
  bindOrderFilters();
  await loadAdminOrders();
});

function initOrderState() {
  const params = App.getQueryParams();
  orderState.keyword = params.get("keyword") || "";
  orderState.status = params.get("status") || "";
  orderState.page = Number(params.get("page") || 1);
  orderState.pageSize = Number(params.get("pageSize") || 10);

  const searchInput = document.getElementById("order-search");
  const statusSelect = document.getElementById("order-status-filter");

  if (searchInput) searchInput.value = orderState.keyword;
  if (statusSelect) statusSelect.value = orderState.status;
}

function bindOrderFilters() {
  const searchInput = document.getElementById("order-search");
  const statusSelect = document.getElementById("order-status-filter");

  let timer = null;

  if (searchInput) {
    searchInput.addEventListener("input", function () {
      clearTimeout(timer);
      timer = setTimeout(function () {
        App.updateQuery({ keyword: searchInput.value.trim(), page: 1 });
      }, 350);
    });
  }

  if (statusSelect) {
    statusSelect.addEventListener("change", function () {
      App.updateQuery({ status: statusSelect.value, page: 1 });
    });
  }
}

function toAdminOrdersQuery() {
  const params = new URLSearchParams();
  if (orderState.keyword) params.set("keyword", orderState.keyword);
  if (orderState.status) params.set("status", orderState.status);
  params.set("page", String(orderState.page));
  params.set("pageSize", String(orderState.pageSize));
  return "?" + params.toString();
}

async function loadAdminOrders() {
  const body = document.getElementById("orders-tbody");
  const paging = document.getElementById("orders-pagination");

  if (!body || !paging) return;

  body.innerHTML = '<tr><td colspan="5"><div class="loading-center"><div class="spinner"></div></div></td></tr>';

  try {
    const data = await AppApi.getAdminOrders(toAdminOrdersQuery());
    orderState.data = data;
    renderOrderStats(data);

    if (!(data.items || []).length) {
      body.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:28px;color:#64748B">Không có đơn hàng</td></tr>';
      paging.innerHTML = "";
      return;
    }

    renderOrderRows(data.items || []);

    App.renderPagination(paging, data.page, data.pageSize, data.total, function (nextPage) {
      App.updateQuery({ page: nextPage, pageSize: data.pageSize });
    });
  } catch (error) {
    body.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:28px;color:#EF4444">' + App.escapeHtml(App.getApiErrorMessage(error, "Không tải được đơn hàng")) + "</td></tr>";
    paging.innerHTML = "";
  }
}

function renderOrderStats(data) {
  const root = document.getElementById("order-stats");
  if (!root) return;

  root.innerHTML = '' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#FEF3C7;color:#B45309">' + App.icon("clock") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Số đơn hàng đang chờ</div><div class="admin-stat-value">' + (data.totalPendingOrders || 0) + '</div></div></div>' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#DBEAFE;color:#1D4ED8">' + App.icon("truck") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Số đơn hàng chưa hoàn thành</div><div class="admin-stat-value">' + (data.totalIncompleteOrders || 0) + '</div></div></div>' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#FCE7F3;color:#BE185D">' + App.icon("box") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Tổng số đơn hàng</div><div class="admin-stat-value">' + Number(data.total || 0).toLocaleString("vi-VN") + '</div></div></div>';
}

function renderOrderRows(items) {
  const body = document.getElementById("orders-tbody");
  if (!body) return;

  body.innerHTML = items.map(function (order) {
    const rowId = String(order.id);
    const expanded = orderState.expandedId === rowId;

    return '' +
      '<tr class="order-row ' + (expanded ? "expanded" : "") + '" data-order-id="' + rowId + '">' +
      '  <td><strong style="color:#2563EB">#ORD-' + rowId + '</strong></td>' +
      '  <td>' + App.formatDate(order.createdAt) + '</td>' +
      '  <td>' + App.formatDate(order.updatedAt) + '</td>' +
      '  <td><strong style="color:#EF4444">' + App.formatPrice(order.totalPrice) + '</strong></td>' +
      '  <td>' + renderStatusSelect(order) + '</td>' +
      '</tr>' +
      (expanded ? renderExpandedOrder(order) : "");
  }).join("");

  Array.from(body.querySelectorAll(".order-row")).forEach(function (row) {
    row.addEventListener("click", function (event) {
      if (event.target.closest("select")) return;
      const rowId = row.getAttribute("data-order-id");
      orderState.expandedId = orderState.expandedId === rowId ? null : rowId;
      renderOrderRows((orderState.data && orderState.data.items) || []);
    });
  });

  Array.from(body.querySelectorAll(".status-badge-select")).forEach(function (select) {
    select.addEventListener("change", async function () {
      const orderId = Number(select.getAttribute("data-order-id"));
      try {
        await AppApi.updateAdminOrderStatus(orderId, select.value);
        App.showToast("Đã cập nhật trạng thái đơn hàng", "success");
        await loadAdminOrders();
      } catch (error) {
        App.showToast(App.getApiErrorMessage(error, "Cập nhật trạng thái thất bại"), "error");
        await loadAdminOrders();
      }
    });
  });
}

function renderStatusSelect(order) {
  const statuses = ["pending", "processing", "shipped", "delivered", "cancelled"];
  return '<select class="status-badge-select ' + App.getStatusClass(order.status) + '" data-order-id="' + order.id + '">' +
    statuses.map(function (status) {
      return '<option value="' + status + '" ' + (status === order.status ? "selected" : "") + '>' + App.getStatusText(status) + '</option>';
    }).join("") +
    "</select>";
}

function renderExpandedOrder(order) {
  return '' +
    '<tr class="order-expanded-row">' +
    '  <td colspan="5">' +
    '    <div class="order-expanded-body">' +
    '      <div class="order-expanded-products">' +
    '        <div class="order-section-title">Sản phẩm (' + (order.orderDetails || []).length + ')</div>' +
    (order.orderDetails || []).map(function (detail) {
      return '' +
        '<div class="od-item">' +
        '  <img class="od-item-img" src="' + AppConfig.buildImageUrl(detail.thumbnail) + '" alt="' + App.escapeHtml(detail.productName) + '">' +
        '  <div class="od-item-info">' +
        '    <div class="od-item-name">' + App.escapeHtml(detail.productName) + '</div>' +
        '    <div class="od-item-variant">Phân loại: ' + App.escapeHtml(detail.color) + ' - ' + App.escapeHtml(detail.size) + ' | Số lượng: ' + detail.quantity + '</div>' +
        '  </div>' +
        '  <div class="od-item-right">' + App.formatPrice(detail.price * detail.quantity) + '</div>' +
        '</div>';
    }).join("") +
    '      </div>' +
    '      <div class="order-expanded-delivery">' +
    '        <div class="order-section-title">Thông tin giao hàng</div>' +
    '        <div class="od-info-row"><span>Khách hàng:</span> <span>' + App.escapeHtml(order.email || "") + '</span></div>' +
    '        <div class="od-info-row"><span>Người nhận:</span> <span>' + App.escapeHtml(order.shippingName) + '</span></div>' +
    '        <div class="od-info-row"><span>Số ĐT:</span> <span>' + App.escapeHtml(order.shippingPhone) + '</span></div>' +
    '        <div class="od-info-row"><span>Địa chỉ:</span> <span>' + App.escapeHtml(order.shippingAddress) + '</span></div>' +
    '      </div>' +
    '    </div>' +
    '  </td>' +
    '</tr>';
}
