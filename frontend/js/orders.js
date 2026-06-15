let userOrdersState = {
  items: [],
  expandedById: {}
};

document.addEventListener("DOMContentLoaded", async function () {
  await App.mountPublicPage({ showSearch: false, withWhyChoose: false, withFooter: false });

  document.addEventListener("app:auth-changed", async function () {
    if (!App.getUser()) return;
    await loadOrders();
  });

  if (!App.ensureLoggedIn()) {
    return;
  }

  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get("payos_success") === "true") {
    App.showToast("Thanh toán đơn hàng thành công!", "success");
    window.history.replaceState({}, document.title, window.location.pathname);
  }

  await loadOrders();
});

async function loadOrders() {
  const tbody = document.getElementById("orders-tbody");
  const empty = document.getElementById("orders-empty");
  const paging = document.getElementById("orders-pagination");

  if (!tbody || !empty || !paging) return;

  const params = App.getQueryParams();
  const page = Number(params.get("page") || 1);
  const pageSize = Number(params.get("pageSize") || 10);

  tbody.innerHTML = '<tr><td colspan="5"><div class="loading-center"><div class="spinner"></div></div></td></tr>';

  try {
    const data = await AppApi.getOrders("?page=" + page + "&pageSize=" + pageSize);
    const items = data.items || [];

    userOrdersState.items = items;
    const nextExpandedState = {};
    items.forEach(function (order) {
      const id = String(order.id);
      if (Object.prototype.hasOwnProperty.call(userOrdersState.expandedById, id)) {
        nextExpandedState[id] = !!userOrdersState.expandedById[id];
      } else {
        nextExpandedState[id] = !isClosedOrderStatus(order.status);
      }
    });
    userOrdersState.expandedById = nextExpandedState;

    if (!items.length) {
      tbody.innerHTML = "";
      empty.style.display = "flex";
      paging.innerHTML = "";
      return;
    }

    empty.style.display = "none";
    renderOrdersTable();

    App.renderPagination(paging, data.page, data.pageSize, data.total, function (nextPage) {
      App.updateQuery({ page: nextPage, pageSize: data.pageSize });
    });
  } catch (error) {
    tbody.innerHTML = "";
    empty.style.display = "flex";
    empty.querySelector("p").textContent = App.getApiErrorMessage(error, "Không tải được danh sách đơn hàng");
    paging.innerHTML = "";
  }
}

function renderOrdersTable() {
  const tbody = document.getElementById("orders-tbody");
  if (!tbody) return;

  tbody.innerHTML = userOrdersState.items.map(function (order) {
    const orderId = String(order.id);
    const expanded = !!userOrdersState.expandedById[orderId];
    const statusClass = getStatusClass(order.status);

    return '' +
      '<tr class="order-row order-row-' + statusClass + '" data-order-id="' + orderId + '">' +
      '  <td><span class="order-id-link">#ORD-' + order.id + '</span></td>' +
      '  <td>' + App.formatDate(order.createdAt) + '</td>' +
      '  <td>' + App.formatDate(order.updatedAt) + '</td>' +
      '  <td><span class="order-total-price">' + App.formatPrice(order.totalPrice) + '</span></td>' +
      '  <td><span class="status-badge ' + getStatusClass(order.status) + '">' + App.getStatusText(order.status) + '</span></td>' +
      '</tr>' +
      (expanded
        ? '<tr class="order-expanded-row">' +
          '  <td colspan="5">' + renderExpandedOrder(order) + '</td>' +
          '</tr>'
        : '');
  }).join("");

  Array.from(tbody.querySelectorAll(".order-row[data-order-id]"))
    .forEach(function (row) {
      row.addEventListener("click", function () {
        const orderId = row.getAttribute("data-order-id");
        userOrdersState.expandedById[orderId] = !userOrdersState.expandedById[orderId];
        renderOrdersTable();
      });
    });
}

function renderExpandedOrder(order) {
  const details = Array.isArray(order.orderDetails) ? order.orderDetails : [];

  return '' +
    '<div class="order-expanded-body">' +
    '  <div>' +
    '    <div class="order-section-title">Sản phẩm (' + details.length + ')</div>' +
    details.map(function (detail) {
      const thumbnail = AppConfig.buildImageUrl(detail.thumbnail);
      const name = App.escapeHtml(detail.productName || "Sản phẩm");
      const hasProductId = detail.productId !== undefined && detail.productId !== null && detail.productId !== "";
      const keywordFallback = String(detail.productName || "").trim();
      const hasProductLink = hasProductId || keywordFallback !== "";
      const productHref = hasProductId
        ? "product.html?id=" + encodeURIComponent(detail.productId)
        : "index.html?keyword=" + encodeURIComponent(keywordFallback);
      const imageHtml = hasProductLink
        ? '<a class="order-item-thumb-link" href="' + productHref + '"><img class="order-item-img" src="' + thumbnail + '" alt="' + name + '"></a>'
        : '<img class="order-item-img" src="' + thumbnail + '" alt="' + name + '">';
      const nameHtml = hasProductLink
        ? '<a class="order-item-name-link" href="' + productHref + '">' + name + '</a>'
        : name;
      const color = App.escapeHtml(detail.color || "-");
      const size = App.escapeHtml(detail.size || "-");
      const quantity = Number(detail.quantity) || 0;
      const price = Number(detail.price) || 0;
      const linePrice = price * quantity;

      return '' +
        '<div class="order-item">' +
        '  ' + imageHtml +
        '  <div class="order-item-info">' +
        '    <div class="order-item-name">' + nameHtml + '</div>' +
        '    <div class="order-item-variant">Phân loại: ' + color + ' - ' + size + ' | SL: ' + quantity + '</div>' +
        '  </div>' +
        '  <div class="order-item-price">' + App.formatPrice(linePrice) + '</div>' +
        '</div>';
    }).join("") +
    '  </div>' +
    '  <div class="order-expanded-delivery">' +
    '    <div class="order-section-title">Thông tin giao hàng</div>' +
    '    <div class="order-delivery-row"><span class="order-delivery-label">Người nhận:</span> ' + App.escapeHtml(order.shippingName || "") + '</div>' +
    '    <div class="order-delivery-row"><span class="order-delivery-label">Số ĐT:</span> ' + App.escapeHtml(order.shippingPhone || "") + '</div>' +
    '    <div class="order-delivery-row"><span class="order-delivery-label">Địa chỉ:</span> ' + App.escapeHtml(order.shippingAddress || "") + '</div>' +
    '  </div>' +
    '</div>';
}

function isClosedOrderStatus(status) {
  const normalized = String(status || "").toLowerCase();
  return normalized === "delivered" || normalized === "cancelled";
}

function getStatusClass(status) {
  return App.getStatusClass(status);
}
