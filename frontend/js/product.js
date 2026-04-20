let productData = null;
let selectedColor = "";
let selectedSize = "";
let selectedQuantity = 1;
let galleryImages = [];
let activeImageIndex = 0;

document.addEventListener("DOMContentLoaded", async function () {
  await App.mountPublicPage({ showSearch: true, withWhyChoose: true, withFooter: true });

  const productId = App.getQueryParams().get("id");
  if (!productId) {
    window.location.href = "index.html";
    return;
  }

  await loadProduct(productId);
});

async function loadProduct(productId) {
  const container = document.getElementById("product-detail");

  if (!container) return;

  container.innerHTML = '<div class="loading-center"><div class="spinner"></div></div>';

  try {
    productData = await AppApi.getProductDetail(productId);

    document.title = (productData.name || "Sản phẩm") + " - Fashon Shop";

    const images = [productData.thumbnail]
      .concat((productData.images || []).map(function (image) { return image.image; }))
      .filter(Boolean)
      .filter(function (value, index, arr) { return arr.indexOf(value) === index; });
    galleryImages = images.slice();
    activeImageIndex = 0;

    const colors = getUniqueValues((productData.variants || []).map(function (variant) { return variant.color; }));
    selectedColor = colors[0] || "";
    selectedSize = "";
    selectedQuantity = 1;

    const mainImageUrl = AppConfig.buildImageUrl(images[0] || "");

    container.innerHTML = '' +
      '<div class="pd-gallery">' +
      '  <img class="pd-main-img" id="pd-main-image" src="' + mainImageUrl + '" alt="' + App.escapeHtml(productData.name) + '">' +
      '  <div class="pd-thumbs-wrap">' +
      '    <button class="pd-thumb-arrow" type="button" id="thumb-prev">' + App.icon("chevronLeft") + '</button>' +
      '    <div class="pd-thumbs" id="pd-thumbs">' +
      images.map(function (image, index) {
        const imageUrl = AppConfig.buildImageUrl(image);
        return '<img class="pd-thumb ' + (index === 0 ? "active" : "") + '" src="' + imageUrl + '" data-src="' + imageUrl + '" data-index="' + index + '" alt="Ảnh sản phẩm">';
      }).join("") +
      '    </div>' +
      '    <button class="pd-thumb-arrow" type="button" id="thumb-next">' + App.icon("chevronRight") + '</button>' +
      '  </div>' +
      '</div>' +
      '<div class="pd-info">' +
      '  <h1 class="pd-name">' + App.escapeHtml(productData.name) + '</h1>' +
      '  <div class="pd-price-row">' +
      '    <div class="pd-price" id="pd-price">' + App.formatPrice(productData.price) + '</div>' +
      '    <div class="pd-sold">Đã bán: ' + formatSoldCount(productData.soldCount) + '</div>' +
      '  </div>' +
      '  <div class="pd-separator"></div>' +
      '  <div class="pd-desc-section">' +
      '    <h3 class="pd-desc-title">Mô tả sản phẩm</h3>' +
      '    <div class="pd-desc-field"><span class="pd-desc-label">Danh mục:</span> <span class="pd-desc-value">' + App.escapeHtml(productData.category) + '</span></div>' +
      '    <div class="pd-desc-field"><span class="pd-desc-label">Giới tính:</span> <span class="pd-desc-value">' + App.escapeHtml(App.getGenderText(productData.gender)) + '</span></div>' +
      '    <p class="pd-desc-text"><strong>Chi tiết:</strong><br>' + App.escapeHtml(productData.description || "") + '</p>' +
      '  </div>' +
      '  <div class="pd-separator"></div>' +
      '  <div class="pd-section">' +
      '    <div class="pd-section-label">Màu sắc:</div>' +
      '    <div class="pd-colors" id="pd-colors">' +
      colors.map(function (color) {
        return '<button class="pd-color-btn ' + (color === selectedColor ? "active" : "") + '" type="button" data-color="' + App.escapeHtml(color) + '">' + App.escapeHtml(color) + '</button>';
      }).join("") +
      '    </div>' +
      '  </div>' +
      '  <div class="pd-section">' +
      '    <div class="pd-section-label">Size:</div>' +
      '    <div class="pd-sizes" id="pd-sizes"></div>' +
      '  </div>' +
      '  <div class="pd-section">' +
      '    <div class="pd-section-label">Số lượng:</div>' +
      '    <div class="pd-actions">' +
      '      <div class="qty-control">' +
      '        <button type="button" id="qty-minus">-</button>' +
      '        <span id="qty-value">1</span>' +
      '        <button type="button" id="qty-plus">+</button>' +
      '      </div>' +
      '      <button class="pd-add-cart" id="btn-add-cart" type="button">' + App.icon("cart") + '<span>Thêm vào giỏ hàng</span></button>' +
      '    </div>' +
      '    <div class="pd-stock" id="pd-stock"></div>' +
      '  </div>' +
      '</div>';

    bindProductEvents();
    renderSizes();
  } catch (error) {
    container.innerHTML =
      '<div class="empty-state" style="display:flex">' +
      "<h3>Không tìm thấy sản phẩm</h3>" +
      "<p>" + App.escapeHtml(App.getApiErrorMessage(error, "Sản phẩm không tồn tại")) + "</p>" +
      '<a href="index.html" class="btn btn-primary">Quay lại</a>' +
      "</div>";
  }
}

function getUniqueValues(values) {
  return (values || []).filter(function (value, index, arr) {
    return value && arr.indexOf(value) === index;
  });
}

function formatSoldCount(value) {
  const count = Number(value || 0);
  if (count >= 1000) return (count / 1000).toFixed(1) + "k";
  return String(count);
}

function bindProductEvents() {
  Array.from(document.querySelectorAll(".pd-thumb")).forEach(function (thumb) {
    thumb.addEventListener("click", function () {
      setActiveImage(Number(thumb.getAttribute("data-index")));
    });
  });

  const prev = document.getElementById("thumb-prev");
  const next = document.getElementById("thumb-next");

  if (prev) {
    if (galleryImages.length <= 1) {
      prev.style.display = "none";
    }
    prev.addEventListener("click", function () {
      setActiveImage(activeImageIndex - 1);
    });
  }

  if (next) {
    if (galleryImages.length <= 1) {
      next.style.display = "none";
    }
    next.addEventListener("click", function () {
      setActiveImage(activeImageIndex + 1);
    });
  }

  Array.from(document.querySelectorAll(".pd-color-btn")).forEach(function (button) {
    button.addEventListener("click", function () {
      selectedColor = button.getAttribute("data-color");
      selectedSize = "";
      Array.from(document.querySelectorAll(".pd-color-btn")).forEach(function (other) {
        other.classList.remove("active");
      });
      button.classList.add("active");
      renderSizes();
    });
  });

  const minusButton = document.getElementById("qty-minus");
  const plusButton = document.getElementById("qty-plus");

  if (minusButton) {
    minusButton.addEventListener("click", function () {
      selectedQuantity = Math.max(1, selectedQuantity - 1);
      updateQuantity();
    });
  }

  if (plusButton) {
    plusButton.addEventListener("click", function () {
      selectedQuantity += 1;
      updateQuantity();
    });
  }

  const addCartButton = document.getElementById("btn-add-cart");
  if (addCartButton) {
    addCartButton.addEventListener("click", async function () {
      const variant = findSelectedVariant();
      if (!variant) {
        App.showToast("Vui lòng chọn đầy đủ màu sắc và size", "warning");
        return;
      }

      if (!App.ensureLoggedIn()) {
        return;
      }

      try {
        await AppApi.addToCart(variant.id, selectedQuantity);
        App.showToast("Đã thêm vào giỏ hàng", "success");
      } catch (error) {
        App.showToast(App.getApiErrorMessage(error, "Thêm vào giỏ thất bại"), "error");
      }
    });
  }
}

function setActiveImage(index) {
  if (!galleryImages.length) return;

  const total = galleryImages.length;
  let nextIndex = Number(index);

  if (Number.isNaN(nextIndex)) return;
  if (nextIndex < 0) nextIndex = total - 1;
  if (nextIndex >= total) nextIndex = 0;

  activeImageIndex = nextIndex;

  const mainImage = document.getElementById("pd-main-image");
  if (mainImage) {
    mainImage.src = AppConfig.buildImageUrl(galleryImages[activeImageIndex] || "");
  }

  Array.from(document.querySelectorAll(".pd-thumb")).forEach(function (thumb) {
    const thumbIndex = Number(thumb.getAttribute("data-index"));
    thumb.classList.toggle("active", thumbIndex === activeImageIndex);
  });
}

function renderSizes() {
  const sizesWrap = document.getElementById("pd-sizes");
  if (!sizesWrap) return;

  const variants = (productData.variants || []).filter(function (variant) {
    return variant.color === selectedColor;
  });

  sizesWrap.innerHTML = variants
    .map(function (variant) {
      const disabled = Number(variant.stock || 0) <= 0;
      return '<button class="pd-size-btn ' + (selectedSize === variant.size ? "active" : "") + ' ' + (disabled ? "disabled" : "") + '" type="button" data-size="' + App.escapeHtml(variant.size) + '" ' + (disabled ? "disabled" : "") + '>' + App.escapeHtml(variant.size) + "</button>";
    })
    .join("");

  Array.from(sizesWrap.querySelectorAll(".pd-size-btn:not(.disabled)"))
    .forEach(function (button) {
      button.addEventListener("click", function () {
        selectedSize = button.getAttribute("data-size");
        Array.from(sizesWrap.querySelectorAll(".pd-size-btn")).forEach(function (other) {
          other.classList.remove("active");
        });
        button.classList.add("active");
        updateVariantInfo();
      });
    });

  updateVariantInfo();
}

function findSelectedVariant() {
  return (productData.variants || []).find(function (variant) {
    return variant.color === selectedColor && variant.size === selectedSize;
  }) || null;
}

function updateQuantity() {
  const qtyValue = document.getElementById("qty-value");
  if (qtyValue) qtyValue.textContent = String(selectedQuantity);
}

function updateVariantInfo() {
  const stock = document.getElementById("pd-stock");
  const price = document.getElementById("pd-price");
  const variant = findSelectedVariant();

  if (!stock || !price) return;

  if (!variant) {
    stock.textContent = "";
    price.textContent = App.formatPrice(productData.price);
    return;
  }

  const finalPrice = variant.priceOverride == null ? productData.price : variant.priceOverride;
  price.textContent = App.formatPrice(finalPrice);
  if (Number(variant.stock || 0) <= 0) {
    stock.className = "pd-stock out";
    stock.textContent = "Hết hàng";
  } else {
    stock.className = "pd-stock";
    stock.textContent = "Còn " + variant.stock + " sản phẩm";
  }
}
