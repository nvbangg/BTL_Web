package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.ProductImageRequest;
import com.nvbangg.fashonshop.dto.request.ProductUpsertRequest;
import com.nvbangg.fashonshop.dto.request.ProductVariantRequest;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProductService {

    private static final Set<String> VALID_GENDERS = Set.of("male", "female", "unisex");

    private final JdbcTemplate jdbcTemplate;

    public ProductService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getPublicProducts(String keyword,
                                                 String category,
                                                 String gender,
                                                 String color,
                                                 String size,
                                                 Long minPrice,
                                                 Long maxPrice,
                                                 String sort,
                                                 String page,
                                                 String pageSize) {
        return getProductsInternal(false, keyword, category, gender, color, size, minPrice, maxPrice, sort, null, page, pageSize);
    }

    public Map<String, Object> getAdminProducts(String keyword,
                                                String category,
                                                String gender,
                                                String color,
                                                String size,
                                                Long minPrice,
                                                Long maxPrice,
                                                String sort,
                                                String isActive,
                                                String page,
                                                String pageSize) {
        return getProductsInternal(true, keyword, category, gender, color, size, minPrice, maxPrice, sort, isActive, page, pageSize);
    }

    public Map<String, Object> getFilters() {
        List<String> categories = jdbcTemplate.queryForList(
            "SELECT DISTINCT category FROM products WHERE is_active = TRUE ORDER BY category ASC",
                String.class
        );

        List<String> colors = jdbcTemplate.queryForList(
            "SELECT DISTINCT pv.color FROM product_variants pv JOIN products p ON p.id = pv.product_id WHERE p.is_active = TRUE ORDER BY pv.color ASC",
                String.class
        );

        List<String> sizes = jdbcTemplate.queryForList(
            "SELECT DISTINCT pv.size FROM product_variants pv JOIN products p ON p.id = pv.product_id WHERE p.is_active = TRUE ORDER BY pv.size ASC",
                String.class
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("categories", categories);
        data.put("genders", List.of("male", "female", "unisex"));
        data.put("colors", colors);
        data.put("sizes", sizes);
        return data;
    }

    public Map<String, Object> getPublicProductDetail(Long id) {
        return getProductDetail(id, false);
    }

    public Map<String, Object> getAdminProductDetail(Long id) {
        return getProductDetail(id, true);
    }

    @Transactional
    public Map<String, Object> createProduct(ProductUpsertRequest request) {
        validateUpsertRequest(request);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO products(name, description, thumbnail, category, gender, price, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, request.getName().trim());
            ps.setString(2, nullableTrim(request.getDescription()));
            ps.setString(3, request.getThumbnail().trim());
            ps.setString(4, request.getCategory().trim());
            ps.setString(5, request.getGender().trim().toLowerCase(Locale.ROOT));
            ps.setLong(6, request.getPrice());
            ps.setBoolean(7, request.getIsActive() == null || request.getIsActive());
            return ps;
        }, keyHolder);

        Long productId = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        if (productId == null) {
            throw new IllegalStateException("Không thể tạo sản phẩm");
        }

        insertImages(productId, request.getImages());
        insertVariants(productId, request.getVariants());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", productId);
        return data;
    }

    @Transactional
    public void updateProduct(Long id, ProductUpsertRequest request) {
        ensureProductExists(id);
        validateUpsertRequest(request);

        jdbcTemplate.update(
                """
                UPDATE products
                SET name = ?, description = ?, thumbnail = ?, category = ?, gender = ?, price = ?, is_active = ?
                WHERE id = ?
                """,
                request.getName().trim(),
                nullableTrim(request.getDescription()),
                request.getThumbnail().trim(),
                request.getCategory().trim(),
                request.getGender().trim().toLowerCase(Locale.ROOT),
                request.getPrice(),
                request.getIsActive() == null || request.getIsActive(),
                id
        );

        jdbcTemplate.update("DELETE FROM product_images WHERE product_id = ?", id);
        jdbcTemplate.update("DELETE FROM product_variants WHERE product_id = ?", id);

        insertImages(id, request.getImages());
        insertVariants(id, request.getVariants());
    }

    private Map<String, Object> getProductsInternal(boolean admin,
                                                    String keyword,
                                                    String category,
                                                    String gender,
                                                    String color,
                                                    String size,
                                                    Long minPrice,
                                                    Long maxPrice,
                                                    String sort,
                                                    String isActive,
                                                    String page,
                                                    String pageSize) {
        validatePriceRange(admin, minPrice, maxPrice);

        String normalizedGender = normalizeNullable(gender);
        if (normalizedGender != null && !VALID_GENDERS.contains(normalizedGender)) {
            throw new BadRequestException("Dữ liệu truy vấn không hợp lệ",
                List.of(new ErrorDetail("gender", "Giới tính (gender) không hợp lệ (chỉ hỗ trợ: male, female, unisex)")));
        }

        Boolean activeFilter = null;
        if (admin) {
            activeFilter = parseNullableBoolean(isActive);
        }

        int defaultPageSize = admin ? 10 : 16;
        int pageValue = parsePositiveOrDefault(page, 1);
        int pageSizeValue = parsePositiveOrDefault(pageSize, defaultPageSize);
        int offset = (pageValue - 1) * pageSizeValue;

        String orderBy = resolveSort(sort, admin);

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!admin) {
            where.append(" AND p.is_active = TRUE ");
        } else if (activeFilter != null) {
            where.append(" AND p.is_active = ? ");
            params.add(activeFilter);
        }

        String normalizedKeyword = normalizeNullable(keyword);
        if (normalizedKeyword != null) {
            where.append(" AND (LOWER(p.name) LIKE ? OR LOWER(IFNULL(p.description, '')) LIKE ?) ");
            params.add("%" + normalizedKeyword + "%");
            params.add("%" + normalizedKeyword + "%");
        }

        String normalizedCategory = normalizeNullable(category);
        if (normalizedCategory != null) {
            where.append(" AND LOWER(p.category) = ? ");
            params.add(normalizedCategory);
        }

        if (normalizedGender != null) {
            where.append(" AND p.gender = ? ");
            params.add(normalizedGender);
        }

        if (minPrice != null) {
            where.append(" AND p.price >= ? ");
            params.add(minPrice);
        }

        if (maxPrice != null) {
            where.append(" AND p.price <= ? ");
            params.add(maxPrice);
        }

        String normalizedColor = normalizeNullable(color);
        if (normalizedColor != null) {
            where.append(" AND EXISTS (SELECT 1 FROM product_variants pv WHERE pv.product_id = p.id AND LOWER(pv.color) = ?) ");
            params.add(normalizedColor);
        }

        String normalizedSize = normalizeNullable(size);
        if (normalizedSize != null) {
            where.append(" AND EXISTS (SELECT 1 FROM product_variants pv WHERE pv.product_id = p.id AND LOWER(pv.size) = ?) ");
            params.add(normalizedSize);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products p " + where,
                params.toArray(),
                Long.class
        );

        String listSql =
                """
                SELECT p.id,
                       p.name,
                       p.thumbnail,
                       p.price,
                       p.is_active,
                       p.created_at,
                       COALESCE((
                           SELECT SUM(oi.quantity)
                           FROM order_items oi
                           JOIN product_variants pv2 ON pv2.id = oi.product_variant_id
                           JOIN orders o ON o.id = oi.order_id
                           WHERE pv2.product_id = p.id AND o.status = 'delivered'
                       ), 0) AS sold_count,
                       COALESCE((
                           SELECT SUM(pv3.stock)
                           FROM product_variants pv3
                           WHERE pv3.product_id = p.id
                       ), 0) AS total_stock
                FROM products p
                """ + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";

        List<Object> listParams = new ArrayList<>(params);
        listParams.add(pageSizeValue);
        listParams.add(offset);

        List<Map<String, Object>> items = jdbcTemplate.query(listSql, listParams.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            if (admin) {
                row.put("id", rs.getLong("id"));
                row.put("thumbnail", rs.getString("thumbnail"));
                row.put("name", rs.getString("name"));
                row.put("price", rs.getLong("price"));
                row.put("soldCount", rs.getLong("sold_count"));
                row.put("totalStock", rs.getLong("total_stock"));
                row.put("isActive", rs.getBoolean("is_active"));
            } else {
                row.put("id", rs.getLong("id"));
                row.put("name", rs.getString("name"));
                row.put("thumbnail", rs.getString("thumbnail"));
                row.put("price", rs.getLong("price"));
            }
            return row;
        });

        Map<String, Object> data = new LinkedHashMap<>();
        if (admin) {
            Long totalActiveProducts = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM products WHERE is_active = TRUE",
                    Long.class
            );
            Long totalOutOfStockProducts = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM products p
                    WHERE COALESCE((
                        SELECT SUM(pv.stock)
                        FROM product_variants pv
                        WHERE pv.product_id = p.id
                    ), 0) = 0
                    """,
                    Long.class
            );

            data.put("totalActiveProducts", totalActiveProducts == null ? 0L : totalActiveProducts);
            data.put("totalOutOfStockProducts", totalOutOfStockProducts == null ? 0L : totalOutOfStockProducts);
        }
        data.put("items", items);
        data.put("page", pageValue);
        data.put("pageSize", pageSizeValue);
        data.put("total", total == null ? 0L : total);
        return data;
    }

    private Map<String, Object> getProductDetail(Long id, boolean admin) {
        String sql =
                """
                SELECT p.id,
                       p.name,
                       p.description,
                       p.thumbnail,
                       p.category,
                       p.gender,
                       p.price,
                       p.is_active,
                       COALESCE((
                           SELECT SUM(oi.quantity)
                           FROM order_items oi
                           JOIN product_variants pv2 ON pv2.id = oi.product_variant_id
                           JOIN orders o ON o.id = oi.order_id
                           WHERE pv2.product_id = p.id AND o.status = 'delivered'
                       ), 0) AS sold_count
                FROM products p
                WHERE p.id = ?
                """ + (admin ? "" : " AND p.is_active = TRUE ");

        List<Map<String, Object>> products = jdbcTemplate.query(sql, new Object[]{id}, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("name", rs.getString("name"));
            row.put("description", rs.getString("description"));
            row.put("thumbnail", rs.getString("thumbnail"));
            row.put("category", rs.getString("category"));
            row.put("gender", rs.getString("gender"));
            row.put("price", rs.getLong("price"));
            if (admin) {
                row.put("isActive", rs.getBoolean("is_active"));
            } else {
                row.put("soldCount", rs.getLong("sold_count"));
            }
            return row;
        });

        if (products.isEmpty()) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }

        Map<String, Object> product = products.getFirst();

        List<Map<String, Object>> images = jdbcTemplate.query(
                "SELECT id, image, sort_order FROM product_images WHERE product_id = ? ORDER BY sort_order, id",
                new Object[]{id},
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("image", rs.getString("image"));
                    row.put("sortOrder", rs.getInt("sort_order"));
                    return row;
                }
        );

        List<Map<String, Object>> variants = jdbcTemplate.query(
                "SELECT id, color, size, stock, price_override FROM product_variants WHERE product_id = ? ORDER BY color, size",
                new Object[]{id},
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("color", rs.getString("color"));
                    row.put("size", rs.getString("size"));
                    row.put("stock", rs.getInt("stock"));
                    Long priceOverride = rs.getObject("price_override", Long.class);
                    row.put("priceOverride", priceOverride);
                    return row;
                }
        );

        product.put("images", images);
        product.put("variants", variants);
        return product;
    }

    private void ensureProductExists(Long id) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE id = ?", Long.class, id);
        if (count == null || count == 0) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    private void validateUpsertRequest(ProductUpsertRequest request) {
        String gender = request.getGender().trim().toLowerCase(Locale.ROOT);
        if (!VALID_GENDERS.contains(gender)) {
            throw new BadRequestException("Dữ liệu đầu vào không hợp lệ",
                    List.of(new ErrorDetail("gender", "Giới tính không hợp lệ (Hỗ trợ: male, female, unisex)")));
        }

        validateImageFilename("thumbnail", request.getThumbnail());

        List<ProductImageRequest> images = request.getImages() == null ? List.of() : request.getImages();
        for (ProductImageRequest image : images) {
            validateImageFilename("images", image.getImage());
        }

        Set<String> uniqueVariantKeys = new HashSet<>();
        for (ProductVariantRequest variant : request.getVariants()) {
            String key = variant.getColor().trim().toLowerCase(Locale.ROOT) + "|" + variant.getSize().trim().toLowerCase(Locale.ROOT);
            if (!uniqueVariantKeys.add(key)) {
                throw new BadRequestException("Dữ liệu đầu vào không hợp lệ",
                        List.of(new ErrorDetail("variants", "Biến thể color + size bị trùng")));
            }
        }
    }

    private void insertImages(Long productId, List<ProductImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (int i = 0; i < images.size(); i++) {
            ProductImageRequest image = images.get(i);
            int sortOrder = image.getSortOrder() == null ? (i + 1) : image.getSortOrder();
            jdbcTemplate.update(
                    "INSERT INTO product_images(product_id, image, sort_order) VALUES (?, ?, ?)",
                    productId,
                    image.getImage().trim(),
                    sortOrder
            );
        }
    }

    private void insertVariants(Long productId, List<ProductVariantRequest> variants) {
        for (ProductVariantRequest variant : variants) {
            jdbcTemplate.update(
                    "INSERT INTO product_variants(product_id, color, size, stock, price_override) VALUES (?, ?, ?, ?, ?)",
                    productId,
                    variant.getColor().trim(),
                    variant.getSize().trim(),
                    variant.getStock(),
                    variant.getPriceOverride()
            );
        }
    }

    private void validateImageFilename(String field, String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Dữ liệu đầu vào không hợp lệ",
                    List.of(new ErrorDetail(field, "Ảnh sản phẩm không được để trống")));
        }
    }

    private String resolveSort(String sort, boolean admin) {
        String defaultSort = admin ? "newest" : "best_selling";
        String normalizedSort = normalizeNullable(sort);
        String finalSort = normalizedSort == null ? defaultSort : normalizedSort;
        String invalidSortMessage = "Giá trị sắp xếp (sort) không được hỗ trợ (chỉ hỗ trợ: best_selling, newest, price_asc, price_desc)";

        return switch (finalSort) {
            case "best_selling" -> "sold_count DESC, p.created_at DESC";
            case "newest" -> "p.created_at DESC";
            case "price_asc" -> "p.price ASC, p.created_at DESC";
            case "price_desc" -> "p.price DESC, p.created_at DESC";
            default -> throw new BadRequestException(admin ? "Lỗi truy vấn" : "Dữ liệu truy vấn không hợp lệ",
                    List.of(new ErrorDetail("sort", invalidSortMessage)));
        };
    }

    private void validatePriceRange(boolean admin, Long minPrice, Long maxPrice) {
        if (admin) {
            if ((minPrice != null && minPrice < 0)
                    || (maxPrice != null && maxPrice < 0)
                    || (minPrice != null && maxPrice != null && maxPrice < minPrice)) {
                throw new BadRequestException("Lỗi truy vấn",
                        List.of(new ErrorDetail("minPrice", "Giá trị khoảng giá không hợp lệ")));
            }
            return;
        }

        if (minPrice != null && minPrice < 0) {
            throw new BadRequestException("Dữ liệu truy vấn không hợp lệ",
                    List.of(new ErrorDetail("minPrice", "Giá trị minPrice phải là số nguyên và lớn hơn hoặc bằng 0")));
        }

        if ((maxPrice != null && maxPrice < 0)
                || (minPrice != null && maxPrice != null && maxPrice < minPrice)) {
            throw new BadRequestException("Dữ liệu truy vấn không hợp lệ",
                    List.of(new ErrorDetail("maxPrice", "Giá trị maxPrice phải là số nguyên, lớn hơn hoặc bằng 0, và không được nhỏ hơn minPrice")));
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

    private Boolean parseNullableBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new BadRequestException("Lỗi truy vấn",
                List.of(new ErrorDetail("isActive", "isActive phải là true hoặc false")));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String nullableTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
