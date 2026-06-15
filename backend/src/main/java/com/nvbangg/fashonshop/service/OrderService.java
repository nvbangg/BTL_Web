package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.common.util.QueryUtils;
import com.nvbangg.fashonshop.dto.request.AdminUpdateOrderStatusRequest;
import com.nvbangg.fashonshop.dto.request.CreateOrderRequest;
import com.nvbangg.fashonshop.dto.response.*;
import com.nvbangg.fashonshop.entity.OrderStatus;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import com.nvbangg.fashonshop.repository.OrderRepository;
import com.nvbangg.fashonshop.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final JdbcTemplate jdbcTemplate;
    private final OrderRepository orderRepository;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        List<Long> cartItemIds = normalizeCartItemIds(request.getCartItemIds());

        String shippingName = requireShippingValue(request.getShippingName(), "shippingName", "Họ và tên người nhận là bắt buộc");
        String shippingPhone = requireShippingValue(request.getShippingPhone(), "shippingPhone", "Số điện thoại là bắt buộc");
        String shippingAddress = requireShippingValue(request.getShippingAddress(), "shippingAddress", "Địa chỉ nhận hàng là bắt buộc");

        String placeholders = QueryUtils.buildPlaceholders(cartItemIds.size());
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
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("cartItemIds", "Một số sản phẩm không hợp lệ hoặc vượt quá số lượng tồn kho")));
        }

        long totalPrice = 0L;
        for (Map<String, Object> item : cartItems) {
            int quantity = ((Number) item.get("quantity")).intValue();
            int stock = ((Number) item.get("stock")).intValue();
            if (quantity > stock) {
                throw new BadRequestException("Dữ liệu không hợp lệ",
                        List.of(new ErrorDetail("cartItemIds", "Một số sản phẩm không hợp lệ hoặc vượt quá số lượng tồn kho")));
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
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, shippingName);
            ps.setString(3, shippingPhone);
            ps.setString(4, shippingAddress);
            ps.setLong(5, finalTotalPrice);
            ps.setString(6, OrderStatus.pending.name());
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
                throw new BadRequestException("Dữ liệu không hợp lệ",
                        List.of(new ErrorDetail("cartItemIds", "Một số sản phẩm không hợp lệ hoặc vượt quá số lượng tồn kho")));
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
                OrderStatus.pending.name(),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    @Transactional(readOnly = true)
    public OrderListResponse getMyOrders(String page, String pageSize) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        OrderSearchResult result = getOrdersInternal(false, userId, null, null, page, pageSize);
        List<OrderSummaryResponse> items = result.items().stream()
                .map(item -> new OrderSummaryResponse(
                        item.id(), item.shippingName(), item.shippingPhone(), item.shippingAddress(),
                        item.totalPrice(), item.status(), item.createdAt(), item.updatedAt(), item.orderDetails()))
                .toList();

        return new OrderListResponse(items, result.page(), result.pageSize(), result.total());
    }

    @Transactional(readOnly = true)
    public AdminOrderListResponse getAdminOrders(String keyword, String status, String page, String pageSize) {
        OrderSearchResult result = getOrdersInternal(true, null, keyword, status, page, pageSize);
        List<AdminOrderSummaryResponse> items = result.items().stream()
                .map(item -> new AdminOrderSummaryResponse(
                        item.id(), item.userId(), item.email(), item.shippingName(), item.shippingPhone(),
                        item.shippingAddress(), item.totalPrice(), item.status(), item.createdAt(), item.updatedAt(), item.orderDetails()
                ))
                .toList();

        return new AdminOrderListResponse(
                result.totalPendingOrders(), result.totalIncompleteOrders(), items, result.page(), result.pageSize(), result.total());
    }

    public void updateOrderStatus(Long id, AdminUpdateOrderStatusRequest request) {
        OrderStatus status = parseOrderStatus(request.getStatus(), "Dữ liệu không hợp lệ", "Trạng thái đơn hàng không hợp lệ", false);

        int updated = jdbcTemplate.update("UPDATE orders SET status = ? WHERE id = ?", status.name(), id);
        if (updated == 0) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    public StatisticsResponse getStatistics() {
        String deliveredStatus = OrderStatus.delivered.name();

        Map<String, Object> summary = jdbcTemplate.queryForMap(
                """
                        SELECT COALESCE(SUM(CASE
                               WHEN status = ?
                                AND YEAR(created_at) = YEAR(CURDATE())
                                AND MONTH(created_at) = MONTH(CURDATE())
                               THEN total_price ELSE 0 END), 0) AS revenue_this_month,
                               COALESCE(SUM(CASE
                               WHEN status = ?
                                AND YEAR(created_at) = YEAR(CURDATE())
                               THEN total_price ELSE 0 END), 0) AS revenue_year,
                               COALESCE(SUM(CASE
                               WHEN status = ?
                               THEN total_price ELSE 0 END), 0) AS revenue_all_time
                        FROM orders
                        """,
                deliveredStatus,
                deliveredStatus,
                deliveredStatus
        );

        List<RevenueByMonthResponse> revenueByMonth = jdbcTemplate.query(
                """
                        SELECT DATE_FORMAT(created_at, '%Y-%m') AS month,
                               COALESCE(SUM(total_price), 0) AS revenue
                        FROM orders
                        WHERE status = ?
                        GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                        ORDER BY month
                        """,
                (rs, rowNum) -> new RevenueByMonthResponse(
                        rs.getString("month"),
                        rs.getLong("revenue")
                ),
                deliveredStatus
        );

        return new StatisticsResponse(
                toLong(summary.get("revenue_this_month")),
                toLong(summary.get("revenue_year")),
                toLong(summary.get("revenue_all_time")),
                revenueByMonth
        );
    }

    private OrderSearchResult getOrdersInternal(boolean admin,
                                                Long userId,
                                                String keyword,
                                                String status,
                                                String page,
                                                String pageSize) {
        int pageValue = QueryUtils.parsePositiveOrDefault(page, 1);
        int pageSizeValue = QueryUtils.parsePositiveOrDefault(pageSize, 10);
        int offset = (pageValue - 1) * pageSizeValue;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!admin) {
            where.append(" AND o.user_id = ? ");
            params.add(userId);
        }

        String normalizedKeyword = QueryUtils.normalizeNullable(keyword);
        if (normalizedKeyword != null) {
            where.append(" AND (CAST(o.id AS CHAR) LIKE ? OR LOWER(o.shipping_name) LIKE ? OR LOWER(u.email) LIKE ?) ");
            String pattern = "%" + normalizedKeyword + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        OrderStatus normalizedStatus = parseOrderStatusFilter(status);
        if (normalizedStatus != null) {
            where.append(" AND o.status = ? ");
            params.add(normalizedStatus.name());
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

        List<OrderBaseRow> baseRows = jdbcTemplate.query(sql, (rs, rowNum) -> new OrderBaseRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("email"),
                rs.getString("shipping_name"),
                rs.getString("shipping_phone"),
                rs.getString("shipping_address"),
                rs.getLong("total_price"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ), queryParams.toArray());

        Map<Long, List<OrderItemResponse>> orderItemsByOrderId = loadOrderItemsByOrderIds(
                baseRows.stream().map(OrderBaseRow::id).toList()
        );

        List<OrderSearchRow> items = baseRows.stream()
                .map(baseRow -> new OrderSearchRow(
                        baseRow.id(),
                        baseRow.userId(),
                        baseRow.email(),
                        baseRow.shippingName(),
                        baseRow.shippingPhone(),
                        baseRow.shippingAddress(),
                        baseRow.totalPrice(),
                        baseRow.status(),
                        baseRow.createdAt(),
                        baseRow.updatedAt(),
                        orderItemsByOrderId.getOrDefault(baseRow.id(), Collections.emptyList())
                ))
                .toList();

        Long totalPendingOrders = null;
        Long totalIncompleteOrders = null;
        if (admin) {
            long pendingOrders = orderRepository.countByStatus(OrderStatus.pending);
            long paidOrders = orderRepository.countByStatus(OrderStatus.paid);
            long shippedOrders = orderRepository.countByStatus(OrderStatus.shipped);
            totalPendingOrders = pendingOrders;
            totalIncompleteOrders = pendingOrders + paidOrders + shippedOrders;
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

    private record OrderSearchResult(List<OrderSearchRow> items,
                                     Integer page,
                                     Integer pageSize,
                                     Long total,
                                     Long totalPendingOrders,
                                     Long totalIncompleteOrders) {
    }

    private record OrderBaseRow(Long id,
                                Long userId,
                                String email,
                                String shippingName,
                                String shippingPhone,
                                String shippingAddress,
                                Long totalPrice,
                                String status,
                                java.time.LocalDateTime createdAt,
                                java.time.LocalDateTime updatedAt) {
    }

    private record OrderSearchRow(Long id,
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
    }

    private Map<Long, List<OrderItemResponse>> loadOrderItemsByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = QueryUtils.buildPlaceholders(orderIds.size());
        String sql =
                """
                        SELECT oi.order_id,
                               oi.id,
                               p.id AS product_id,
                               pv.id AS variant_id,
                               p.name AS product_name,
                               p.thumbnail,
                               pv.color,
                               pv.size,
                               oi.quantity,
                               oi.price_at_purchase
                        FROM order_items oi
                        JOIN product_variants pv ON pv.id = oi.product_variant_id
                        JOIN products p ON p.id = pv.product_id
                        WHERE oi.order_id IN (%s)
                        ORDER BY oi.order_id ASC, oi.id ASC
                        """.formatted(placeholders);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, orderIds.toArray());
        Map<Long, List<OrderItemResponse>> orderItemsByOrderId = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Long orderId = ((Number) row.get("order_id")).longValue();
            OrderItemResponse item = new OrderItemResponse(
                    ((Number) row.get("id")).longValue(),
                    ((Number) row.get("product_id")).longValue(),
                    ((Number) row.get("variant_id")).longValue(),
                    (String) row.get("product_name"),
                    (String) row.get("thumbnail"),
                    (String) row.get("color"),
                    (String) row.get("size"),
                    ((Number) row.get("quantity")).intValue(),
                    ((Number) row.get("price_at_purchase")).longValue()
            );

            orderItemsByOrderId.computeIfAbsent(orderId, key -> new ArrayList<>()).add(item);
        }

        return orderItemsByOrderId;
    }

    private List<Long> normalizeCartItemIds(List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw cartItemIdsRequiredError();
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long itemId : cartItemIds) {
            if (itemId == null || itemId <= 0) {
                throw cartItemIdsRequiredError();
            }
            uniqueIds.add(itemId);
        }

        return new ArrayList<>(uniqueIds);
    }

    private String requireShippingValue(String value, String fieldName, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail(fieldName, message)));
        }
        return value.trim();
    }

    private OrderStatus parseOrderStatus(String value, String message, String errorMessage, boolean nullable) {
        String normalized = QueryUtils.normalizeNullable(value);
        if (normalized == null) {
            if (nullable) {
                return null;
            }
            throw statusBadRequest(message, errorMessage);
        }

        try {
            return OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw statusBadRequest(message, errorMessage);
        }
    }

    private OrderStatus parseOrderStatusFilter(String value) {
        String normalized = QueryUtils.normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        try {
            return OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BadRequestException statusBadRequest(String message, String errorMessage) {
        return new BadRequestException(message,
                List.of(new ErrorDetail("status", errorMessage)));
    }

    private BadRequestException cartItemIdsRequiredError() {
        return new BadRequestException("Dữ liệu không hợp lệ",
                List.of(new ErrorDetail("cartItemIds", "Danh sách sản phẩm là bắt buộc")));
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
