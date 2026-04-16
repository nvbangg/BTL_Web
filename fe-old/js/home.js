/* ===== HOME PAGE (VIEW ONLY) ===== */
let HOME_PAGE = 1;

document.addEventListener('DOMContentLoaded', async () => {
  await initPage();

  const categories = mockGetCategories();
  const catContainer = document.getElementById('filter-categories');
  categories.forEach((c) => {
    catContainer.innerHTML += `<label class="checkbox-wrap"><input type="checkbox" name="category" value="${c.id}"><span>${c.name}</span></label>`;
  });

  document.querySelectorAll('.sort-pill').forEach((pill) => {
    pill.addEventListener('click', () => {
      document.querySelectorAll('.sort-pill').forEach((p) => p.classList.remove('active'));
      pill.classList.add('active');
      HOME_PAGE = 1;
      loadProducts();
    });
  });

  const filterPanel = document.getElementById('filter-panel');
  const filterOverlay = document.getElementById('filter-overlay');
  document.getElementById('btn-filter').addEventListener('click', () => {
    filterPanel.classList.add('show');
    filterOverlay.classList.add('show');
  });
  const closeFilter = () => {
    filterPanel.classList.remove('show');
    filterOverlay.classList.remove('show');
  };
  document.getElementById('filter-close').addEventListener('click', closeFilter);
  filterOverlay.addEventListener('click', closeFilter);

  document.getElementById('filter-apply').addEventListener('click', () => {
    HOME_PAGE = 1;
    closeFilter();
    loadProducts();
  });

  document.getElementById('filter-reset').addEventListener('click', () => {
    document.querySelectorAll('#filter-panel input[type="checkbox"]').forEach((c) => c.checked = false);
    HOME_PAGE = 1;
    closeFilter();
    loadProducts();
  });

  loadProducts();
});

function loadProducts() {
  const grid = document.getElementById('product-grid');
  const empty = document.getElementById('empty-state');
  const paginationArea = document.getElementById('pagination-area');

  const keyword = (getParam('keyword') || '').trim().toLowerCase();
  const allProducts = mockGetProducts();

  if (keyword) {
    const filtered = allProducts.filter((p) => String(p.name || '').toLowerCase().includes(keyword));
    const pageSize = 12;
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
    HOME_PAGE = Math.min(Math.max(1, HOME_PAGE), totalPages);
    const start = (HOME_PAGE - 1) * pageSize;
    renderResult(filtered.slice(start, start + pageSize), HOME_PAGE, totalPages);
    return;
  }

  const totalPages = mockGetTotalPages();
  HOME_PAGE = Math.min(Math.max(1, HOME_PAGE), totalPages);
  const products = mockGetProductsByPage(HOME_PAGE);
  renderResult(products, HOME_PAGE, totalPages);

  function renderResult(products, page, pages) {
    if (!products.length) {
      grid.innerHTML = '';
      empty.style.display = 'flex';
      paginationArea.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    grid.innerHTML = products.map((p) => renderProductCard(p)).join('');
    paginationArea.innerHTML = renderPagination(page, pages);
    bindPagination(paginationArea, (nextPage) => {
      HOME_PAGE = nextPage;
      loadProducts();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }
}
