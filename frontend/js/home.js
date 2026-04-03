/* ===== HOME PAGE (API CALLS) ===== */
let HOME_PAGE = 1;

document.addEventListener('DOMContentLoaded', async () => {
  await initPage();

  try {
    const filters = await apiGetProductFilters();
    const catContainer = document.getElementById('filter-categories');
    if (filters.categories) {
      filters.categories.forEach((c) => {
        catContainer.innerHTML += `<label class="checkbox-wrap"><input type="checkbox" name="category" value="${c.id}"><span>${c.name}</span></label>`;
      });
    }
  } catch (error) {
    console.error('Failed to load categories', error);
  }

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

async function loadProducts() {
  const grid = document.getElementById('product-grid');
  const empty = document.getElementById('empty-state');
  const paginationArea = document.getElementById('pagination-area');

  const keyword = (getParam('keyword') || '').trim();
  const sortBtn = document.querySelector('.sort-pill.active');
  const sortMap = { popular: 'best_selling', 'best-seller': 'best_selling', newest: 'newest', 'price-asc': 'price_asc' };
  const sort = sortMap[sortBtn?.dataset.sort] || 'best_selling';

  const params = {
    keyword: keyword || undefined,
    sort,
    page: HOME_PAGE,
    pageSize: 16
  };

  try {
    const response = await apiGetProducts(params);
    const products = response.items || [];
    const page = response.page || HOME_PAGE;
    const total = response.total || 0;
    const pageSize = response.pageSize || 16;
    const totalPages = Math.ceil(total / pageSize) || 1;

    if (!products.length) {
      grid.innerHTML = '';
      empty.style.display = 'flex';
      paginationArea.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    grid.innerHTML = products.map((p) => renderProductCard(p)).join('');
    paginationArea.innerHTML = renderPagination(page, totalPages);
    bindPagination(paginationArea, (nextPage) => {
      HOME_PAGE = nextPage;
      loadProducts();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  } catch (error) {
    console.error('Failed to load products', error);
    grid.innerHTML = '';
    empty.style.display = 'flex';
    paginationArea.innerHTML = '';
  }
}
