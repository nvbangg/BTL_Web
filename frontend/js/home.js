document.addEventListener("DOMContentLoaded", async function () {
  await App.mountPublicPage({ showSearch: true, withWhyChoose: true, withFooter: true });

  const state = readStateFromQuery();
  bindSortPills(state);
  bindFilterDrawer(state);
  bindFilterActions(state);

  await Promise.all([
    loadFilterOptions(state),
    loadProducts(state)
  ]);
});

function readStateFromQuery() {
  const params = App.getQueryParams();
  return {
    keyword: params.get("keyword") || "",
    category: params.get("category") || "",
    gender: params.get("gender") || "",
    color: params.get("color") || "",
    size: params.get("size") || "",
    minPrice: params.get("minPrice") || "",
    maxPrice: params.get("maxPrice") || "",
    sort: params.get("sort") || "hot",
    page: Number(params.get("page") || 1),
    pageSize: Number(params.get("pageSize") || 16)
  };
}

function toProductsQueryString(state) {
  const params = new URLSearchParams();
  if (state.keyword) params.set("keyword", state.keyword);
  if (state.category) params.set("category", state.category);
  if (state.gender) params.set("gender", state.gender);
  if (state.color) params.set("color", state.color);
  if (state.size) params.set("size", state.size);
  if (state.minPrice) params.set("minPrice", state.minPrice);
  if (state.maxPrice) params.set("maxPrice", state.maxPrice);
  if (state.sort) params.set("sort", state.sort);
  params.set("page", String(state.page || 1));
  params.set("pageSize", String(state.pageSize || 16));
  return "?" + params.toString();
}

function bindSortPills(state) {
  const pills = Array.from(document.querySelectorAll(".sort-pill"));
  pills.forEach(function (pill) {
    if (pill.getAttribute("data-sort") === state.sort) {
      pill.classList.add("active");
    } else {
      pill.classList.remove("active");
    }

    pill.addEventListener("click", function () {
      App.updateQuery({
        sort: pill.getAttribute("data-sort"),
        page: 1
      });
    });
  });
}

function bindFilterDrawer() {
  const openButton = document.getElementById("btn-filter");
  const closeButton = document.getElementById("filter-close");
  const overlay = document.getElementById("filter-overlay");
  const panel = document.getElementById("filter-panel");

  if (!openButton || !closeButton || !overlay || !panel) return;

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
}

function bindFilterActions(state) {
  const applyButton = document.getElementById("filter-apply");
  const resetButton = document.getElementById("filter-reset");

  if (applyButton) {
    applyButton.addEventListener("click", function () {
      const category = document.getElementById("filter-category").value || "";
      const gender = document.getElementById("filter-gender").value || "";
      const color = document.getElementById("filter-color").value || "";
      const size = document.getElementById("filter-size").value || "";
      const minPrice = (document.getElementById("filter-min-price").value || "").trim();
      const maxPrice = (document.getElementById("filter-max-price").value || "").trim();

      App.updateQuery({
        category: category,
        gender: gender,
        color: color,
        size: size,
        minPrice: minPrice,
        maxPrice: maxPrice,
        page: 1
      });
    });
  }

  if (resetButton) {
    resetButton.addEventListener("click", function () {
      App.updateQuery({
        category: "",
        gender: "",
        color: "",
        size: "",
        minPrice: "",
        maxPrice: "",
        page: 1
      });
    });
  }

  const categoryInput = document.getElementById("filter-category");
  const genderInput = document.getElementById("filter-gender");
  const colorInput = document.getElementById("filter-color");
  const sizeInput = document.getElementById("filter-size");
  const minInput = document.getElementById("filter-min-price");
  const maxInput = document.getElementById("filter-max-price");

  if (categoryInput) categoryInput.value = state.category;
  if (genderInput) genderInput.value = state.gender;
  if (colorInput) colorInput.value = state.color;
  if (sizeInput) sizeInput.value = state.size;
  if (minInput) minInput.value = state.minPrice;
  if (maxInput) maxInput.value = state.maxPrice;
}

async function loadFilterOptions(state) {
  try {
    const filters = await AppApi.getProductFilters();

    fillSelect("filter-category", filters.categories || [], state.category);
    fillSelect("filter-gender", filters.genders || ["male", "female", "unisex"], state.gender);
    fillSelect("filter-color", filters.colors || [], state.color);
    fillSelect("filter-size", filters.sizes || [], state.size);
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Không tải được bộ lọc"), "error");
  }
}

function fillSelect(selectId, items, selectedValue) {
  const select = document.getElementById(selectId);
  if (!select) return;

  const options = ['<option value="">Chọn</option>'];
  (items || []).forEach(function (item) {
    const value = String(item);
    const selected = value === String(selectedValue || "") ? "selected" : "";
    const label = App.getGenderText(value);
    options.push('<option value="' + App.escapeHtml(value) + '" ' + selected + '>' + App.escapeHtml(label) + "</option>");
  });

  select.innerHTML = options.join("");
}

async function loadProducts(state) {
  const grid = document.getElementById("product-grid");
  const emptyState = document.getElementById("empty-state");
  const paging = document.getElementById("pagination-area");

  if (!grid || !emptyState || !paging) return;

  grid.innerHTML = '<div class="loading-center"><div class="spinner"></div></div>';

  try {
    const data = await AppApi.getProducts(toProductsQueryString(state));
    const items = data.items || [];

    if (!items.length) {
      grid.innerHTML = "";
      emptyState.style.display = "flex";
      paging.innerHTML = "";
      return;
    }

    emptyState.style.display = "none";
    grid.innerHTML = items
      .map(function (item, index) {
        const imageUrl = AppConfig.buildImageUrl(item.thumbnail);
        const pastel = ["#FCE7F3", "#FFE4E6", "#FBCFE8", "#E2E8F0"][index % 4];
        const productHref = "product.html?id=" + encodeURIComponent(item.id);

        return '' +
          '<a class="product-card" href="' + productHref + '">' +
          '  <img class="product-card-img" src="' + imageUrl + '" alt="' + App.escapeHtml(item.name) + '" style="background:' + pastel + '" onerror="this.style.background=\'' + pastel + '\';this.removeAttribute(\'src\')">' +
          '  <div class="product-card-body">' +
          '    <div class="product-card-price">' + App.formatPrice(item.price) + '</div>' +
          '    <div class="product-card-name">' + App.escapeHtml(item.name) + '</div>' +
          '  </div>' +
          '</a>';
      })
      .join("");

    App.renderPagination(paging, data.page, data.pageSize, data.total, function (nextPage) {
      App.updateQuery({ page: nextPage });
    });
  } catch (error) {
    grid.innerHTML = "";
    emptyState.style.display = "flex";
    emptyState.querySelector("p").textContent = App.getApiErrorMessage(error, "Không tải được sản phẩm");
    paging.innerHTML = "";
  }
}
