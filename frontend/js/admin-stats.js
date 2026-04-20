let revenueChart = null;

document.addEventListener("DOMContentLoaded", async function () {
  document.addEventListener("app:auth-changed", function () {
    window.location.reload();
  });

  const user = await App.mountAdminPage({ activeTab: "statistics" });
  if (!user) return;

  await loadStatistics();
});

async function loadStatistics() {
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

    const grouped = groupRevenueByYear(data.revenueByMonth || []);
    const years = Object.keys(grouped).sort(function (a, b) {
      return Number(b) - Number(a);
    });

    yearSelect.innerHTML = years.map(function (year) {
      return '<option value="' + year + '">Năm ' + year + "</option>";
    }).join("");

    if (!years.length) {
      yearSelect.innerHTML = '<option value="">Không có dữ liệu</option>';
      return;
    }

    const render = function () {
      const selectedYear = yearSelect.value;
      const values = grouped[selectedYear] || Array(12).fill(0);
      renderRevenueChart(chartCanvas, values);
    };

    yearSelect.addEventListener("change", render);
    render();
  } catch (error) {
    App.showToast(App.getApiErrorMessage(error, "Không tải được thống kê"), "error");
  }
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

function renderRevenueChart(canvas, values) {
  if (!window.Chart) {
    return;
  }

  if (revenueChart) {
    revenueChart.destroy();
  }

  revenueChart = new Chart(canvas.getContext("2d"), {
    type: "bar",
    data: {
      labels: ["T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"],
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
