let userState = {
  keyword: "",
  role: "",
  page: 1,
  pageSize: 10,
  data: null
};

document.addEventListener("DOMContentLoaded", async function () {
  document.addEventListener("app:auth-changed", function () {
    window.location.reload();
  });

  const user = await App.mountAdminPage({ activeTab: "users" });
  if (!user) return;

  initUserState();
  bindUserFilters();
  await loadUsers();
});

function initUserState() {
  const params = App.getQueryParams();
  userState.keyword = params.get("keyword") || "";
  userState.role = params.get("role") || "";
  userState.page = Number(params.get("page") || 1);
  userState.pageSize = Number(params.get("pageSize") || 10);

  const searchInput = document.getElementById("user-search");
  const roleSelect = document.getElementById("user-role-filter");

  if (searchInput) searchInput.value = userState.keyword;
  if (roleSelect) roleSelect.value = userState.role;
}

function bindUserFilters() {
  const searchInput = document.getElementById("user-search");
  const roleSelect = document.getElementById("user-role-filter");

  let timer = null;

  if (searchInput) {
    searchInput.addEventListener("input", function () {
      clearTimeout(timer);
      timer = setTimeout(function () {
        App.updateQuery({ keyword: searchInput.value.trim(), page: 1 });
      }, 350);
    });
  }

  if (roleSelect) {
    roleSelect.addEventListener("change", function () {
      App.updateQuery({ role: roleSelect.value, page: 1 });
    });
  }
}

function buildUsersQuery() {
  const params = new URLSearchParams();
  if (userState.keyword) params.set("keyword", userState.keyword);
  if (userState.role) params.set("role", userState.role);
  params.set("page", String(userState.page));
  params.set("pageSize", String(userState.pageSize));
  return "?" + params.toString();
}

async function loadUsers() {
  const body = document.getElementById("users-tbody");
  const paging = document.getElementById("users-pagination");

  if (!body || !paging) return;

  body.innerHTML = '<tr><td colspan="5"><div class="loading-center"><div class="spinner"></div></div></td></tr>';

  try {
    const data = await AppApi.getAdminUsers(buildUsersQuery());
    userState.data = data;
    renderStats(data);

    if (!(data.items || []).length) {
      body.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:28px;color:#64748B">Không có người dùng</td></tr>';
      paging.innerHTML = "";
      return;
    }

    body.innerHTML = (data.items || []).map(function (item, index) {
      const stt = (data.page - 1) * data.pageSize + index + 1;
      return '' +
        '<tr>' +
        '  <td>' + stt + '</td>' +
        '  <td>' +
        '    <div style="font-weight:700">' + App.escapeHtml(item.name || "") + '</div>' +
        '    <div style="font-size:13px;color:#64748B">' + App.escapeHtml(item.email || "") + '</div>' +
        '  </td>' +
        '  <td>' +
        '    <div style="font-weight:700">' + App.escapeHtml(item.phone || "") + '</div>' +
        '    <div style="font-size:13px;color:#64748B">' + App.escapeHtml(item.address || "") + '</div>' +
        '  </td>' +
        '  <td>' + App.formatDate(item.createdAt) + '</td>' +
        '  <td>' +
        '    <select class="status-badge-select ' + (item.role === "admin" ? "pending" : "delivered") + '" data-user-id="' + item.id + '">' +
        '      <option value="user" ' + (item.role === "user" ? "selected" : "") + '>User</option>' +
        '      <option value="admin" ' + (item.role === "admin" ? "selected" : "") + '>Admin</option>' +
        '    </select>' +
        '  </td>' +
        '</tr>';
    }).join("");

    Array.from(body.querySelectorAll("select[data-user-id]")).forEach(function (select) {
      select.addEventListener("change", async function () {
        const userId = Number(select.getAttribute("data-user-id"));
        try {
          await AppApi.updateAdminUserRole(userId, select.value);
          App.showToast("Đã cập nhật vai trò", "success");
          await loadUsers();
        } catch (error) {
          App.showToast(App.getApiErrorMessage(error, "Cập nhật vai trò thất bại"), "error");
          await loadUsers();
        }
      });
    });

    App.renderPagination(paging, data.page, data.pageSize, data.total, function (nextPage) {
      App.updateQuery({ page: nextPage, pageSize: data.pageSize });
    });
  } catch (error) {
    body.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:28px;color:#EF4444">' + App.escapeHtml(App.getApiErrorMessage(error, "Không tải được danh sách người dùng")) + "</td></tr>";
    paging.innerHTML = "";
  }
}

function renderStats(data) {
  const root = document.getElementById("user-stats");
  if (!root) return;

  root.innerHTML = '' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#F1F5F9;color:#475569">' + App.icon("users") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Tổng số người dùng</div><div class="admin-stat-value">' + Number(data.total || 0).toLocaleString("vi-VN") + '</div></div></div>' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#DCFCE7;color:#15803D">' + App.icon("checkCircle") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Người dùng đã mua hàng</div><div class="admin-stat-value">' + Number(data.totalPurchasedUsers || 0).toLocaleString("vi-VN") + '</div></div></div>' +
    '<div class="admin-stat-card"><div class="admin-stat-icon" style="background:#FEF3C7;color:#B45309">' + App.icon("shield") + '</div><div class="admin-stat-info"><div class="admin-stat-label">Quản trị viên (Admin)</div><div class="admin-stat-value">' + Number(data.totalAdmins || 0).toLocaleString("vi-VN") + '</div></div></div>';
}
