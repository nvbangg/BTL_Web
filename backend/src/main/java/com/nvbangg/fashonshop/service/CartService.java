package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.CartAddRequest;
import com.nvbangg.fashonshop.dto.request.CartUpdateRequest;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import com.nvbangg.fashonshop.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final JdbcTemplate jdbcTemplate;

    public CartService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addItem(CartAddRequest request) {
        Long userId = SecurityUtils.getCurrentUser().getId();

        Integer variantStock = findVariantStock(request.getProductVariantId());
        if (variantStock == null) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }

        List<Map<String, Object>> existingRows = jdbcTemplate.queryForList(
                "SELECT id, quantity FROM cart_items WHERE user_id = ? AND product_variant_id = ?",
                userId,
                request.getProductVariantId()
        );

        if (!existingRows.isEmpty()) {
            Map<String, Object> row = existingRows.getFirst();
            long cartId = ((Number) row.get("id")).longValue();
            int oldQuantity = ((Number) row.get("quantity")).intValue();
            int newQuantity = oldQuantity + request.getQuantity();
            validateStock(newQuantity, variantStock);

            jdbcTemplate.update("UPDATE cart_items SET quantity = ? WHERE id = ?", newQuantity, cartId);
            return;
        }

        validateStock(request.getQuantity(), variantStock);
        jdbcTemplate.update(
                "INSERT INTO cart_items(user_id, product_variant_id, quantity) VALUES (?, ?, ?)",
                userId,
                request.getProductVariantId(),
                request.getQuantity()
        );
    }

    public Map<String, Object> getMyCart() {
        Long userId = SecurityUtils.getCurrentUser().getId();

        List<Map<String, Object>> items = jdbcTemplate.query(
                """
                SELECT c.id,
                       c.product_variant_id,
                       c.quantity,
                       c.created_at,
                       pv.color,
                       pv.size,
                       pv.stock,
                       pv.price_override,
                       p.id AS product_id,
                       p.name AS product_name,
                       p.thumbnail,
                       p.price AS base_price
                FROM cart_items c
                JOIN product_variants pv ON pv.id = c.product_variant_id
                JOIN products p ON p.id = pv.product_id
                WHERE c.user_id = ?
                ORDER BY c.created_at DESC
                """,
                new Object[]{userId},
                (rs, rowNum) -> {
                    long price = rs.getObject("price_override", Long.class) == null
                            ? rs.getLong("base_price")
                            : rs.getLong("price_override");

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("productId", rs.getLong("product_id"));
                    row.put("variantId", rs.getLong("product_variant_id"));
                    row.put("productName", rs.getString("product_name"));
                    row.put("thumbnail", rs.getString("thumbnail"));
                    row.put("color", rs.getString("color"));
                    row.put("size", rs.getString("size"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("price", price);
                    row.put("stock", rs.getInt("stock"));
                    return row;
                }
        );

        long totalPrice = items.stream()
                .mapToLong(item -> ((Number) item.get("price")).longValue() * ((Number) item.get("quantity")).longValue())
                .sum();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("totalPrice", totalPrice);
        return data;
    }

    public void updateItem(Long itemId, CartUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUser().getId();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT c.id, pv.stock
                FROM cart_items c
                JOIN product_variants pv ON pv.id = c.product_variant_id
                WHERE c.id = ? AND c.user_id = ?
                """,
                itemId,
                userId
        );

        if (rows.isEmpty()) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }

        int stock = ((Number) rows.getFirst().get("stock")).intValue();
        validateStock(request.getQuantity(), stock);

        jdbcTemplate.update("UPDATE cart_items SET quantity = ? WHERE id = ?", request.getQuantity(), itemId);
    }

    public void deleteItem(Long itemId) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        int deleted = jdbcTemplate.update("DELETE FROM cart_items WHERE id = ? AND user_id = ?", itemId, userId);
        if (deleted == 0) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    private Integer findVariantStock(Long variantId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT pv.stock
                FROM product_variants pv
                JOIN products p ON p.id = pv.product_id
                WHERE pv.id = ? AND p.is_active = TRUE
                """,
                variantId
        );

        if (rows.isEmpty()) {
            return null;
        }

        return ((Number) rows.getFirst().get("stock")).intValue();
    }

    private void validateStock(int quantity, int stock) {
        if (quantity > stock) {
            throw new BadRequestException("Dữ liệu đầu vào không hợp lệ",
                    List.of(new ErrorDetail("quantity", "Số lượng yêu cầu vượt quá tồn kho hoặc không hợp lệ")));
        }
    }

}
