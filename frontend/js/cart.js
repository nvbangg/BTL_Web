let cartState = {
  items: [],
  profile: null,
  checkoutExpanded: false
};

document.addEventListener("DOMContentLoaded", async function () {
  await App.mountPublicPage({ showSearch: false, withWhyChoose: false, withFooter: false });

  const persistedToast = sessionStorage.getItem("fashon.cart.toast");
  if (persistedToast) {
    sessionStorage.removeItem("fashon.cart.toast");
    App.showToast(persistedToast, "success");
  }



  document.addEventListener("app:auth-changed", async function () {
    if (!App.getUser()) return;
    await loadCartAndProfile();
  });

  bindCartStaticEvents();

  if (!App.ensureLoggedIn()) {
    return;
  }

  await loadCartAndProfile();
});

async function loadCartAndProfile() {
  try {
    const result = await Promise.all([AppApi.getCart(), AppApi.getMe()]);
    const cart = result[0];
    const profile = result[1];

    const sourceItems = cart.items || [];

    cartState.items = sourceItems.map(function (item) {
      const idCandidates = collectCartItemIdCandidates(item);
      const resolvedId = idCandidates.length ? idCandidates[0] : "";
      return {
        id: resolvedId,
        removeIds: idCandidates,
        productId: item.productId,
        variantId: item.variantId,
        productName: item.productName,
        thumbnail: item.thumbnail,
        color: item.color,
        size: item.size,
        quantity: Number(item.quantity || 1),
        stock: Number(item.stock || 0),
        price: Number(item.price || 0),
        checked: true
      };
    });

    cartState.profile = profile;
    fillOrderForm(profile);
    renderCart();
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Không tải được giỏ hàng"), "error");
  }
}

function bindCartStaticEvents() {
  const selectAll = document.getElementById("select-all");
  if (selectAll) {
    selectAll.addEventListener("change", function () {
      cartState.items = cartState.items.map(function (item) {
        return Object.assign({}, item, { checked: selectAll.checked });
      });
      renderCart();
    });
  }

  const orderButton = document.getElementById("btn-place-order");
  if (orderButton) {
    orderButton.addEventListener("click", function () {
      if (!getSelectedItems().length) {
        App.showToast("Vui lòng chọn ít nhất 1 sản phẩm", "warning");
        return;
      }

      if (!cartState.checkoutExpanded) {
        cartState.checkoutExpanded = true;
        renderCart();
        return;
      }

      submitOrder();
    });
  }

  const saveDefaultButton = document.getElementById("btn-save-default-profile");
  if (saveDefaultButton) {
    saveDefaultButton.addEventListener("click", saveDefaultProfile);
  }
}

function renderCart() {
  const layout = document.getElementById("cart-layout");
  const empty = document.getElementById("cart-empty");
  const list = document.getElementById("cart-list");
  const selectAll = document.getElementById("select-all");
  const selectLabel = document.getElementById("select-all-label");
  const summary = document.getElementById("cart-summary");
  const checkoutPanel = document.getElementById("inline-order-panel");
  const checkoutLabel = document.querySelector("#btn-place-order .btn-checkout-label");
  const summaryPrice = document.getElementById("cart-summary-price");

  if (!layout || !empty || !list || !summary || !checkoutPanel || !checkoutLabel || !summaryPrice) {
    return;
  }

  if (!cartState.items.length) {
    layout.style.display = "none";
    empty.style.display = "flex";
    return;
  }

  layout.style.display = "grid";
  empty.style.display = "none";

  const selectedCount = getSelectedItems().length;
  const allSelected = selectedCount > 0 && selectedCount === cartState.items.length;

  if (selectAll) selectAll.checked = allSelected;
  if (selectLabel) selectLabel.textContent = "Chọn tất cả (" + cartState.items.length + " sản phẩm)";

  list.innerHTML = cartState.items.map(function (item, index) {
    const itemId = item.id == null ? "" : String(item.id);
    const escapedItemId = App.escapeHtml(itemId);
    const hasProductId = item.productId !== undefined && item.productId !== null && item.productId !== "";
    const hasProductName = String(item.productName || "").trim() !== "";
    const hasProductLink = hasProductId || hasProductName;
    const productHref = hasProductId
      ? "product.html?id=" + encodeURIComponent(item.productId)
      : "index.html?keyword=" + encodeURIComponent(String(item.productName || "").trim());
    const imageHtml = hasProductLink
      ? '<a class="cart-item-link" href="' + productHref + '" aria-label="Xem chi tiết sản phẩm ' + App.escapeHtml(item.productName) + '"><img class="cart-item-img" src="' + AppConfig.buildImageUrl(item.thumbnail) + '" alt="' + App.escapeHtml(item.productName) + '"></a>'
      : '<img class="cart-item-img" src="' + AppConfig.buildImageUrl(item.thumbnail) + '" alt="' + App.escapeHtml(item.productName) + '">';
    const nameHtml = hasProductLink
      ? '<a class="cart-item-name-link" href="' + productHref + '">' + App.escapeHtml(item.productName) + '</a>'
      : App.escapeHtml(item.productName);

    return '' +
      '<div class="cart-item" data-item-id="' + escapedItemId + '">' +
      '  <div class="cart-item-check">' +
      '    <input type="checkbox" class="item-check" data-item-id="' + escapedItemId + '" data-item-index="' + index + '" ' + (item.checked ? "checked" : "") + '>' +
      '  </div>' +
      '  ' + imageHtml +
      '  <div class="cart-item-info">' +
      '    <div class="cart-item-top">' +
      '      <div class="cart-item-name">' + nameHtml + '</div>' +
      '      <button type="button" class="cart-item-remove" data-remove-id="' + escapedItemId + '" data-item-index="' + index + '" aria-label="Xóa sản phẩm">' + App.icon("trash") + '</button>' +
      '    </div>' +
      '    <div class="cart-item-variant">Phân loại: ' + App.escapeHtml(item.color) + ' - ' + App.escapeHtml(item.size) + '</div>' +
      '    <div class="cart-item-unit-price">Đơn giá: <strong>' + App.formatPrice(item.price) + '</strong></div>' +
      '    <div class="cart-item-bottom">' +
      '      <div class="cart-qty-row">' +
      '        <div class="cart-qty-label">Số lượng:</div>' +
      '        <div class="qty-control">' +
      '          <button type="button" class="qty-minus" data-qty-id="' + escapedItemId + '" data-item-index="' + index + '">-</button>' +
      '          <span>' + item.quantity + '</span>' +
      '          <button type="button" class="qty-plus" data-qty-id="' + escapedItemId + '" data-item-index="' + index + '">+</button>' +
      '        </div>' +
      '      </div>' +
      '      <div class="cart-item-subtotal">Số tiền: ' + App.formatPrice(item.price * item.quantity) + '</div>' +
      '    </div>' +
      '  </div>' +
      '</div>';
  }).join("");

  bindListEvents();

  const total = getSelectedItems().reduce(function (sum, item) {
    return sum + item.price * item.quantity;
  }, 0);

  summaryPrice.textContent = App.formatPrice(total);
  checkoutPanel.style.display = cartState.checkoutExpanded ? "block" : "none";
  checkoutLabel.textContent = cartState.checkoutExpanded ? "Xác nhận đặt hàng" : "Đặt hàng";
  if (cartState.checkoutExpanded) {
    summary.classList.add("is-expanded");
  } else {
    summary.classList.remove("is-expanded");
  }
}

function bindListEvents() {
  const list = document.getElementById("cart-list");
  if (!list) return;

  if (list.dataset.bound === "true") return;
  list.dataset.bound = "true";

  list.addEventListener("change", function (event) {
    const checkbox = event.target.closest(".item-check");
    if (!checkbox) return;

    const itemId = checkbox.getAttribute("data-item-id");
    const itemIndex = Number(checkbox.getAttribute("data-item-index"));

    cartState.items = cartState.items.map(function (item, index) {
      const matchesIndex = Number.isFinite(itemIndex) && index === itemIndex;
      const matchesId = String(item.id) === String(itemId);
      if (!matchesIndex && !matchesId) return item;
      return Object.assign({}, item, { checked: checkbox.checked });
    });

    renderCart();
  });

  list.addEventListener("click", async function (event) {
    const removeButton = event.target.closest("[data-remove-id]");
    if (removeButton) {
      const itemId = removeButton.getAttribute("data-remove-id");
      const itemIndex = Number(removeButton.getAttribute("data-item-index"));
      const current = cartState.items[itemIndex] || null;

      const deleteCandidates = getDeleteIdCandidates(current, itemId);
      if (!deleteCandidates.length) {
        App.showToast("Không xác định được sản phẩm để xóa", "error");
        return;
      }

      let lastError = null;
      let removed = false;
      for (let i = 0; i < deleteCandidates.length; i += 1) {
        try {
          await AppApi.deleteCart(deleteCandidates[i]);
          removed = true;
          break;
        } catch (error) {
          lastError = error;
        }
      }

      if (!removed) {
        App.showToast(App.getApiErrorMessage(lastError, "Không thể xóa sản phẩm"), "error");
        return;
      }

      App.showToast("Đã xóa sản phẩm khỏi giỏ", "success");
      await loadCartAndProfile();
      return;
    }

    const qtyButton = event.target.closest(".qty-minus, .qty-plus");
    if (!qtyButton) return;

    const itemId = qtyButton.getAttribute("data-qty-id");
    const itemIndex = Number(qtyButton.getAttribute("data-item-index"));
    const isPlus = qtyButton.classList.contains("qty-plus");

    const current = cartState.items[itemIndex] || cartState.items.find(function (item) {
      return String(item.id) === String(itemId);
    });
    if (!current) return;

    const nextQty = isPlus ? current.quantity + 1 : Math.max(1, current.quantity - 1);
    if (nextQty > current.stock) {
      App.showToast("Số lượng vượt quá tồn kho", "warning");
      return;
    }

    const updateCandidates = getDeleteIdCandidates(current, itemId);
    if (!updateCandidates.length) {
      App.showToast("Không xác định được sản phẩm để cập nhật", "error");
      return;
    }

    let updatedId = null;
    let updateError = null;
    for (let i = 0; i < updateCandidates.length; i += 1) {
      try {
        await AppApi.updateCart(updateCandidates[i], nextQty);
        updatedId = updateCandidates[i];
        break;
      } catch (error) {
        updateError = error;
      }
    }

    if (!updatedId) {
      App.showToast(App.getApiErrorMessage(updateError, "Không thể cập nhật giỏ hàng"), "error");
      return;
    }

    cartState.items = cartState.items.map(function (item, index) {
      const matchesIndex = Number.isFinite(itemIndex) && index === itemIndex;
      const matchesId = String(item.id) === String(current.id);
      if (!matchesIndex && !matchesId) return item;
      return Object.assign({}, item, {
        quantity: nextQty,
        id: updatedId,
        removeIds: getDeleteIdCandidates(item, updatedId)
      });
    });
    renderCart();
  });
}

function collectCartItemIdCandidates(item) {
  if (!item || item.id === null || item.id === undefined || item.id === "") {
    return [];
  }
  return [String(item.id)];
}

function getDeleteIdCandidates(item, preferredId) {
  const fallback = item && item.id !== undefined && item.id !== null && item.id !== "" ? item.id : null;
  const resolved = preferredId !== undefined && preferredId !== null && preferredId !== "" ? preferredId : fallback;
  if (resolved === null || resolved === undefined || resolved === "") {
    return [];
  }
  return [String(resolved)];
}

function getSelectedItems() {
  return cartState.items.filter(function (item) {
    return item.checked;
  });
}

function fillOrderForm(profile) {
  if (!profile) return;
  const nameInput = document.getElementById("order-name");
  const phoneInput = document.getElementById("order-phone");
  const addressInput = document.getElementById("order-address");

  if (nameInput) nameInput.value = profile.name || "";
  if (phoneInput) phoneInput.value = profile.phone || "";
  if (addressInput) addressInput.value = profile.address || "";
}

async function saveDefaultProfile() {
  const name = String((document.getElementById("order-name") || {}).value || "").trim();
  const phone = String((document.getElementById("order-phone") || {}).value || "").trim();
  const address = String((document.getElementById("order-address") || {}).value || "").trim();

  if (!name || !phone || !address) {
    App.showToast("Vui lòng điền đầy đủ thông tin", "warning");
    return;
  }

  try {
    await AppApi.updateMe({ name: name, phone: phone, address: address });
    App.showToast("Đã lưu thông tin mặc định", "success");
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Lưu thông tin thất bại"), "error");
  }
}

async function submitOrder() {
  const selectedItems = getSelectedItems();
  if (!selectedItems.length) {
    App.showToast("Vui lòng chọn sản phẩm cần đặt", "warning");
    return;
  }

  const shippingName = String((document.getElementById("order-name") || {}).value || "").trim();
  const shippingPhone = String((document.getElementById("order-phone") || {}).value || "").trim();
  const shippingAddress = String((document.getElementById("order-address") || {}).value || "").trim();

  if (!shippingName || !shippingPhone || !shippingAddress) {
    App.showToast("Vui lòng điền đầy đủ thông tin giao hàng", "warning");
    return;
  }

  const selectedCartItemIds = selectedItems
    .map(function (item) {
      return item.id || null;
    })
    .filter(function (id) {
      return id !== null && id !== undefined && id !== "";
    })
    .filter(function (id, index, arr) {
      return arr.indexOf(id) === index;
    });

  if (!selectedCartItemIds.length) {
    App.showToast("Không xác định được sản phẩm để đặt hàng", "error");
    return;
  }

  try {
    const orderResponse = await AppApi.createOrder({
      cartItemIds: selectedCartItemIds,
      shippingName: shippingName,
      shippingPhone: shippingPhone,
      shippingAddress: shippingAddress
    });

    if (orderResponse && orderResponse.checkoutUrl) {
      window.location.href = orderResponse.checkoutUrl;
    } else {
      sessionStorage.setItem("fashon.cart.toast", "Đặt hàng thành công");
      window.location.href = "orders.html";
    }
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Đặt hàng thất bại"), "error");
  }
}
