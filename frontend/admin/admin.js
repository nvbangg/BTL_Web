/* ===== ADMIN STATISTICS PAGE ===== */
document.addEventListener('DOMContentLoaded', async () => {
  await initAdminPage('statistics');
  loadStatistics();
});

async function loadStatistics() {
  const stats = getMockData()?.statistics || {
    revenueThisMonth: 0,
    revenueThisYear: 0,
    revenueAllTime: 0,
    monthlyRevenue: { [new Date().getFullYear()]: Array(12).fill(0) }
  };
  document.getElementById('stats-grid').innerHTML = `
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#DBEAFE">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
      </div>
      <div class="admin-stat-info">
        <div class="admin-stat-label">Doanh thu tháng này</div>
        <div class="admin-stat-value">${formatPrice(stats.revenueThisMonth)}</div>
      </div>
    </div>
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#FEF3C7">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#D97706" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
      </div>
      <div class="admin-stat-info">
        <div class="admin-stat-label">Doanh thu năm nay</div>
        <div class="admin-stat-value">${formatPrice(stats.revenueThisYear)}</div>
      </div>
    </div>
    <div class="admin-stat-card">
      <div class="admin-stat-icon" style="background:#FCE7F3">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#DB2777" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/><circle cx="18" cy="15" r="1"/></svg>
      </div>
      <div class="admin-stat-info">
        <div class="admin-stat-label">Tổng doanh thu toàn thời gian</div>
        <div class="admin-stat-value">${formatPrice(stats.revenueAllTime)}</div>
      </div>
    </div>
  `;

  const years = Object.keys(stats.monthlyRevenue).sort((a, b) => b - a);
  const yearSelect = document.getElementById('chart-year');
  yearSelect.innerHTML = years.map(y => `<option value="${y}">Năm ${y}</option>`).join('');

  const renderChart = (year) => {
    const data = stats.monthlyRevenue[year];
    const max = Math.max(...data, 1);
    const months = ['T1','T2','T3','T4','T5','T6','T7','T8','T9','T10','T11','T12'];
    const currentMonth = new Date().getMonth();
    const isCurrentYear = String(year) === String(new Date().getFullYear());

    document.getElementById('chart-bars').innerHTML = data.map((val, i) => {
      const h = Math.round(val / max * 180);
      const isActive = isCurrentYear && i === currentMonth;
      const isFuture = isCurrentYear && i > currentMonth;
      const labelClass = isActive ? 'chart-bar-label active' : (isFuture ? 'chart-bar-label future' : 'chart-bar-label');
      const fillClass = isActive ? 'chart-bar-fill active' : 'chart-bar-fill';
      return `
        <div class="chart-bar">
          <div class="chart-tooltip">${(val / 1000000).toFixed(1)}M</div>
          <div class="${fillClass}" style="height:${h}px"></div>
          <div class="${labelClass}">${months[i]}</div>
        </div>`;
    }).join('');
  };

  renderChart(years[0]);
  yearSelect.addEventListener('change', (e) => renderChart(e.target.value));
}
