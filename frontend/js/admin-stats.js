let revenueChart = null;
let allRevenueByMonth = [];

document.addEventListener("DOMContentLoaded", async function () {
  document.addEventListener("app:auth-changed", function () {
    window.location.reload();
  });

  const user = await App.mountAdminPage({ activeTab: "statistics" });
  if (!user) return;

  const yearSelect = document.getElementById("chart-year");
  const monthSelect = document.getElementById("chart-month");

  if (yearSelect && monthSelect) {
    yearSelect.addEventListener("change", fetchAndRender);
    monthSelect.addEventListener("change", fetchAndRender);
  }

  await loadStatisticsInit();
});

async function loadStatisticsInit() {
  const statsRoot = document.getElementById("stats-grid");
  const yearSelect = document.getElementById("chart-year");
  const chartCanvas = document.getElementById("revenue-chart");

  if (!statsRoot || !yearSelect || !chartCanvas) return;

  try {
    const data = await AppApi.getAdminStatistics();

    statsRoot.innerHTML = '' +
      '<div class="admin-stat-card">' +
      '  <div class="admin-stat-icon" style="background:#DBEAFE;color:#1D4ED8">' + App.icon("wallet") + '</div>' +
      '  <div class="admin-stat-info">' +
      '    <div class="admin-stat-label">Doanh thu tháng này</div>' +
      '    <div class="admin-stat-value">' + App.formatPrice(data.revenueThisMonth) + '</div>' +
      '  </div>' +
      '</div>' +
      '<div class="admin-stat-card">' +
      '  <div class="admin-stat-icon" style="background:#FEF3C7;color:#B45309">' + App.icon("calendar") + '</div>' +
      '  <div class="admin-stat-info">' +
      '    <div class="admin-stat-label">Doanh thu năm nay</div>' +
      '    <div class="admin-stat-value">' + App.formatPrice(data.revenueYear) + '</div>' +
      '  </div>' +
      '</div>' +
      '<div class="admin-stat-card">' +
      '  <div class="admin-stat-icon" style="background:#FCE7F3;color:#BE185D">' + App.icon("chart") + '</div>' +
      '  <div class="admin-stat-info">' +
      '    <div class="admin-stat-label">Tổng doanh thu toàn thời gian</div>' +
      '    <div class="admin-stat-value">' + App.formatPrice(data.revenueAllTime) + '</div>' +
      '  </div>' +
      '</div>';

    allRevenueByMonth = data.revenueByMonth || [];
    const grouped = groupRevenueByYear(allRevenueByMonth);
    const years = Object.keys(grouped).sort(function (a, b) {
      return Number(b) - Number(a);
    });

    yearSelect.innerHTML = years.map(function (year) {
      return '<option value="' + year + '">Năm ' + year + "</option>";
    }).join("");

    if (!years.length) {
      const currentYear = new Date().getFullYear();
      yearSelect.innerHTML = '<option value="' + currentYear + '">Năm ' + currentYear + "</option>";
    }

    await fetchAndRender();
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Không tải được thống kê ban đầu"), "error");
  }
}

async function fetchAndRender() {
  const yearSelect = document.getElementById("chart-year");
  const monthSelect = document.getElementById("chart-month");
  const chartCanvas = document.getElementById("revenue-chart");
  const ordersListTbody = document.getElementById("delivered-orders-list");

  if (!yearSelect || !monthSelect || !chartCanvas || !ordersListTbody) return;

  const year = yearSelect.value;
  const month = monthSelect.value;

  let query = "?year=" + year;
  if (month) {
    query += "&month=" + month;
  }

  try {
    const data = await AppApi.getAdminStatistics(query);

    if (month) {
      const daysInMonth = getDaysInMonth(Number(year), Number(month));
      const labels = [];
      const values = Array(daysInMonth).fill(0);
      for (let d = 1; d <= daysInMonth; d++) {
        labels.push("N" + d);
      }
      if (data.revenueByDay) {
        data.revenueByDay.forEach(function (item) {
          const dayIndex = Number(item.day) - 1;
          if (dayIndex >= 0 && dayIndex < daysInMonth) {
            values[dayIndex] = Number(item.revenue || 0);
          }
        });
      }
      renderRevenueChart(chartCanvas, labels, values);
    } else {
      const grouped = groupRevenueByYear(data.revenueByMonth || allRevenueByMonth);
      const values = grouped[year] || Array(12).fill(0);
      const labels = ["T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"];
      renderRevenueChart(chartCanvas, labels, values);
    }

    renderDeliveredOrders(ordersListTbody, data.deliveredOrders || []);
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Không tải được dữ liệu chi tiết"), "error");
  }
}

function getDaysInMonth(year, month) {
  return new Date(year, month, 0).getDate();
}

function renderDeliveredOrders(tbody, orders) {
  if (!orders || orders.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #64748b; padding: 20px;">Không có đơn hàng đã giao nào trong khoảng thời gian này</td></tr>';
    return;
  }

  tbody.innerHTML = orders.map(function (order) {
    return '<tr>' +
      '  <td><strong style="color:#2563EB">#ORD-' + order.id + '</strong></td>' +
      '  <td>' + App.formatDate(order.createdAt) + '</td>' +
      '  <td>' + App.formatDate(order.updatedAt) + '</td>' +
      '  <td><strong style="color: #0F172A;">' + App.formatPrice(order.totalPrice) + '</strong></td>' +
      '</tr>';
  }).join("");
}

function groupRevenueByYear(rows) {
  const grouped = {};

  rows.forEach(function (item) {
    const monthValue = String(item.month || "");
    const parts = monthValue.split("-");
    if (parts.length !== 2) return;

    const year = parts[0];
    const monthIndex = Number(parts[1]) - 1;

    if (!grouped[year]) {
      grouped[year] = Array(12).fill(0);
    }

    if (monthIndex >= 0 && monthIndex < 12) {
      grouped[year][monthIndex] = Number(item.revenue || 0);
    }
  });

  return grouped;
}

function renderRevenueChart(canvas, labels, values) {
  if (!window.Chart) {
    return;
  }

  if (revenueChart) {
    revenueChart.destroy();
  }

  revenueChart = new Chart(canvas.getContext("2d"), {
    type: "bar",
    data: {
      labels: labels,
      datasets: [{
        data: values,
        borderRadius: 8,
        backgroundColor: "#2563EB"
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      layout: {
        padding: {
          left: 8,
          right: 10,
          top: 6,
          bottom: 0
        }
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: function (ctx) {
              return "Doanh thu: " + App.formatPrice(ctx.parsed.y);
            }
          }
        }
      },
      scales: {
        x: {
          grid: { display: false }
        },
        y: {
          beginAtZero: true,
          grid: {
            color: "rgba(148, 163, 184, 0.28)",
            drawBorder: false
          },
          border: {
            display: false
          },
          ticks: {
            color: "#475569",
            maxTicksLimit: 6,
            padding: 8,
            callback: function (value) {
              return formatRevenueAxisLabel(value);
            }
          }
        }
      }
    }
  });
}

function formatRevenueAxisLabel(value) {
  const amount = Number(value || 0);
  if (amount >= 1000000000) {
    const ratio = amount / 1000000000;
    return (ratio % 1 === 0 ? ratio.toFixed(0) : ratio.toFixed(1)) + " tỷ";
  }
  if (amount >= 1000000) {
    const ratio = amount / 1000000;
    return (ratio % 1 === 0 ? ratio.toFixed(0) : ratio.toFixed(1)) + " triệu";
  }
  return App.formatPrice(amount);
}
