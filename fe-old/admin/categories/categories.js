/* ===== ADMIN CATEGORIES PAGE ===== */
document.addEventListener('DOMContentLoaded', async () => {
  await initAdminPage('categories');
  renderCategoryStats();
  renderCategoryTable();
  bindCategoryEvents();
});

function openAddCatModal() {
  const modal = document.getElementById('add-cat-modal');
  const input = document.getElementById('add-cat-name');
  if (!modal) return;
  if (input) input.value = '';
  modal.style.display = 'flex';
  setTimeout(() => { if (input) input.focus(); }, 60);
}

function closeAddCatModal() {
  const modal = document.getElementById('add-cat-modal');
  if (modal) modal.style.display = 'none';
}

function renderCategoryStats() {
  const categories = mockGetCategories();
  const products = mockGetProducts();
  const empty = categories.filter(c => !products.some(p => p.categoryId === c.id)).length;

  document.getElementById('category-stats').innerHTML = `
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#DBEAFE">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="2"><path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2z"/></svg>
      </div>
      <div class="admin-stat-info"><div class="admin-stat-value">${categories.length}</div><div class="admin-stat-label">Số danh mục</div></div>
    </div>
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#FEF3C7">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#D97706" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      </div>
      <div class="admin-stat-info"><div class="admin-stat-value">${empty}</div><div class="admin-stat-label">Danh mục trống</div></div>
    </div>`;
}

let editingCatId = null;

function renderCategoryTable(query) {
  const categories = mockGetCategories();
  const products = mockGetProducts();
  const q = (query || '').toLowerCase().trim();
  const filtered = q ? categories.filter(c => c.name.toLowerCase().includes(q)) : categories;

  document.getElementById('categories-tbody').innerHTML = filtered.map((c, idx) => {
    const count = products.filter(p => p.categoryId === c.id).length;
    const isEditing = editingCatId === c.id;
    return `
      <tr>
        <td>${idx + 1}</td>
        <td>
          ${isEditing
            ? `<input class="cat-edit-input" id="cat-input-${c.id}" value="${c.name}" autofocus>`
            : c.name}
        </td>
        <td>${count}</td>
        <td>
          <div class="admin-actions">
            ${isEditing
              ? `<button class="btn-save-cat" data-cid="${c.id}" title="Lưu">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#16A34A" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                </button>`
              : `<button class="btn-edit-icon" data-cid="${c.id}" title="Chỉnh sửa">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="2" stroke-linecap="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>`}
          </div>
        </td>
      </tr>`;
  }).join('');
}

function bindCategoryEvents() {
  const searchInput = document.getElementById('cat-search-input');
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      editingCatId = null;
      renderCategoryTable(searchInput.value);
    });
  }

  // Delegated click for edit / save
  document.getElementById('categories-tbody').addEventListener('click', e => {
    const editBtn = e.target.closest('.btn-edit-icon');
    if (editBtn) {
      editingCatId = Number(editBtn.dataset.cid);
      renderCategoryTable(searchInput?.value);
      return;
    }
    const saveBtn = e.target.closest('.btn-save-cat');
    if (saveBtn) {
      editingCatId = null;
      renderCategoryTable(searchInput?.value);
      showToast('Đã cập nhật danh mục (demo)', 'success');
    }
  });

  // Enter key to save when editing
  document.getElementById('categories-tbody').addEventListener('keydown', e => {
    if (e.key === 'Enter' && editingCatId) {
      editingCatId = null;
      renderCategoryTable(searchInput?.value);
      showToast('Đã cập nhật danh mục', 'success');
    }
  });

  // Add category button — open modal
  const addBtn = document.getElementById('btn-add-cat');
  if (addBtn) addBtn.addEventListener('click', openAddCatModal);

  const closeBtn = document.getElementById('add-cat-close');
  if (closeBtn) closeBtn.addEventListener('click', closeAddCatModal);

  const cancelBtn = document.getElementById('add-cat-cancel');
  if (cancelBtn) cancelBtn.addEventListener('click', closeAddCatModal);

  const catModal = document.getElementById('add-cat-modal');
  if (catModal) catModal.addEventListener('click', e => {
    if (e.target === catModal) closeAddCatModal();
  });

  const confirmBtn = document.getElementById('add-cat-confirm');
  if (confirmBtn) confirmBtn.addEventListener('click', () => {
    const input = document.getElementById('add-cat-name');
    const name = (input?.value || '').trim();
    if (!name) { if (input) input.focus(); return; }
    closeAddCatModal();
    showToast(`Đã thêm danh mục “${name}”`, 'success');
  });

  const nameInput = document.getElementById('add-cat-name');
  if (nameInput) nameInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('add-cat-confirm')?.click();
  });
}
