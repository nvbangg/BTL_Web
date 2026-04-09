package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.AdminUpdateUserRoleRequest;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import com.nvbangg.fashonshop.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> VALID_ROLES = Set.of("admin", "user");

    private final JdbcTemplate jdbcTemplate;

    public AdminUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getUsers(String keyword, String role, String page, String pageSize) {
        String normalizedRole = normalizeNullable(role);
        if (normalizedRole != null && !VALID_ROLES.contains(normalizedRole)) {
            throw new BadRequestException("Dữ liệu truy vấn không hợp lệ",
                    List.of(new ErrorDetail("role", "Bộ lọc vai trò (role) không hợp lệ")));
        }

        int pageValue = parsePositiveOrDefault(page, 1);
        int pageSizeValue = parsePositiveOrDefault(pageSize, 10);
        int offset = (pageValue - 1) * pageSizeValue;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String normalizedKeyword = normalizeNullable(keyword);
        if (normalizedKeyword != null) {
            where.append(" AND (LOWER(u.email) LIKE ? OR LOWER(IFNULL(u.name, '')) LIKE ?) ");
            String pattern = "%" + normalizedKeyword + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (normalizedRole != null) {
            where.append(" AND u.role = ? ");
            params.add(normalizedRole);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users u " + where, params.toArray(), Long.class);

        String sql =
                "SELECT u.id, u.email, u.name, u.phone, u.address, u.role, u.created_at FROM users u "
                        + where
                        + " ORDER BY u.created_at DESC LIMIT ? OFFSET ?";

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(pageSizeValue);
        queryParams.add(offset);

        List<Map<String, Object>> items = jdbcTemplate.query(sql, queryParams.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("email", rs.getString("email"));
            row.put("name", rs.getString("name"));
            row.put("phone", rs.getString("phone"));
            row.put("address", rs.getString("address"));
            row.put("role", rs.getString("role"));
            row.put("createdAt", rs.getTimestamp("created_at"));
            return row;
        });

        Long totalPurchasedUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM orders WHERE status = 'delivered'",
                Long.class
        );
        Long totalAdmins = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'admin'",
                Long.class
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalPurchasedUsers", totalPurchasedUsers == null ? 0L : totalPurchasedUsers);
        data.put("totalAdmins", totalAdmins == null ? 0L : totalAdmins);
        data.put("items", items);
        data.put("page", pageValue);
        data.put("pageSize", pageSizeValue);
        data.put("total", total == null ? 0L : total);
        return data;
    }

    public void updateRole(Long id, AdminUpdateUserRoleRequest request) {
        String role = request.getRole().trim().toLowerCase(Locale.ROOT);
        if (!VALID_ROLES.contains(role)) {
            throw new BadRequestException("Cập nhật thất bại",
                    List.of(new ErrorDetail("role", "Vai trò không hợp lệ hoặc không thể thay đổi vai trò của bản thân")));
        }

        Long currentUserId = SecurityUtils.getCurrentUser().getId();
        if (currentUserId.equals(id)) {
            throw new BadRequestException("Cập nhật thất bại",
                    List.of(new ErrorDetail("role", "Vai trò không hợp lệ hoặc không thể thay đổi vai trò của bản thân")));
        }

        int updated = jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role, id);
        if (updated == 0) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    private int parsePositiveOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
