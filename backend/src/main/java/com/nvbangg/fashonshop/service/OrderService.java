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
    private final com.nvbangg.fashonshop.repository.OrderRepository orderRepository;
    private final vn.payos.PayOS payOS;

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
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
                            INSERT INTO orders(user_id, shipping_name, shipping_phone, shipping_address, shipping_note, total_price, status)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, shippingName);
            ps.setString(3, shippingPhone);
            ps.setString(4, shippingAddress);
            ps.setString(5, request.getShippingNote());
            ps.setLong(6, finalTotalPrice);
            ps.setString(7, OrderStatus.pending.name());
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

        String checkoutUrl = null;
        try {
            List<vn.payos.model.v2.paymentRequests.PaymentLinkItem> payOSItems = new java.util.ArrayList<>();
            for (Map<String, Object> item : cartItems) {
                String name = (String) item.get("product_name");
                int quantity = ((Number) item.get("quantity")).intValue();
                int unitPrice = ((Number) item.get("unit_price")).intValue();
                payOSItems.add(vn.payos.model.v2.paymentRequests.PaymentLinkItem.builder()
                        .name(name != null && name.length() > 0 ? name : "Sản phẩm")
                        .quantity(quantity)
                        .price((long) unitPrice)
                        .build());
            }

            String desc = "Don hang " + orderId;
            if (desc.length() > 25) {
                desc = desc.substring(0, 25);
            }

            vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest paymentData = vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest.builder()
                    .orderCode(orderId)
                    .amount(finalTotalPrice)
                    .description(desc)
                    .returnUrl(buildFrontendUrl("/orders.html?payos_success=true", httpRequest))
                    .cancelUrl(buildFrontendUrl("/orders.html?payos_cancel=true&orderId=" + orderId, httpRequest))
                    .items(payOSItems)
                    .build();

            vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse paymentResponse = payOS.paymentRequests().create(paymentData);
            checkoutUrl = paymentResponse.getCheckoutUrl();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo link thanh toán: " + e.getMessage(), e);
        }

        return new CreateOrderResponse(
                orderId,
                totalPrice,
                OrderStatus.pending.name(),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                checkoutUrl
        );
    }

    @Transactional
    public String getCheckoutUrl(Long orderId, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.getCurrentUser().getId();

        Map<String, Object> orderMap;
        try {
            orderMap = jdbcTemplate.queryForMap(
                    "SELECT user_id, status, total_price FROM orders WHERE id = ?",
                    orderId
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new NotFoundException("Không tìm thấy đơn hàng");
        }

        Long orderUserId = ((Number) orderMap.get("user_id")).longValue();
        if (!orderUserId.equals(userId)) {
            throw new BadRequestException("Không có quyền truy cập đơn hàng này");
        }

        String status = (String) orderMap.get("status");
        if (!"pending".equals(status)) {
            throw new BadRequestException("Đơn hàng không ở trạng thái chờ thanh toán");
        }

        long finalTotalPrice = ((Number) orderMap.get("total_price")).longValue();

        try {
            vn.payos.model.v2.paymentRequests.PaymentLink existingLink = payOS.paymentRequests().get(orderId);
            if (existingLink.getStatus() == vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PENDING) {
                return "https://pay.payos.vn/web/" + existingLink.getId();
            }
        } catch (Exception ignored) {
        }

        return createNewPaymentLink(orderId, finalTotalPrice, httpRequest);
    }

    private String createNewPaymentLink(Long orderId, long totalPrice, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String itemsSql =
                """
                SELECT p.name AS product_name,
                       oi.quantity,
                       oi.price_at_purchase AS unit_price
                FROM order_items oi
                JOIN product_variants pv ON pv.id = oi.product_variant_id
                JOIN products p ON p.id = pv.product_id
                WHERE oi.order_id = ?
                """;
        List<Map<String, Object>> orderItems = jdbcTemplate.queryForList(itemsSql, orderId);

        List<vn.payos.model.v2.paymentRequests.PaymentLinkItem> payOSItems = new java.util.ArrayList<>();
        for (Map<String, Object> item : orderItems) {
            String name = (String) item.get("product_name");
            int quantity = ((Number) item.get("quantity")).intValue();
            int unitPrice = ((Number) item.get("unit_price")).intValue();
            payOSItems.add(vn.payos.model.v2.paymentRequests.PaymentLinkItem.builder()
                    .name(name != null && name.length() > 0 ? name : "Sản phẩm")
                    .quantity(quantity)
                    .price((long) unitPrice)
                    .build());
        }

        String desc = "Don hang " + orderId;
        if (desc.length() > 25) {
            desc = desc.substring(0, 25);
        }

        long newOrderCode = orderId * 10000 + (System.currentTimeMillis() % 10000);

        try {
            vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest paymentData = vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest.builder()
                    .orderCode(newOrderCode)
                    .amount(totalPrice)
                    .description(desc)
                    .returnUrl(buildFrontendUrl("/orders.html?payos_success=true", httpRequest))
                    .cancelUrl(buildFrontendUrl("/orders.html?payos_cancel=true&orderId=" + orderId, httpRequest))
                    .items(payOSItems)
                    .build();

            vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse paymentResponse = payOS.paymentRequests().create(paymentData);
            return paymentResponse.getCheckoutUrl();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo link thanh toán: " + e.getMessage(), e);
        }
    }

    private String buildFrontendUrl(String path, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String base = null;
        if (httpRequest != null) {
            String referer = httpRequest.getHeader("Referer");
            if (referer != null && referer.contains("/")) {
                int lastSlash = referer.lastIndexOf('/');
                if (lastSlash > 8) { 
                    base = referer.substring(0, lastSlash);
                }
            }
        }
        
        if (base == null || base.isEmpty()) {
            base = frontendUrl != null ? frontendUrl : "http://localhost:5500";
        }

        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        if (!base.endsWith("/frontend") && (base.contains("localhost") || base.contains("127.0.0.1"))) {
            base += "/frontend";
        }

        return base + path;
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Long userId = SecurityUtils.getCurrentUser().getId();

        Map<String, Object> orderMap;
        try {
            orderMap = jdbcTemplate.queryForMap(
                    "SELECT user_id, status FROM orders WHERE id = ?",
                    orderId
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new NotFoundException("Không tìm thấy đơn hàng");
        }

        Long orderUserId = ((Number) orderMap.get("user_id")).longValue();
        if (!orderUserId.equals(userId)) {
            throw new BadRequestException("Không có quyền truy cập đơn hàng này");
        }

        String status = (String) orderMap.get("status");
        if (!"pending".equals(status)) {
            return;
        }

        jdbcTemplate.update("UPDATE orders SET status = ? WHERE id = ?", OrderStatus.cancelled.name(), orderId);

        List<Map<String, Object>> orderItems = jdbcTemplate.queryForList(
                "SELECT product_variant_id, quantity FROM order_items WHERE order_id = ?",
                orderId
        );
        for (Map<String, Object> item : orderItems) {
            Long variantId = ((Number) item.get("product_variant_id")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            jdbcTemplate.update(
                    "UPDATE product_variants SET stock = stock + ? WHERE id = ?",
                    quantity, variantId
            );
        }
    }

    @Transactional(readOnly = true)
    public OrderListResponse getMyOrders(String page, String pageSize) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        OrderSearchResult result = getOrdersInternal(false, userId, null, null, page, pageSize);
        List<OrderSummaryResponse> items = result.items().stream()
                .map(item -> new OrderSummaryResponse(
                        item.id(), item.shippingName(), item.shippingPhone(), item.shippingAddress(), item.shippingNote(),
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
                        item.shippingAddress(), item.shippingNote(), item.totalPrice(), item.status(), item.createdAt(), item.updatedAt(), item.orderDetails()
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

    public StatisticsResponse getStatistics(Integer year, Integer month) {
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

        int selectedYear = (year != null) ? year : java.time.LocalDate.now().getYear();

        List<RevenueByDayResponse> revenueByDay = null;
        if (month != null) {
            revenueByDay = jdbcTemplate.query(
                    """
                            SELECT DAY(created_at) AS day,
                                   COALESCE(SUM(total_price), 0) AS revenue
                            FROM orders
                            WHERE status = ?
                              AND YEAR(created_at) = ?
                              AND MONTH(created_at) = ?
                            GROUP BY DAY(created_at)
                            ORDER BY day
                            """,
                    (rs, rowNum) -> new RevenueByDayResponse(
                            rs.getInt("day"),
                            rs.getLong("revenue")
                    ),
                    deliveredStatus,
                    selectedYear,
                    month
            );
        }

        List<DeliveredOrderResponse> deliveredOrders = jdbcTemplate.query(
                """
                        SELECT id, created_at, updated_at, total_price
                        FROM orders
                        WHERE status = ?
                          AND YEAR(created_at) = ?
                          AND (? IS NULL OR MONTH(created_at) = ?)
                        ORDER BY updated_at DESC
                        """,
                (rs, rowNum) -> new DeliveredOrderResponse(
                        rs.getLong("id"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        rs.getLong("total_price")
                ),
                deliveredStatus,
                selectedYear,
                month,
                month
        );

        return new StatisticsResponse(
                toLong(summary.get("revenue_this_month")),
                toLong(summary.get("revenue_year")),
                toLong(summary.get("revenue_all_time")),
                revenueByMonth,
                revenueByDay,
                deliveredOrders
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
                            o.shipping_note,
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
                rs.getString("shipping_note"),
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
                        baseRow.shippingNote(),
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
                                String shippingNote,
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
                                  String shippingNote,
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
