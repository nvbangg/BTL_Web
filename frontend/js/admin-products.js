let productState = {
  keyword: "",
  category: "",
  gender: "",
  color: "",
  size: "",
  minPrice: "",
  maxPrice: "",
  sort: "newest",
  isActive: "",
  page: 1,
  pageSize: 10,
  filters: null,
  data: null,
  modalProductId: null,
  modalImages: [],
  modalVariants: []
};

document.addEventListener("DOMContentLoaded", async function () {
  document.addEventListener("app:auth-changed", function () {
    window.location.reload();
  });

  const user = await App.mountAdminPage({ activeTab: "products" });
  if (!user) return;

  initStateFromQuery();
  bindSearchAndSort();
  bindFilterPanel();
  bindModalEvents();

  await loadFilters();
  await loadProducts();
});

function initStateFromQuery() {
  const params = App.getQueryParams();
  productState.keyword = params.get("keyword") || "";
  productState.category = params.get("category") || "";
  productState.gender = params.get("gender") || "";
  productState.color = params.get("color") || "";
  productState.size = params.get("size") || "";
  productState.minPrice = params.get("minPrice") || "";
  productState.maxPrice = params.get("maxPrice") || "";
  productState.sort = params.get("sort") || "newest";
  productState.isActive = params.get("isActive") || "";
  productState.page = Number(params.get("page") || 1);
  productState.pageSize = Number(params.get("pageSize") || 10);

  const search = document.getElementById("product-search");
  if (search) search.value = productState.keyword;

  Array.from(document.querySelectorAll(".sort-chip[data-sort]")).forEach(function (button) {
    button.classList.toggle("sort-chip-active", button.getAttribute("data-sort") === productState.sort);
  });
}

function buildListQuery() {
  const params = new URLSearchParams();
  if (productState.keyword) params.set("keyword", productState.keyword);
  if (productState.category) params.set("category", productState.category);
  if (productState.gender) params.set("gender", productState.gender);
  if (productState.color) params.set("color", productState.color);
  if (productState.size) params.set("size", productState.size);
  if (productState.minPrice) params.set("minPrice", productState.minPrice);
  if (productState.maxPrice) params.set("maxPrice", productState.maxPrice);
  if (productState.sort) params.set("sort", productState.sort);
  if (productState.isActive) params.set("isActive", productState.isActive);
  params.set("page", String(productState.page));
  params.set("pageSize", String(productState.pageSize));
  return "?" + params.toString();
}

async function loadFilters() {
  try {
    productState.filters = await AppApi.getProductFilters();
    fillSelect("admin-filter-category", productState.filters.categories || [], productState.category, true);
    fillSelect("admin-filter-gender", ["male", "female", "unisex"], productState.gender, true);
    fillSelect("admin-filter-color", productState.filters.colors || [], productState.color, true);
    fillSelect("admin-filter-size", productState.filters.sizes || [], productState.size, true);

    const categoryList = document.getElementById("modal-category-list");
    if (categoryList) {
      categoryList.innerHTML = (productState.filters.categories || []).map(function (item) {
        return '<option value="' + App.escapeHtml(item) + '"></option>';
      }).join("");
    }
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Không tải được bộ lọc"), "error");
  }
}

function fillSelect(selectId, items, selected, hasAllOption) {
  const select = document.getElementById(selectId);
  if (!select) return;

  const options = [];
  if (hasAllOption) {
    options.push('<option value="">Tất cả</option>');
  }

  (items || []).forEach(function (item) {
    const value = String(item);
    const isSelected = value === String(selected || "") ? "selected" : "";
    options.push('<option value="' + App.escapeHtml(value) + '" ' + isSelected + '>' + App.escapeHtml(App.getGenderText(value)) + "</option>");
  });

  select.innerHTML = options.join("");
}

async function loadProducts() {
  const body = document.getElementById("products-tbody");
  const paging = document.getElementById("products-pagination");

  if (!body || !paging) return;

  body.innerHTML = '<tr><td colspan="5"><div class="loading-center"><div class="spinner"></div></div></td></tr>';

  try {
    const data = await AppApi.getAdminProducts(buildListQuery());
    productState.data = data;
    renderStats(data);

    if (!(data.items || []).length) {
      body.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:28px;color:#64748B">Không có sản phẩm</td></tr>';
      paging.innerHTML = "";
      return;
    }

    body.innerHTML = (data.items || []).map(function (item) {
      const stockColor = Number(item.totalStock || 0) === 0 ? "#EF4444" : "#16A34A";
      const rowClass = item.isActive === false ? "admin-product-row inactive" : "admin-product-row";
      return '' +
        '<tr class="' + rowClass + '">' +
        '  <td>' +
        '    <div style="display:flex;align-items:center;gap:12px">' +
        '      <img class="table-product-img" src="' + AppConfig.buildImageUrl(item.thumbnail) + '" alt="' + App.escapeHtml(item.name) + '">' +
        '      <strong>' + App.escapeHtml(item.name) + '</strong>' +
        '    </div>' +
        '  </td>' +
        '  <td style="font-weight:700">' + App.formatPrice(item.price) + '</td>' +
        '  <td>' + Number(item.soldCount || 0).toLocaleString("vi-VN") + '</td>' +
        '  <td style="font-weight:700;color:' + stockColor + '">' + Number(item.totalStock || 0) + '</td>' +
        '  <td><button class="btn-edit-icon" type="button" data-edit-product="' + item.id + '" aria-label="Chỉnh sửa sản phẩm">' + App.icon("pencil") + '</button></td>' +
        '</tr>';
    }).join("");

    Array.from(body.querySelectorAll("[data-edit-product]")).forEach(function (button) {
      button.addEventListener("click", function () {
        openProductModal(Number(button.getAttribute("data-edit-product")));
      });
    });

    App.renderPagination(paging, data.page, data.pageSize, data.total, function (nextPage) {
      App.updateQuery({ page: nextPage, pageSize: data.pageSize });
    });
  } catch (error) {
    body.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:28px;color:#EF4444">' + App.escapeHtml(App.getApiErrorMessage(error, "Không tải được sản phẩm")) + "</td></tr>";
    paging.innerHTML = "";
  }
}

function renderStats(data) {
  const root = document.getElementById("product-stats");
  if (!root) return;

  root.innerHTML = '' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#F1F5F9;color:#475569">' + App.icon("bag") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Tổng số sản phẩm</div><div class="admin-stat-value">' + Number(data.total || 0).toLocaleString("vi-VN") + '</div></div></div>' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#DCFCE7;color:#15803D">' + App.icon("checkCircle") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Sản phẩm đang kinh doanh</div><div class="admin-stat-value">' + Number(data.totalActiveProducts || 0).toLocaleString("vi-VN") + '</div></div></div>' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#FEE2E2;color:#DC2626">' + App.icon("alertCircle") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Sản phẩm hết hàng</div><div class="admin-stat-value" style="color:#EF4444">' + Number(data.totalOutOfStockProducts || 0).toLocaleString("vi-VN") + '</div></div></div>';
}

function bindSearchAndSort() {
  const searchInput = document.getElementById("product-search");
  let timer = null;

  if (searchInput) {
    searchInput.addEventListener("input", function () {
      clearTimeout(timer);
      timer = setTimeout(function () {
        App.updateQuery({ keyword: searchInput.value.trim(), page: 1 });
      }, 350);
    });
  }

  Array.from(document.querySelectorAll(".sort-chip[data-sort]")).forEach(function (button) {
    button.addEventListener("click", function () {
      App.updateQuery({ sort: button.getAttribute("data-sort"), page: 1 });
    });
  });

  const addButton = document.getElementById("btn-add-product");
  if (addButton) {
    addButton.addEventListener("click", function () {
      openProductModal(null);
    });
  }
}

function bindFilterPanel() {
  const openButton = document.getElementById("btn-open-admin-filter");
  const closeButton = document.getElementById("admin-filter-close");
  const overlay = document.getElementById("filter-overlay");
  const panel = document.getElementById("admin-filter-panel");
  const applyButton = document.getElementById("admin-filter-apply");
  const resetButton = document.getElementById("admin-filter-reset");

  if (!openButton || !closeButton || !overlay || !panel || !applyButton || !resetButton) return;

  document.getElementById("admin-filter-category").value = productState.category;
  document.getElementById("admin-filter-gender").value = productState.gender;
  document.getElementById("admin-filter-color").value = productState.color;
  document.getElementById("admin-filter-size").value = productState.size;
  document.getElementById("admin-filter-min-price").value = productState.minPrice;
  document.getElementById("admin-filter-max-price").value = productState.maxPrice;
  document.getElementById("admin-filter-is-active").value = productState.isActive;

  function openPanel() {
    panel.classList.add("show");
    overlay.classList.add("show");
  }

  function closePanel() {
    panel.classList.remove("show");
    overlay.classList.remove("show");
  }

  openButton.addEventListener("click", openPanel);
  closeButton.addEventListener("click", closePanel);
  overlay.addEventListener("click", closePanel);

  applyButton.addEventListener("click", function () {
    App.updateQuery({
      category: document.getElementById("admin-filter-category").value,
      gender: document.getElementById("admin-filter-gender").value,
      color: document.getElementById("admin-filter-color").value,
      size: document.getElementById("admin-filter-size").value,
      minPrice: String(document.getElementById("admin-filter-min-price").value || "").trim(),
      maxPrice: String(document.getElementById("admin-filter-max-price").value || "").trim(),
      isActive: document.getElementById("admin-filter-is-active").value,
      page: 1
    });
  });

  resetButton.addEventListener("click", function () {
    App.updateQuery({
      category: "",
      gender: "",
      color: "",
      size: "",
      minPrice: "",
      maxPrice: "",
      isActive: "",
      page: 1
    });
  });
}

function bindModalEvents() {
  const modal = document.getElementById("edit-product-modal");
  const closeButton = document.getElementById("edit-product-close");
  const saveButton = document.getElementById("modal-save-product");
  const addVariantButton = document.getElementById("modal-add-variant");
  const addImageButton = document.getElementById("modal-add-image");

  if (!modal || !closeButton || !saveButton || !addVariantButton || !addImageButton) return;

  closeButton.addEventListener("click", closeProductModal);
  modal.addEventListener("click", function (event) {
    if (event.target === modal) {
      closeProductModal();
    }
  });

  addVariantButton.addEventListener("click", function () {
    productState.modalVariants.push({ id: null, color: "", size: "", stock: 0, priceOverride: "" });
    renderVariantRows();
  });

  addImageButton.addEventListener("click", function () {
    productState.modalImages.push({ id: null, image: "", sortOrder: productState.modalImages.length + 1 });
    renderImageRows();
  });

  saveButton.addEventListener("click", saveProduct);
}

async function openProductModal(productId) {
  const title = document.getElementById("edit-product-title");
  const modal = document.getElementById("edit-product-modal");
  if (!title || !modal) return;

  productState.modalProductId = productId;
  productState.modalImages = [];
  productState.modalVariants = [];

  if (!productId) {
    title.textContent = "Thêm sản phẩm";
    fillModalFields({
      name: "",
      description: "",
      thumbnail: "",
      category: "",
      gender: "male",
      price: "",
      isActive: true,
      images: [],
      variants: [{ id: null, color: "", size: "", stock: 0, priceOverride: "" }]
    });
    modal.classList.add("show");
    return;
  }

  title.textContent = "Chỉnh sửa sản phẩm";

  try {
    const detail = await AppApi.getAdminProductDetail(productId);
    fillModalFields(detail);
    modal.classList.add("show");
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Không tải được chi tiết sản phẩm"), "error");
  }
}

function fillModalFields(product) {
  document.getElementById("modal-name").value = product.name || "";
  document.getElementById("modal-description").value = product.description || "";
  document.getElementById("modal-thumbnail").value = product.thumbnail || "";
  document.getElementById("modal-category").value = product.category || "";
  document.getElementById("modal-gender").value = product.gender || "male";
  document.getElementById("modal-price").value = product.price || "";
  document.getElementById("modal-is-active").checked = product.isActive !== false;

  productState.modalImages = (product.images || []).map(function (item, index) {
    return {
      id: item.id || null,
      image: item.image || "",
      sortOrder: item.sortOrder || (index + 1)
    };
  });

  if (!productState.modalImages.length && product.thumbnail) {
    productState.modalImages.push({ id: null, image: product.thumbnail, sortOrder: 1 });
  }

  productState.modalVariants = (product.variants || []).map(function (variant) {
    return {
      id: variant.id || null,
      color: variant.color || "",
      size: variant.size || "",
      stock: Number(variant.stock || 0),
      priceOverride: variant.priceOverride == null ? "" : Number(variant.priceOverride)
    };
  });

  renderImageRows();
  renderVariantRows();
}

function renderImageRows() {
  const body = document.getElementById("modal-image-rows");
  if (!body) return;

  body.innerHTML = productState.modalImages.map(function (image, index) {
    return '' +
      '<div class="modal-row-inline">' +
      '  <input class="form-input" data-image-index="' + index + '" value="' + App.escapeHtml(image.image) + '" placeholder="Tên file ảnh, vd: ao-nam1.1.png">' +
      '  <span class="modal-row-hint">Thứ tự: ' + (index + 1) + '</span>' +
      '</div>';
  }).join("");

  Array.from(body.querySelectorAll("[data-image-index]")).forEach(function (input) {
    input.addEventListener("input", function () {
      const index = Number(input.getAttribute("data-image-index"));
      productState.modalImages[index].image = input.value.trim();
    });
  });
}

function renderVariantRows() {
  const body = document.getElementById("modal-variant-rows");
  if (!body) return;

  body.innerHTML = productState.modalVariants.map(function (variant, index) {
    return '' +
      '<tr>' +
      '  <td><input class="form-input" data-variant-color="' + index + '" value="' + App.escapeHtml(variant.color) + '" placeholder="Màu sắc"></td>' +
      '  <td><input class="form-input" data-variant-size="' + index + '" value="' + App.escapeHtml(variant.size) + '" placeholder="Size"></td>' +
      '  <td><input class="form-input" type="number" min="0" data-variant-stock="' + index + '" value="' + variant.stock + '"></td>' +
        '  <td><input class="form-input" type="number" min="0" data-variant-price="' + index + '" value="' + variant.priceOverride + '" placeholder="Để trống = giá cơ bản"></td>' +
      '</tr>';
  }).join("");

  Array.from(body.querySelectorAll("[data-variant-color]")).forEach(function (input) {
    input.addEventListener("input", function () {
      const index = Number(input.getAttribute("data-variant-color"));
      productState.modalVariants[index].color = input.value.trim();
    });
  });

  Array.from(body.querySelectorAll("[data-variant-size]")).forEach(function (input) {
    input.addEventListener("input", function () {
      const index = Number(input.getAttribute("data-variant-size"));
      productState.modalVariants[index].size = input.value.trim();
    });
  });

  Array.from(body.querySelectorAll("[data-variant-stock]")).forEach(function (input) {
    input.addEventListener("input", function () {
      const index = Number(input.getAttribute("data-variant-stock"));
      productState.modalVariants[index].stock = Number(input.value || 0);
    });
  });

  Array.from(body.querySelectorAll("[data-variant-price]")).forEach(function (input) {
    input.addEventListener("input", function () {
      const index = Number(input.getAttribute("data-variant-price"));
      productState.modalVariants[index].priceOverride = input.value === "" ? "" : Number(input.value);
    });
  });
}

function closeProductModal() {
  const modal = document.getElementById("edit-product-modal");
  if (modal) modal.classList.remove("show");
}

async function saveProduct() {
  const payload = buildProductPayload();
  if (!payload) return;

  try {
    if (productState.modalProductId) {
      await AppApi.updateAdminProduct(productState.modalProductId, payload);
      App.showToast("Đã cập nhật sản phẩm", "success");
    } else {
      await AppApi.createAdminProduct(payload);
      App.showToast("Đã tạo sản phẩm", "success");
    }

    closeProductModal();
    await loadProducts();
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Lưu sản phẩm thất bại"), "error");
  }
}

function buildProductPayload() {
  const name = String(document.getElementById("modal-name").value || "").trim();
  const description = String(document.getElementById("modal-description").value || "").trim();
  const thumbnail = String(document.getElementById("modal-thumbnail").value || "").trim();
  const category = String(document.getElementById("modal-category").value || "").trim();
  const gender = String(document.getElementById("modal-gender").value || "").trim();
  const priceRaw = String(document.getElementById("modal-price").value || "").trim();
  const isActive = !!document.getElementById("modal-is-active").checked;

  const price = Number(priceRaw || 0);

  if (!name || !thumbnail || !category || !gender || price <= 0) {
    App.showToast("Vui lòng điền đầy đủ thông tin bắt buộc", "warning");
    return null;
  }

  const variants = productState.modalVariants
    .filter(function (variant) {
      return variant.color && variant.size;
    })
    .map(function (variant) {
      const payloadVariant = {
        color: variant.color,
        size: variant.size,
        stock: Number(variant.stock || 0)
      };
      if (variant.priceOverride !== "") {
        payloadVariant.priceOverride = Number(variant.priceOverride);
      }
      if (variant.id) {
        payloadVariant.id = variant.id;
      }
      return payloadVariant;
    });

  if (!variants.length) {
    App.showToast("Sản phẩm phải có ít nhất một phân loại", "warning");
    return null;
  }

  const images = productState.modalImages
    .filter(function (image) {
      return image.image;
    })
    .map(function (image, index) {
      const payloadImage = {
        image: image.image,
        sortOrder: index + 1
      };
      if (image.id) {
        payloadImage.id = image.id;
      }
      return payloadImage;
    });

  return {
    name: name,
    description: description || null,
    thumbnail: thumbnail,
    category: category,
    gender: gender,
    price: price,
    isActive: isActive,
    images: images,
    variants: variants
  };
}
