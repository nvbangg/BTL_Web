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
            
            if ("00".equals(data.getCode())) {
                String orderIdStr = String.valueOf(data.getOrderCode());
                jdbcTemplate.update(
                        "UPDATE orders SET status = ? WHERE id = ?",
                        OrderStatus.paid.name(),
                        orderIdStr
                );
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
