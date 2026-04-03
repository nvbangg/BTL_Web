/* ===== PRODUCT DETAIL PAGE ===== */
let product = null;
let selectedColor = '';
let selectedSize = '';
let selectedQty = 1;
let categoryMap = {};

document.addEventListener('DOMContentLoaded', async () => {
  await initPage();

  const id = getParam('id');
  if (!id) return (window.location.href = '../');

  try {
    const data = await apiGetProductDetail(id);
    product = data;

    const filtersResp = await apiGetProductFilters();
    if (filtersResp.categories) {
      categoryMap = Object.fromEntries(filtersResp.categories.map((c) => [c.id, c.name]));
    }

    if (!product) return (window.location.href = '../');

    document.title = product.name + ' - Fashon Shop';
    document.getElementById('breadcrumb-name').textContent = product.name;
    renderDetail();
  } catch (error) {
    console.error('Failed to load product', error);
    window.location.href = '../';
  }
});

function renderDetail() {
  const colors = getProductColors(product);
  selectedColor = colors[0] || '';
  selectedQty = 1;
  const totalStock = getTotalStock(product);
  const categoryName = categoryMap[product.categoryId] || '';
  const productImages = product.images || [];

  const container = document.getElementById('product-detail');
  container.innerHTML = `
    <div class="pd-gallery">
      <img class="pd-main-img" id="main-img" src="../${productImages[0]?.image || product.thumbnail}" alt="${product.name}">
      <div class="pd-thumbs-wrap">
        <button class="pd-thumb-arrow" id="thumb-prev">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <div class="pd-thumbs">
          ${productImages.map((img, i) => `<img class="pd-thumb ${i === 0 ? 'active' : ''}" src="../${img.image}" data-idx="${i}" alt="">`).join('')}
        </div>
        <button class="pd-thumb-arrow" id="thumb-next">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>
    </div>
    <div class="pd-info">
      <h1 class="pd-name">${product.name}</h1>
      <div class="pd-price-row">
        <div class="pd-price" id="pd-price">${formatPrice(product.price)}</div>
        <div class="pd-sold">Đã bán: ${product.soldCount >= 1000 ? (product.soldCount / 1000).toFixed(1) + 'k' : product.soldCount || 0}</div>
      </div>
      <div class="pd-separator"></div>

      <div class="pd-desc-section">
        <h3 class="pd-desc-title">Mô tả sản phẩm</h3>
        <div class="pd-desc-field"><span class="pd-desc-label">Danh mục:</span> <span class="pd-desc-value">${categoryName}</span></div>
        <div class="pd-desc-field"><span class="pd-desc-label">Giới tính:</span> <span class="pd-desc-value">${product.gender}</span></div>
        <p class="pd-desc-text"><strong>Chi tiết:</strong><br>${product.description}</p>
      </div>

      <div class="pd-separator"></div>

      <div class="pd-section">
        <div class="pd-section-label">Màu sắc:</div>
        <div class="pd-colors" id="pd-colors">
          ${colors.map((c) => `<button class="pd-color-btn ${c === selectedColor ? 'active' : ''}" data-color="${c}">${c}</button>`).join('')}
        </div>
      </div>

      <div class="pd-section">
        <div class="pd-section-label">Size:</div>
        <div class="pd-sizes" id="pd-sizes"></div>
        <div class="pd-stock" id="pd-stock"></div>
      </div>

      <div class="pd-section">
        <div class="pd-section-label">Số lượng:</div>
        <div class="pd-actions">
          <div class="qty-control">
            <button type="button" id="qty-minus">−</button>
            <span id="qty-value">1</span>
            <button type="button" id="qty-plus">+</button>
          </div>
          <button class="pd-add-cart" id="btn-add-cart" ${totalStock === 0 ? 'disabled' : ''}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/></svg>
            ${totalStock === 0 ? 'Hết hàng' : 'Thêm vào giỏ hàng'}
          </button>
        </div>
      </div>
    </div>
  `;

  // Thumbnail click
  container.querySelectorAll('.pd-thumb').forEach((thumb) => {
    thumb.addEventListener('click', () => {
      container.querySelectorAll('.pd-thumb').forEach((t) => t.classList.remove('active'));
      thumb.classList.add('active');
      document.getElementById('main-img').src = thumb.src;
    });
  });

  // Thumbnail arrows
  document.getElementById('thumb-prev')?.addEventListener('click', () => scrollThumbs(-1));
  document.getElementById('thumb-next')?.addEventListener('click', () => scrollThumbs(1));

  // Color buttons
  container.querySelectorAll('.pd-color-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      selectedColor = btn.dataset.color;
      container.querySelectorAll('.pd-color-btn').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      selectedSize = '';
      renderSizes();
    });
  });

  // Quantity buttons
  document.getElementById('qty-minus')?.addEventListener('click', () => {
    if (selectedQty > 1) {
      selectedQty--;
      document.getElementById('qty-value').textContent = selectedQty;
    }
  });
  document.getElementById('qty-plus')?.addEventListener('click', () => {
    selectedQty++;
    document.getElementById('qty-value').textContent = selectedQty;
  });

  // Add to cart
  document.getElementById('btn-add-cart')?.addEventListener('click', async () => {
    if (!selectedSize) {
      showToast('Vui lòng chọn size trước khi thêm vào giỏ hàng', 'warning');
      return;
    }
    const variant = findVariant(product, selectedColor, selectedSize);
    if (!variant || variant.stock === 0) {
      showToast('Sản phẩm đã hết hàng', 'error');
      return;
    }
    
    const user = getCurrentUser();
    if (!user) {
      showToast('Vui lòng đăng nhập trước', 'warning');
      openAuth();
      return;
    }

    try {
      await apiAddToCart(variant.id, selectedQty);
      showToast(`Đã thêm ${selectedQty} "${product.name}" (${selectedColor}/${selectedSize}) vào giỏ hàng`, 'success');
    } catch (error) {
      showToast('Lỗi khi thêm vào giỏ hàng', 'error');
    }
  });

  renderSizes();
}

function scrollThumbs(dir) {
  const thumbsContainer = document.querySelector('.pd-thumbs');
  if (thumbsContainer) {
    thumbsContainer.scrollBy({ left: dir * 110, behavior: 'smooth' });
  }
}

function renderSizes() {
  const sizes = getProductSizes(product, selectedColor);
  const sizesEl = document.getElementById('pd-sizes');

  sizesEl.innerHTML = sizes.map((s) => {
    const variant = findVariant(product, selectedColor, s);
    const oos = !variant || variant.stock === 0;
    return `<button class="pd-size-btn ${oos ? 'disabled' : ''} ${s === selectedSize ? 'active' : ''}" data-size="${s}" ${oos ? 'disabled' : ''}>${s}</button>`;
  }).join('');

  sizesEl.querySelectorAll('.pd-size-btn:not(.disabled)').forEach((btn) => {
    btn.addEventListener('click', () => {
      selectedSize = btn.dataset.size;
      sizesEl.querySelectorAll('.pd-size-btn').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      updateStockDisplay();
    });
  });

  updateStockDisplay();
}

function updateStockDisplay() {
  const stockEl = document.getElementById('pd-stock');
  const priceEl = document.getElementById('pd-price');
  if (!selectedSize) {
    stockEl.textContent = '';
    priceEl.textContent = formatPrice(product.basePrice);
    return;
  }

  const variant = findVariant(product, selectedColor, selectedSize);
  if (!variant) return;
  priceEl.textContent = formatPrice(getVariantPrice(variant, product.basePrice));
  stockEl.textContent = variant.stock === 0 ? 'Hết hàng' : `Còn ${variant.stock} sản phẩm`;
  stockEl.className = variant.stock === 0 ? 'pd-stock out' : 'pd-stock';
}
