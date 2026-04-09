package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.AdminUpdateOrderStatusRequest;
import com.nvbangg.fashonshop.dto.request.CreateOrderRequest;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import com.nvbangg.fashonshop.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    private static final Set<String> VALID_ORDER_STATUS = Set.of("pending", "processing", "shipped", "delivered", "cancelled");

    private final JdbcTemplate jdbcTemplate;

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> createOrder(CreateOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        List<Long> cartItemIds = normalizeCartItemIds(request.getCartItemIds());

        String shippingName = requireShippingValue(request.getShippingName(), "shippingName", "Họ và tên người nhận không được để trống");
        String shippingPhone = requireShippingValue(request.getShippingPhone(), "shippingPhone", "Số điện thoại không được để trống");
        String shippingAddress = requireShippingValue(request.getShippingAddress(), "shippingAddress", "Địa chỉ nhận hàng không được để trống");

        String placeholders = buildPlaceholders(cartItemIds.size());
        String cartSql =
                """
                SELECT c.id AS cart_id,
                       c.quantity,
                       pv.id AS product_variant_id,
                       pv.stock,
                       COALESCE(pv.price_override, p.price) AS unit_price
                FROM cart_items c
                JOIN product_variants pv ON pv.id = c.product_variant_id
                JOIN products p ON p.id = pv.product_id
                WHERE c.user_id = ? AND c.id IN (%s)
                """.formatted(placeholders);

        List<Object> queryParams = new ArrayList<>();
        queryParams.add(userId);
        queryParams.addAll(cartItemIds);

        List<Map<String, Object>> cartItems = jdbcTemplate.queryForList(cartSql, queryParams.toArray());

        if (cartItems.size() != cartItemIds.size()) {
            throw new BadRequestException("Tạo đơn hàng thất bại",
                    List.of(new ErrorDetail("cartItemIds", "Một số phân loại sản phẩm không hợp lệ hoặc đã bị xóa")));
        }

        long totalPrice = 0L;
        for (Map<String, Object> item : cartItems) {
            int quantity = ((Number) item.get("quantity")).intValue();
            int stock = ((Number) item.get("stock")).intValue();
            if (quantity > stock) {
                throw new BadRequestException("Tạo đơn hàng thất bại",
                        List.of(new ErrorDetail("cartItemIds", "Một số sản phẩm trong giỏ đã hết hàng")));
            }

            long unitPrice = ((Number) item.get("unit_price")).longValue();
            totalPrice += unitPrice * quantity;
        }

        final long finalTotalPrice = totalPrice;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO orders(user_id, shipping_name, shipping_phone, shipping_address, total_price, status)
                    VALUES (?, ?, ?, ?, ?, 'pending')
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, shippingName);
            ps.setString(3, shippingPhone);
            ps.setString(4, shippingAddress);
            ps.setLong(5, finalTotalPrice);
            return ps;
        }, keyHolder);

        Long orderId = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        if (orderId == null) {
            throw new IllegalStateException("Không thể tạo đơn hàng");
        }

        for (Map<String, Object> item : cartItems) {
            Long variantId = ((Number) item.get("product_variant_id")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            long unitPrice = ((Number) item.get("unit_price")).longValue();

            jdbcTemplate.update(
                    "INSERT INTO order_items(order_id, product_variant_id, quantity, price_at_purchase) VALUES (?, ?, ?, ?)",
                    orderId,
                    variantId,
                    quantity,
                    unitPrice
            );

            int updated = jdbcTemplate.update(
                    "UPDATE product_variants SET stock = stock - ? WHERE id = ? AND stock >= ?",
                    quantity,
                    variantId,
                    quantity
            );
            if (updated == 0) {
                throw new BadRequestException("Tạo đơn hàng thất bại",
                        List.of(new ErrorDetail("cartItemIds", "Một số sản phẩm trong giỏ đã hết hàng")));
            }
        }

        List<Object> deleteParams = new ArrayList<>();
        deleteParams.add(userId);
        deleteParams.addAll(cartItemIds);
        jdbcTemplate.update("DELETE FROM cart_items WHERE user_id = ? AND id IN (" + placeholders + ")", deleteParams.toArray());

        Timestamp createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM orders WHERE id = ?",
                Timestamp.class,
                orderId
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", orderId);
        data.put("totalPrice", totalPrice);
        data.put("status", "pending");
        data.put("createdAt", createdAt);
        return data;
    }

    public Map<String, Object> getMyOrders(String page, String pageSize) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        return getOrdersInternal(false, userId, null, null, page, pageSize);
    }

    public Map<String, Object> getAdminOrders(String keyword, String status, String page, String pageSize) {
        validateOrderStatusIfPresent(status);
        return getOrdersInternal(true, null, keyword, status, page, pageSize);
    }

    public void updateOrderStatus(Long id, AdminUpdateOrderStatusRequest request) {
        String status = request.getStatus().trim().toLowerCase(Locale.ROOT);
        if (!VALID_ORDER_STATUS.contains(status)) {
            throw new BadRequestException("Cập nhật thất bại",
                    List.of(new ErrorDetail("status", "Trạng thái cập nhật không hợp lệ")));
        }

        int updated = jdbcTemplate.update("UPDATE orders SET status = ? WHERE id = ?", status, id);
        if (updated == 0) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    public Map<String, Object> getStatistics() {
        Long revenueThisMonth = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status = 'delivered'
                  AND YEAR(created_at) = YEAR(CURDATE())
                  AND MONTH(created_at) = MONTH(CURDATE())
                """,
                Long.class
        );

        Long revenueYear = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(total_price), 0)
                FROM orders
                WHERE status = 'delivered'
                  AND YEAR(created_at) = YEAR(CURDATE())
                """,
                Long.class
        );

        Long revenueAllTime = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_price), 0) FROM orders WHERE status = 'delivered'",
                Long.class
        );

        List<Map<String, Object>> revenueByMonth = jdbcTemplate.query(
                """
                SELECT DATE_FORMAT(created_at, '%Y-%m') AS month,
                       COALESCE(SUM(total_price), 0) AS revenue
                FROM orders
                WHERE status = 'delivered'
                GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                ORDER BY month
                """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", rs.getString("month"));
                    row.put("revenue", rs.getLong("revenue"));
                    return row;
                }
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("revenueThisMonth", revenueThisMonth == null ? 0L : revenueThisMonth);
        data.put("revenueYear", revenueYear == null ? 0L : revenueYear);
        data.put("revenueAllTime", revenueAllTime == null ? 0L : revenueAllTime);
        data.put("revenueByMonth", revenueByMonth);
        return data;
    }

    private Map<String, Object> getOrdersInternal(boolean admin,
                                                  Long userId,
                                                  String keyword,
                                                  String status,
                                                  String page,
                                                  String pageSize) {
        int pageValue = parsePositiveOrDefault(page, 1);
        int pageSizeValue = parsePositiveOrDefault(pageSize, 10);
        int offset = (pageValue - 1) * pageSizeValue;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!admin) {
            where.append(" AND o.user_id = ? ");
            params.add(userId);
        }

        String normalizedKeyword = normalizeNullable(keyword);
        if (normalizedKeyword != null) {
            where.append(" AND (CAST(o.id AS CHAR) LIKE ? OR LOWER(o.shipping_name) LIKE ? OR LOWER(u.email) LIKE ?) ");
            String pattern = "%" + normalizedKeyword + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        String normalizedStatus = normalizeNullable(status);
        if (normalizedStatus != null) {
            where.append(" AND o.status = ? ");
            params.add(normalizedStatus);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders o JOIN users u ON u.id = o.user_id " + where,
                params.toArray(),
                Long.class
        );

        String sql =
                """
                SELECT o.id,
                       o.user_id,
                       u.email,
                       o.shipping_name,
                       o.shipping_phone,
                       o.shipping_address,
                       o.total_price,
                       o.status,
                       o.created_at,
                       o.updated_at
                FROM orders o
                JOIN users u ON u.id = o.user_id
                """ + where + " ORDER BY o.updated_at DESC LIMIT ? OFFSET ?";

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(pageSizeValue);
        queryParams.add(offset);

        List<Map<String, Object>> items = jdbcTemplate.query(sql, queryParams.toArray(), (rs, rowNum) -> {
            Long orderId = rs.getLong("id");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", orderId);
            if (admin) {
                row.put("userId", rs.getLong("user_id"));
                row.put("email", rs.getString("email"));
            }
            row.put("shippingName", rs.getString("shipping_name"));
            row.put("shippingPhone", rs.getString("shipping_phone"));
            row.put("shippingAddress", rs.getString("shipping_address"));
            row.put("totalPrice", rs.getLong("total_price"));
            row.put("status", rs.getString("status"));
            row.put("createdAt", rs.getTimestamp("created_at"));
            row.put("updatedAt", rs.getTimestamp("updated_at"));
            row.put("orderDetails", getOrderItems(orderId));
            return row;
        });

        Map<String, Object> data = new LinkedHashMap<>();
        if (admin) {
            Long totalPendingOrders = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM orders WHERE status = 'pending'",
                    Long.class
            );
            Long totalIncompleteOrders = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM orders WHERE status IN ('pending', 'processing', 'shipped')",
                    Long.class
            );
            data.put("totalPendingOrders", totalPendingOrders == null ? 0L : totalPendingOrders);
            data.put("totalIncompleteOrders", totalIncompleteOrders == null ? 0L : totalIncompleteOrders);
        }
        data.put("items", items);
        data.put("page", pageValue);
        data.put("pageSize", pageSizeValue);
        data.put("total", total == null ? 0L : total);
        return data;
    }

    private List<Map<String, Object>> getOrderItems(Long orderId) {
        return jdbcTemplate.query(
                """
                SELECT oi.id,
                       oi.product_variant_id,
                       oi.quantity,
                       oi.price_at_purchase,
                       pv.color,
                       pv.size,
                       p.id AS product_id,
                       p.name AS product_name,
                       p.thumbnail
                FROM order_items oi
                JOIN product_variants pv ON pv.id = oi.product_variant_id
                JOIN products p ON p.id = pv.product_id
                WHERE oi.order_id = ?
                ORDER BY oi.id
                """,
                new Object[]{orderId},
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("productId", rs.getLong("product_id"));
                    row.put("variantId", rs.getLong("product_variant_id"));
                    row.put("productName", rs.getString("product_name"));
                    row.put("thumbnail", rs.getString("thumbnail"));
                    row.put("color", rs.getString("color"));
                    row.put("size", rs.getString("size"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("price", rs.getLong("price_at_purchase"));
                    return row;
                }
        );
    }

    private List<Long> normalizeCartItemIds(List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BadRequestException("Tạo đơn hàng thất bại",
                    List.of(new ErrorDetail("cartItemIds", "Danh sách sản phẩm không được để trống")));
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long itemId : cartItemIds) {
            if (itemId == null || itemId <= 0) {
                throw new BadRequestException("Tạo đơn hàng thất bại",
                        List.of(new ErrorDetail("cartItemIds", "Danh sách sản phẩm không được để trống")));
            }
            uniqueIds.add(itemId);
        }

        return new ArrayList<>(uniqueIds);
    }

    private String requireShippingValue(String value, String fieldName, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Tạo đơn hàng thất bại",
                    List.of(new ErrorDetail(fieldName, message)));
        }
        return value.trim();
    }

    private String buildPlaceholders(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("?");
        }
        return builder.toString();
    }

    private void validateOrderStatusIfPresent(String status) {
        String normalized = normalizeNullable(status);
        if (normalized == null) {
            return;
        }

        if (!VALID_ORDER_STATUS.contains(normalized)) {
            throw new BadRequestException("Lỗi truy vấn danh sách",
                    List.of(new ErrorDetail("status", "Trạng thái đơn hàng không hợp lệ")));
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
