package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.common.util.QueryUtils;
import com.nvbangg.fashonshop.dto.request.AdminUpdateUserRoleRequest;
import com.nvbangg.fashonshop.dto.response.AdminUserItemResponse;
import com.nvbangg.fashonshop.dto.response.AdminUserListResponse;
import com.nvbangg.fashonshop.entity.OrderStatus;
import com.nvbangg.fashonshop.entity.UserRole;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import com.nvbangg.fashonshop.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminUserService {

    private static final Set<UserRole> VALID_ROLES = EnumSet.allOf(UserRole.class);

    private final JdbcTemplate jdbcTemplate;

    public AdminUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminUserListResponse getUsers(String keyword, String role, String page, String pageSize) {
        UserRole normalizedRole = parseNullableRole(role, "Dữ liệu không hợp lệ", "Bộ lọc vai trò (role) không hợp lệ");

        int pageValue = QueryUtils.parsePositiveOrDefault(page, 1);
        int pageSizeValue = QueryUtils.parsePositiveOrDefault(pageSize, 10);
        int offset = (pageValue - 1) * pageSizeValue;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String normalizedKeyword = QueryUtils.normalizeNullable(keyword);
        if (normalizedKeyword != null) {
            where.append(" AND (LOWER(u.email) LIKE ? OR LOWER(IFNULL(u.name, '')) LIKE ?) ");
            String pattern = "%" + normalizedKeyword + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (normalizedRole != null) {
            where.append(" AND u.role = ? ");
            params.add(normalizedRole.name());
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users u " + where, Long.class, params.toArray());

        String sql =
                "SELECT u.id, u.email, u.name, u.phone, u.address, u.role, u.created_at FROM users u "
                        + where
                        + " ORDER BY u.created_at DESC LIMIT ? OFFSET ?";

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(pageSizeValue);
        queryParams.add(offset);

        List<AdminUserItemResponse> items = jdbcTemplate.query(sql, (rs, rowNum) -> new AdminUserItemResponse(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("role"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), queryParams.toArray());

        Long totalPurchasedUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM orders WHERE status = ?",
                Long.class,
                OrderStatus.delivered.name()
        );
        Long totalAdmins = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = ?",
                Long.class,
                UserRole.admin.name()
        );

        return new AdminUserListResponse(
                totalPurchasedUsers == null ? 0L : totalPurchasedUsers,
                totalAdmins == null ? 0L : totalAdmins,
                items,
                pageValue,
                pageSizeValue,
                total == null ? 0L : total
        );
    }

    public void updateRole(Long id, AdminUpdateUserRoleRequest request) {
        UserRole role = parseRole(request.getRole(), "Dữ liệu không hợp lệ", "Vai trò không hợp lệ hoặc không thể thay đổi vai trò của bản thân");

        Long currentUserId = SecurityUtils.getCurrentUser().getId();
        if (currentUserId.equals(id)) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("role", "Vai trò không hợp lệ hoặc không thể thay đổi vai trò của bản thân")));
        }

        int updated = jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role.name(), id);
        if (updated == 0) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    private UserRole parseNullableRole(String value, String message, String errorMessage) {
        String normalized = QueryUtils.normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        try {
            UserRole role = UserRole.valueOf(normalized);
            if (!VALID_ROLES.contains(role)) {
                throw new IllegalArgumentException("Unsupported role");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(message,
                    List.of(new ErrorDetail("role", errorMessage)));
        }
    }

    private UserRole parseRole(String value, String message, String errorMessage) {
        if (value == null) {
            throw new BadRequestException(message,
                    List.of(new ErrorDetail("role", errorMessage)));
        }

        try {
            UserRole role = UserRole.valueOf(value.trim().toLowerCase(Locale.ROOT));
            if (!VALID_ROLES.contains(role)) {
                throw new IllegalArgumentException("Unsupported role");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(message,
                    List.of(new ErrorDetail("role", errorMessage)));
        }
    }

}
