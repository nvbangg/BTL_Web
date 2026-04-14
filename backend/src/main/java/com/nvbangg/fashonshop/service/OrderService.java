package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.AdminUpdateOrderStatusRequest;
import com.nvbangg.fashonshop.dto.request.CreateOrderRequest;
import com.nvbangg.fashonshop.dto.response.AdminOrderListResponse;
import com.nvbangg.fashonshop.dto.response.AdminOrderSummaryResponse;
import com.nvbangg.fashonshop.dto.response.CreateOrderResponse;
import com.nvbangg.fashonshop.dto.response.OrderItemResponse;
import com.nvbangg.fashonshop.dto.response.OrderListResponse;
import com.nvbangg.fashonshop.dto.response.OrderSummaryResponse;
import com.nvbangg.fashonshop.dto.response.RevenueByMonthResponse;
import com.nvbangg.fashonshop.dto.response.StatisticsResponse;
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
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        List<Long> cartItemIds = normalizeCartItemIds(request.getCartItemIds());

        String shippingName = requireShippingValue(request.getShippingName(), "shippingName", "Họ và tên người nhận là bắt buộc");
        String shippingPhone = requireShippingValue(request.getShippingPhone(), "shippingPhone", "Số điện thoại là bắt buộc");
        String shippingAddress = requireShippingValue(request.getShippingAddress(), "shippingAddress", "Địa chỉ nhận hàng là bắt buộc");

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

        return new CreateOrderResponse(
                orderId,
                totalPrice,
                "pending",
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    public OrderListResponse getMyOrders(String page, String pageSize) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        OrderSearchResult result = getOrdersInternal(false, userId, null, null, page, pageSize);
        List<OrderSummaryResponse> items = result.items.stream()
            .map(item -> new OrderSummaryResponse(
                item.id,
                item.shippingName,
                item.shippingPhone,
                item.shippingAddress,
                item.totalPrice,
                item.status,
                item.createdAt,
                item.updatedAt,
                item.orderDetails
            ))
            .toList();

        return new OrderListResponse(
            items,
            result.page,
            result.pageSize,
            result.total
        );
    }

    public AdminOrderListResponse getAdminOrders(String keyword, String status, String page, String pageSize) {
        validateOrderStatusIfPresent(status);
        OrderSearchResult result = getOrdersInternal(true, null, keyword, status, page, pageSize);
        List<AdminOrderSummaryResponse> items = result.items.stream()
            .map(item -> new AdminOrderSummaryResponse(
                item.id,
                item.userId,
                item.email,
                item.shippingName,
                item.shippingPhone,
                item.shippingAddress,
                item.totalPrice,
                item.status,
                item.createdAt,
                item.updatedAt,
                item.orderDetails
            ))
            .toList();

        return new AdminOrderListResponse(
            result.totalPendingOrders,
            result.totalIncompleteOrders,
            items,
            result.page,
            result.pageSize,
            result.total
        );
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

    public StatisticsResponse getStatistics() {
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

        List<RevenueByMonthResponse> revenueByMonth = jdbcTemplate.query(
                """
                SELECT DATE_FORMAT(created_at, '%Y-%m') AS month,
                       COALESCE(SUM(total_price), 0) AS revenue
                FROM orders
                WHERE status = 'delivered'
                GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                ORDER BY month
                """,
                (rs, rowNum) -> new RevenueByMonthResponse(
                        rs.getString("month"),
                        rs.getLong("revenue")
                )
        );

        return new StatisticsResponse(
                revenueThisMonth == null ? 0L : revenueThisMonth,
                revenueYear == null ? 0L : revenueYear,
                revenueAllTime == null ? 0L : revenueAllTime,
                revenueByMonth
        );
    }

    private OrderSearchResult getOrdersInternal(boolean admin,
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
            Long.class,
            params.toArray()
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

        List<OrderSearchRow> items = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long orderId = rs.getLong("id");

            return new OrderSearchRow(
                    orderId,
                    rs.getLong("user_id"),
                    rs.getString("email"),
                    rs.getString("shipping_name"),
                    rs.getString("shipping_phone"),
                    rs.getString("shipping_address"),
                    rs.getLong("total_price"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime(),
                    getOrderItems(orderId)
            );
        }, queryParams.toArray());

        Long totalPendingOrders = null;
        Long totalIncompleteOrders = null;
        if (admin) {
            totalPendingOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = 'pending'",
                Long.class
            );
            totalIncompleteOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status IN ('pending', 'processing', 'shipped')",
                Long.class
            );
        }

        return new OrderSearchResult(
                items,
                pageValue,
                pageSizeValue,
                total == null ? 0L : total,
                totalPendingOrders,
                totalIncompleteOrders
        );
    }

    private static class OrderSearchResult {
        private final List<OrderSearchRow> items;
        private final Integer page;
        private final Integer pageSize;
        private final Long total;
        private final Long totalPendingOrders;
        private final Long totalIncompleteOrders;

        private OrderSearchResult(List<OrderSearchRow> items,
                                  Integer page,
                                  Integer pageSize,
                                  Long total,
                                  Long totalPendingOrders,
                                  Long totalIncompleteOrders) {
            this.items = items;
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalPendingOrders = totalPendingOrders;
            this.totalIncompleteOrders = totalIncompleteOrders;
        }
    }

    private static class OrderSearchRow {
        private final Long id;
        private final Long userId;
        private final String email;
        private final String shippingName;
        private final String shippingPhone;
        private final String shippingAddress;
        private final Long totalPrice;
        private final String status;
        private final java.time.LocalDateTime createdAt;
        private final java.time.LocalDateTime updatedAt;
        private final List<OrderItemResponse> orderDetails;

        private OrderSearchRow(Long id,
                               Long userId,
                               String email,
                               String shippingName,
                               String shippingPhone,
                               String shippingAddress,
                               Long totalPrice,
                               String status,
                               java.time.LocalDateTime createdAt,
                               java.time.LocalDateTime updatedAt,
                               List<OrderItemResponse> orderDetails) {
            this.id = id;
            this.userId = userId;
            this.email = email;
            this.shippingName = shippingName;
            this.shippingPhone = shippingPhone;
            this.shippingAddress = shippingAddress;
            this.totalPrice = totalPrice;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.orderDetails = orderDetails;
        }
    }

    private List<OrderItemResponse> getOrderItems(Long orderId) {
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
                (rs, rowNum) -> new OrderItemResponse(
                        rs.getLong("id"),
                        rs.getLong("product_id"),
                        rs.getLong("product_variant_id"),
                        rs.getString("product_name"),
                        rs.getString("thumbnail"),
                        rs.getString("color"),
                        rs.getString("size"),
                        rs.getInt("quantity"),
                        rs.getLong("price_at_purchase")
                    ),
                    orderId
        );
    }

    private List<Long> normalizeCartItemIds(List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BadRequestException("Tạo đơn hàng thất bại",
                    List.of(new ErrorDetail("cartItemIds", "Danh sách sản phẩm là bắt buộc")));
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long itemId : cartItemIds) {
            if (itemId == null || itemId <= 0) {
                throw new BadRequestException("Tạo đơn hàng thất bại",
                        List.of(new ErrorDetail("cartItemIds", "Danh sách sản phẩm là bắt buộc")));
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
