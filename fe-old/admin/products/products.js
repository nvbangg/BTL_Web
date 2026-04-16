/* ===== ADMIN PRODUCTS PAGE ===== */
let productFilters = { cats: [], genders: [], statuses: [] };

document.addEventListener('DOMContentLoaded', async () => {
  await initAdminPage('products');
  renderProductStats();
  renderProductTable();
  bindProductEvents();
});

function renderProductStats() {
  const products = mockGetProducts();
  const active = products.filter(p => p.active !== false).length;
  const oos = products.filter(p => getTotalStock(p) === 0).length;

  document.getElementById('product-stats').innerHTML = `
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#F1F5F9">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 2 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 22 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
      </div>
      <div class="admin-stat-info"><div class="admin-stat-label">Tổng số sản phẩm</div><div class="admin-stat-value">${products.length}</div></div>
    </div>
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#DCFCE7">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#16A34A" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
      </div>
      <div class="admin-stat-info"><div class="admin-stat-label">Sản phẩm đang kinh doanh</div><div class="admin-stat-value">${active}</div></div>
    </div>
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#FEE2E2">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#EF4444" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      </div>
      <div class="admin-stat-info"><div class="admin-stat-label">Sản phẩm hết hàng</div><div class="admin-stat-value" style="${oos > 0 ? 'color:#EF4444' : ''}">${oos}</div></div>
    </div>`;
}

function renderProductTable(query) {
  const products = mockGetProducts();
  const categories = mockGetCategories();
  const catMap = Object.fromEntries(categories.map(c => [c.id, c.name]));
  const base = getBasePath();
  const q = (query || '').toLowerCase().trim();
  const filtered = products.filter(p => {
    if (q && !p.name.toLowerCase().includes(q) && !(catMap[p.categoryId] || '').toLowerCase().includes(q)) return false;
    if (productFilters.cats.length && !productFilters.cats.includes(String(p.categoryId))) return false;
    if (productFilters.genders.length && !productFilters.genders.includes(p.gender)) return false;
    if (productFilters.statuses.length) {
      const stock = getTotalStock(p);
      const isActive = p.active !== false;
      const pStatus = (stock === 0) ? 'oos' : (isActive ? 'active' : 'inactive');
      if (!productFilters.statuses.includes(pStatus)) return false;
    }
    return true;
  });

  document.getElementById('products-tbody').innerHTML = filtered.map(p => {
    const stock = getTotalStock(p);
    const isActive = p.active !== false;
    const stockColor = stock === 0 ? '#EF4444' : (isActive ? '#16A34A' : '#475569');
    const rowStyle = !isActive ? 'opacity:0.4' : '';
    return `
      <tr style="${rowStyle}">
        <td>
          <div style="display:flex;align-items:center;gap:12px">
            <img class="table-product-img" src="${base}${p.images[0]}" alt="${p.name}">
            <strong>${p.name}</strong>
          </div>
        </td>
        <td>${catMap[p.categoryId] || '-'}</td>
        <td style="font-weight:700">${formatPrice(p.basePrice)}</td>
        <td style="font-weight:700;color:${stockColor}">${stock}</td>
        <td>
          <div class="admin-actions">
            <button class="btn-edit-icon" data-pid="${p.id}" title="Chỉnh sửa">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
          </div>
        </td>
      </tr>`;
  }).join('');
}

function openProductModal(productId) {
  const products = mockGetProducts();
  const categories = mockGetCategories();
  const base = getBasePath();
  const isNew = !productId;
  const p = isNew ? {
    id: null, name: '', categoryId: categories[0]?.id || '', gender: 'Nam',
    basePrice: '', description: '', images: [], active: true, variants: []
  } : products.find(x => x.id === productId);
  if (!p) return;

  document.getElementById('edit-product-title').textContent = isNew ? 'Thêm sản phẩm mới' : 'Chỉnh sửa sản phẩm';

  const catOptions = categories.map(c =>
    `<option value="${c.id}" ${c.id === p.categoryId ? 'selected' : ''}>${c.name}</option>`
  ).join('');

  const genderOptions = ['Nam', 'Nữ', 'Unisex'].map(g =>
    `<option ${g === p.gender ? 'selected' : ''}>${g}</option>`
  ).join('');

  const imagesHtml = (p.images || []).map((img, i) =>
    `<div class="edit-image-item${i === 0 ? ' thumbnail' : ''}"><img src="${base}${img}" alt="Ảnh ${i + 1}"></div>`
  ).join('') + `<div class="edit-image-add"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg></div>`;

  const variantRows = (p.variants || []).map((v, i) =>
    `<tr>
      <td><input type="text" value="${v.color}"></td>
      <td><input type="text" value="${v.size}"></td>
      <td><input type="number" value="${v.stock}" min="0"></td>
      <td><input type="number" value="${v.price || ''}" min="0" placeholder="(Để trống lấy giá cơ bản)"></td>
      <td><button class="btn-delete-variant" title="Xóa"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#EF4444" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg></button></td>
    </tr>`
  ).join('');

  document.getElementById('edit-product-body').innerHTML = `
    <form class="edit-form" onsubmit="return false">
      <div class="form-group">
        <label>Tên sản phẩm <span style="color:#EF4444">*</span></label>
        <input type="text" class="form-input" value="${p.name}" placeholder="Nhập tên sản phẩm...">
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Danh mục <span style="color:#EF4444">*</span></label>
          <select class="admin-filter-select" style="width:100%;height:42px">${catOptions}</select>
        </div>
        <div class="form-group">
          <label>Giới tính</label>
          <select class="admin-filter-select" style="width:100%;height:42px">${genderOptions}</select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Giá bán cơ bản (VNĐ) <span style="color:#EF4444">*</span></label>
          <input type="number" class="form-input" value="${p.basePrice}" min="0" placeholder="VNĐ">
        </div>
        <div class="form-group">
          <label>Trạng thái hiển thị</label>
          <div class="toggle-field">
            <label class="toggle-switch">
              <input type="checkbox" id="modal-active-toggle" ${p.active !== false ? 'checked' : ''} onchange="document.getElementById('modal-active-label').textContent=this.checked?'Đang bật':'Đang ẩn'">
              <span class="toggle-slider"></span>
            </label>
            <span class="toggle-label" id="modal-active-label">${p.active !== false ? 'Đang bật' : 'Đang ẩn'}</span>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label>Mô tả sản phẩm</label>
        <textarea class="form-input" rows="3" style="height:auto;padding:10px 14px;resize:vertical" placeholder="Nhập mô tả sản phẩm...">${p.description}</textarea>
      </div>
      <div class="form-group">
        <label>Hình ảnh <small style="font-weight:400;color:#64748B">(Ảnh đầu tiên sẽ là Thumbnail)</small></label>
        <div class="edit-images">${imagesHtml}</div>
      </div>
      <div class="form-group">
        <label>Phân loại sản phẩm</label>
        <table class="edit-variant-table">
          <thead><tr><th>Màu sắc</th><th>Size</th><th>Tồn kho</th><th>Giá tùy chỉnh</th><th>Xóa</th></tr></thead>
          <tbody>${variantRows}</tbody>
        </table>
        <button type="button" class="btn-add-variant">+ Thêm phân loại</button>
      </div>
      <div class="edit-form-divider"></div>
      <div class="edit-form-footer">
        ${isNew ? '<div></div>' : `<button type="button" class="btn-delete-product" onclick="closeProductModal();showToast('Đã xóa sản phẩm (demo)','success')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/></svg>
          Xóa sản phẩm
        </button>`}
        <div style="display:flex;gap:10px">
          <button type="button" class="btn-cancel" onclick="closeProductModal()">Hủy</button>
          <button type="button" class="btn-save" onclick="closeProductModal();showToast('${isNew ? 'Đã thêm sản phẩm (demo)' : 'Đã lưu thay đổi (demo)'}','success')">
            ${isNew ? 'Thêm sản phẩm' : 'Lưu thay đổi'}
          </button>
        </div>
      </div>
    </form>`;

  document.getElementById('edit-product-modal').classList.add('show');
}

function closeProductModal() {
  document.getElementById('edit-product-modal').classList.remove('show');
}

function bindProductEvents() {
  // Search
  const searchInput = document.getElementById('product-search');
  if (searchInput) {
    searchInput.addEventListener('input', () => renderProductTable(searchInput.value));
  }

  // Edit buttons (delegated)
  document.getElementById('products-tbody').addEventListener('click', e => {
    const btn = e.target.closest('.btn-edit-icon');
    if (btn) openProductModal(Number(btn.dataset.pid));
  });

  // Add product button
  const addBtn = document.getElementById('btn-add-product');
  if (addBtn) addBtn.addEventListener('click', () => openProductModal(null));

  // Close modal
  document.getElementById('edit-product-close').addEventListener('click', closeProductModal);
  document.getElementById('edit-product-modal').addEventListener('click', e => {
    if (e.target === e.currentTarget) closeProductModal();
  });

  bindFilterPanel();
}

function bindFilterPanel() {
  const filterBtn = document.querySelector('.filter-chip');
  const filterPanel = document.getElementById('admin-filter-panel');
  const overlay = document.getElementById('filter-overlay');
  if (!filterPanel || !overlay) return;

  // Populate category checkboxes dynamically
  const cats = mockGetCategories();
  const catContainer = document.getElementById('admin-filter-categories');
  if (catContainer) {
    catContainer.innerHTML = cats.map(c =>
      `<label class="checkbox-wrap"><input type="checkbox" name="af-cat" value="${c.id}"><span>${c.name}</span></label>`
    ).join('');
  }

  const openPanel = () => {
    filterPanel.classList.add('show');
    overlay.classList.add('show');
  };
  const closePanel = () => {
    filterPanel.classList.remove('show');
    overlay.classList.remove('show');
  };

  if (filterBtn) filterBtn.addEventListener('click', openPanel);
  document.getElementById('admin-filter-close')?.addEventListener('click', closePanel);
  overlay.addEventListener('click', closePanel);

  // Apply button
  document.getElementById('admin-filter-apply')?.addEventListener('click', () => {
    productFilters.cats = [...filterPanel.querySelectorAll('input[name="af-cat"]:checked')].map(i => i.value);
    productFilters.genders = [...filterPanel.querySelectorAll('input[name="af-gender"]:checked')].map(i => i.value);
    productFilters.statuses = [...filterPanel.querySelectorAll('input[name="af-status"]:checked')].map(i => i.value);
    closePanel();
    renderProductTable(document.getElementById('product-search')?.value || '');
    const hasFilters = !!(productFilters.cats.length || productFilters.genders.length || productFilters.statuses.length);
    if (filterBtn) filterBtn.classList.toggle('has-filters', hasFilters);
  });

  // Reset button
  document.getElementById('admin-filter-reset')?.addEventListener('click', () => {
    filterPanel.querySelectorAll('input[type="checkbox"]').forEach(c => c.checked = false);
    productFilters = { cats: [], genders: [], statuses: [] };
    closePanel();
    renderProductTable(document.getElementById('product-search')?.value || '');
    if (filterBtn) filterBtn.classList.remove('has-filters');
  });
}
