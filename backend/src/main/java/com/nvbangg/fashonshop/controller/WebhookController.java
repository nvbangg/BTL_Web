package com.nvbangg.fashonshop.controller;

import com.nvbangg.fashonshop.entity.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class WebhookController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PayOS payOS;

    @PostMapping("/payos-webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Webhook webhookBody) {
        try {
            WebhookData data = payOS.webhooks().verify(webhookBody);

            long orderCode = data.getOrderCode();
            long orderId = resolveOrderId(orderCode);

            if ("00".equals(data.getCode())) {
                jdbcTemplate.update(
                        "UPDATE orders SET status = ? WHERE id = ? AND status = ?",
                        OrderStatus.paid.name(),
                        orderId,
                        OrderStatus.pending.name()
                );
            } else {
                String currentStatus = null;
                try {
                    currentStatus = jdbcTemplate.queryForObject(
                            "SELECT status FROM orders WHERE id = ?",
                            String.class,
                            orderId
                    );
                } catch (Exception ignored) {
                }

                if (OrderStatus.pending.name().equals(currentStatus)) {
                    jdbcTemplate.update(
                            "UPDATE orders SET status = ? WHERE id = ?",
                            OrderStatus.cancelled.name(),
                            orderId
                    );

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
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private long resolveOrderId(long orderCode) {
        if (orderCode > 10000) {
            long candidate = orderCode / 10000;
            try {
                jdbcTemplate.queryForObject("SELECT id FROM orders WHERE id = ?", Long.class, candidate);
                return candidate;
            } catch (Exception ignored) {
            }
        }
        return orderCode;
    }
}
