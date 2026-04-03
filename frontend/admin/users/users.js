/* ===== ADMIN USERS PAGE ===== */
let allUsers = [];

document.addEventListener('DOMContentLoaded', async () => {
  await initAdminPage('users');
  loadUsers();
  bindUserFilters();
});

async function loadUsers() {
  try {
    const response = await apiAdminGetUsers({ page: 1, pageSize: 100 });
    allUsers = response.items || [];
    renderUsers(allUsers);
  } catch (error) {
    console.error('Failed to load users from API, trying mock data:', error);
    // Fallback to mock data directly
    if (typeof mockGetUsers === 'function') {
      allUsers = mockGetUsers();
      renderUsers(allUsers);
    } else {
      allUsers = [];
      renderUsers([]);
    }
  }
}

function renderUsers(users) {
  const tbody = document.getElementById('users-tbody');

  tbody.innerHTML = users.map((user) => {
    const statusColor = user.role === 'admin' ? '#2563EB' : '#475569';
    const canDelete = user.role !== 'admin';
    return `
      <tr>
        <td><strong>${user.email}</strong></td>
        <td>${user.name || '—'}</td>
        <td><strong style="color:${statusColor}">${user.role === 'admin' ? 'Admin' : 'Người dùng'}</strong></td>
        <td>
          <div class="admin-actions">
            ${user.role === 'user' ? `
              <select class="status-badge-select" data-user-id="${user.id}" style="appearance:none; background-color:#E0E7FF; color:#4F46E5">
                <option value="user" selected>Người dùng</option>
                <option value="admin">Promote to Admin</option>
              </select>
            ` : `
              <span class="status-badge admin" style="margin:0">Admin</span>
            `}
            ${canDelete ? `
              <button class="btn-delete-user" data-user-id="${user.id}" title="Xóa">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#EF4444" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                </svg>
              </button>
            ` : ''}
          </div>
        </td>
      </tr>`;
  }).join('');

  // Bind role change
  tbody.querySelectorAll('[data-user-id]').forEach(select => {
    select.addEventListener('change', async (e) => {
      const userId = Number(select.dataset.userId);
      const newRole = select.value;
      try {
        await apiAdminUpdateUserRole(userId, newRole);
        showToast('Cập nhật vai trò thành công', 'success');
        loadUsers();
      } catch (error) {
        showToast('Lỗi khi cập nhật vai trò', 'error');
        loadUsers();
      }
    });
  });

  // Bind delete button
  tbody.querySelectorAll('.btn-delete-user').forEach(btn => {
    btn.addEventListener('click', async () => {
      const userId = Number(btn.dataset.userId);
      if (!confirm('Bạn chắc chắn muốn xóa người dùng này?')) return;
      try {
        await apiAdminDeleteUser(userId);
        showToast('Đã xóa người dùng', 'success');
        loadUsers();
      } catch (error) {
        showToast('Lỗi khi xóa người dùng', 'error');
        loadUsers();
      }
    });
  });
}

function getFilteredUsers() {
  const search = (document.getElementById('user-search')?.value || '').trim().toLowerCase();
  const roleFilter = document.getElementById('user-role-filter')?.value || '';
  return allUsers.filter(u => {
    if (roleFilter && u.role !== roleFilter) return false;
    if (search && !u.email.toLowerCase().includes(search)) return false;
    return true;
  });
}

function bindUserFilters() {
  document.getElementById('user-search')?.addEventListener('input', () => {
    renderUsers(getFilteredUsers());
  });
  document.getElementById('user-role-filter')?.addEventListener('change', () => {
    renderUsers(getFilteredUsers());
  });
}
