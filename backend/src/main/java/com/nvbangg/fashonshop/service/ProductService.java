package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.*;
import com.nvbangg.fashonshop.dto.response.*;
import com.nvbangg.fashonshop.entity.ProductGender;
import com.nvbangg.fashonshop.entity.ProductImage;
import com.nvbangg.fashonshop.entity.ProductVariant;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.NotFoundException;
import com.nvbangg.fashonshop.repository.ProductImageRepository;
import com.nvbangg.fashonshop.repository.ProductRepository;
import com.nvbangg.fashonshop.repository.ProductVariantRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Service
public class ProductService {

    private static final Set<ProductGender> VALID_GENDERS = EnumSet.allOf(ProductGender.class);

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductService(JdbcTemplate jdbcTemplate,
                          ProductRepository productRepository,
                          ProductImageRepository productImageRepository,
                          ProductVariantRepository productVariantRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public ProductListResponse getPublicProducts(String keyword,
                                                 String category,
                                                 String gender,
                                                 String color,
                                                 String size,
                                                 Long minPrice,
                                                 Long maxPrice,
                                                 String sort,
                                                 String page,
                                                 String pageSize) {
        ProductSearchResult result = getProductsInternal(false, keyword, category, gender, color, size, minPrice, maxPrice, sort, null, page, pageSize);
        List<ProductListItemResponse> items = result.items.stream()
                .map(item -> new ProductListItemResponse(
                        item.id,
                        item.name,
                        item.thumbnail,
                        item.price
                ))
                .toList();

        return new ProductListResponse(
                items,
                result.page,
                result.pageSize,
                result.total
        );
    }

    public AdminProductListResponse getAdminProducts(String keyword,
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
        ProductSearchResult result = getProductsInternal(true, keyword, category, gender, color, size, minPrice, maxPrice, sort, isActive, page, pageSize);
        List<AdminProductListItemResponse> items = result.items.stream()
                .map(item -> new AdminProductListItemResponse(
                        item.id,
                        item.thumbnail,
                        item.name,
                        item.price,
                        item.soldCount,
                        item.totalStock,
                        item.isActive
                ))
                .toList();

        return new AdminProductListResponse(
                result.totalActiveProducts,
                result.totalOutOfStockProducts,
                items,
                result.page,
                result.pageSize,
                result.total
        );
    }

    public ProductFiltersResponse getFilters() {
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

        return new ProductFiltersResponse(
                categories,
                VALID_GENDERS.stream().map(ProductGender::name).toList(),
                colors,
                sizes
        );
    }

    public ProductDetailResponse getPublicProductDetail(Long id) {
        ProductDetailRow product = getProductDetailInternal(id, false);
        return new ProductDetailResponse(
                product.id,
                product.name,
                product.description,
                product.thumbnail,
                product.category,
                product.gender,
                product.price,
                product.soldCount,
                getProductImages(id),
                getProductVariants(id)
        );
    }

    public AdminProductDetailResponse getAdminProductDetail(Long id) {
        ProductDetailRow product = getProductDetailInternal(id, true);
        return new AdminProductDetailResponse(
                product.id,
                product.name,
                product.description,
                product.thumbnail,
                product.category,
                product.gender,
                product.price,
                product.isActive,
                getProductImages(id),
                getProductVariants(id)
        );
    }

    @Transactional
    public CreateProductResponse createProduct(ProductCreateRequest request) {
        ProductGender gender = validateCreateRequest(request);

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
            ps.setString(5, gender.name());
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

        return new CreateProductResponse(productId);
    }

    @Transactional
    public void updateProduct(Long id, ProductUpdateRequest request) {
        ensureProductExists(id);
        ProductGender gender = validateUpdateRequest(id, request);

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
                gender.name(),
                request.getPrice(),
                request.getIsActive() == null || request.getIsActive(),
                id
        );

        upsertImages(id, request.getImages());
        upsertVariants(id, request.getVariants());
    }

    private ProductSearchResult getProductsInternal(boolean admin,
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

        ProductGender normalizedGender = parseNullableGenderForQuery(gender);

        Boolean activeFilter = admin ? parseNullableBoolean(isActive) : null;

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
            params.add(normalizedGender.name());
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
                Long.class,
                params.toArray()
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

        List<ProductSearchRow> items = jdbcTemplate.query(listSql, (rs, rowNum) -> new ProductSearchRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("thumbnail"),
                rs.getLong("price"),
                rs.getLong("sold_count"),
                rs.getLong("total_stock"),
                rs.getBoolean("is_active")
        ), listParams.toArray());

        Long totalActiveProducts = null;
        Long totalOutOfStockProducts = null;
        if (admin) {
            totalActiveProducts = productRepository.countByIsActiveTrue();
            totalOutOfStockProducts = jdbcTemplate.queryForObject(
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
        }

        return new ProductSearchResult(
                items,
                pageValue,
                pageSizeValue,
                total == null ? 0L : total,
                totalActiveProducts,
                totalOutOfStockProducts
        );
    }

    private ProductDetailRow getProductDetailInternal(Long id, boolean admin) {
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

        List<ProductDetailRow> products = jdbcTemplate.query(sql, (rs, rowNum) -> new ProductDetailRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("thumbnail"),
                rs.getString("category"),
                rs.getString("gender"),
                rs.getLong("price"),
                rs.getLong("sold_count"),
                rs.getBoolean("is_active")
        ), id);

        if (products.isEmpty()) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }

        return products.getFirst();
    }

    private List<ProductImageResponse> getProductImages(Long productId) {
        return productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId)
                .stream()
                .map(this::toProductImageResponse)
                .toList();
    }

    private List<ProductVariantResponse> getProductVariants(Long productId) {
        return productVariantRepository.findByProductIdOrderByColorAscSizeAsc(productId)
                .stream()
                .map(this::toProductVariantResponse)
                .toList();
    }

    private static class ProductSearchResult {
        private final List<ProductSearchRow> items;
        private final Integer page;
        private final Integer pageSize;
        private final Long total;
        private final Long totalActiveProducts;
        private final Long totalOutOfStockProducts;

        private ProductSearchResult(List<ProductSearchRow> items,
                                    Integer page,
                                    Integer pageSize,
                                    Long total,
                                    Long totalActiveProducts,
                                    Long totalOutOfStockProducts) {
            this.items = items;
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalActiveProducts = totalActiveProducts;
            this.totalOutOfStockProducts = totalOutOfStockProducts;
        }
    }

    private static class ProductSearchRow {
        private final Long id;
        private final String name;
        private final String thumbnail;
        private final Long price;
        private final Long soldCount;
        private final Long totalStock;
        private final Boolean isActive;

        private ProductSearchRow(Long id,
                                 String name,
                                 String thumbnail,
                                 Long price,
                                 Long soldCount,
                                 Long totalStock,
                                 Boolean isActive) {
            this.id = id;
            this.name = name;
            this.thumbnail = thumbnail;
            this.price = price;
            this.soldCount = soldCount;
            this.totalStock = totalStock;
            this.isActive = isActive;
        }
    }

    private static class ProductDetailRow {
        private final Long id;
        private final String name;
        private final String description;
        private final String thumbnail;
        private final String category;
        private final String gender;
        private final Long price;
        private final Long soldCount;
        private final Boolean isActive;

        private ProductDetailRow(Long id,
                                 String name,
                                 String description,
                                 String thumbnail,
                                 String category,
                                 String gender,
                                 Long price,
                                 Long soldCount,
                                 Boolean isActive) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.thumbnail = thumbnail;
            this.category = category;
            this.gender = gender;
            this.price = price;
            this.soldCount = soldCount;
            this.isActive = isActive;
        }
    }

    private record ExistingVariantKey(Long id, String key) {
    }

    private record ImageUpsertInstruction(Long imageId, String imageUrl, Integer sortOrder) {
    }

    private void ensureProductExists(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy dữ liệu yêu cầu");
        }
    }

    private ProductGender validateCreateRequest(ProductCreateRequest request) {
        ProductGender gender = validateGender(request.getGender());
        validateUniqueImageSortOrdersForCreate(request.getImages());
        validateUniqueVariantKeysForCreate(request.getVariants());
        return gender;
    }

    private ProductGender validateUpdateRequest(Long productId, ProductUpdateRequest request) {
        ProductGender gender = validateGender(request.getGender());
        validateUniqueImageSortOrdersForUpdate(productId, request.getImages());
        validateUniqueVariantKeysForUpdate(productId, request.getVariants());
        return gender;
    }

    private void validateUniqueImageSortOrdersForCreate(List<ProductCreateImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        Map<Integer, Integer> seenSortOrders = new HashMap<>();
        for (int i = 0; i < images.size(); i++) {
            int sortOrder = resolveImageSortOrderForCreate(i, images.get(i));
            Integer previousIndex = seenSortOrders.putIfAbsent(sortOrder, i);
            if (previousIndex != null) {
                throwDuplicateImageSortOrderError(i);
            }
        }
    }

    private void validateUniqueImageSortOrdersForUpdate(Long productId, List<ProductUpdateImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        List<ProductImage> existingImages = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
        Map<Long, Integer> existingSortById = new HashMap<>();
        Map<Long, Integer> finalSortByOwner = new HashMap<>();

        for (ProductImage existingImage : existingImages) {
            existingSortById.put(existingImage.getId(), existingImage.getSortOrder());
            finalSortByOwner.put(existingImage.getId(), existingImage.getSortOrder());
        }

        Set<Long> seenImageIds = new HashSet<>();
        Map<Long, Integer> payloadIndexByOwner = new HashMap<>();

        for (int i = 0; i < images.size(); i++) {
            ProductUpdateImageRequest image = images.get(i);
            Long imageId = image.getId();
            int resolvedSortOrder = resolveImageSortOrderForUpdate(i, image, existingSortById);

            Long ownerId;
            if (imageId != null && existingSortById.containsKey(imageId)) {
                if (!seenImageIds.add(imageId)) {
                    throwDuplicateImageIdError(i);
                }
                ownerId = imageId;
            } else {
                ownerId = -1L - i;
            }

            finalSortByOwner.put(ownerId, resolvedSortOrder);
            payloadIndexByOwner.put(ownerId, i);
        }

        Map<Integer, Long> firstOwnerBySortOrder = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : finalSortByOwner.entrySet()) {
            Integer sortOrder = entry.getValue();
            Long previousOwner = firstOwnerBySortOrder.putIfAbsent(sortOrder, entry.getKey());
            if (previousOwner == null) {
                continue;
            }

            Integer payloadIndex = payloadIndexByOwner.get(entry.getKey());
            if (payloadIndex == null) {
                payloadIndex = payloadIndexByOwner.get(previousOwner);
            }

            if (payloadIndex != null) {
                throwDuplicateImageSortOrderError(payloadIndex);
            }

            throwDuplicateImageSortOrderError("images");
        }
    }

    private ProductGender validateGender(String genderValue) {
        ProductGender gender = parseGender(genderValue);
        if (!VALID_GENDERS.contains(gender)) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("gender", "Giới tính không hợp lệ")));
        }
        return gender;
    }

    private void validateUniqueVariantKeysForCreate(List<ProductCreateVariantRequest> variants) {
        Set<String> uniqueVariantKeys = new HashSet<>();
        for (ProductCreateVariantRequest variant : variants) {
            String key = normalizeVariantKey(variant.getColor(), variant.getSize());
            if (!uniqueVariantKeys.add(key)) {
                throwDuplicateVariantKeyError();
            }
        }
    }

    private void validateUniqueVariantKeysForUpdate(Long productId, List<ProductUpdateVariantRequest> variants) {
        List<ExistingVariantKey> existingVariants = productVariantRepository.findByProductIdOrderByColorAscSizeAsc(productId)
                .stream()
                .map(variant -> new ExistingVariantKey(
                        variant.getId(),
                        normalizeVariantKey(variant.getColor(), variant.getSize())
                ))
                .toList();

        Map<Long, String> existingKeysById = new HashMap<>();
        Set<String> finalKeys = new HashSet<>();
        for (ExistingVariantKey existingVariant : existingVariants) {
            existingKeysById.put(existingVariant.id(), existingVariant.key());
            finalKeys.add(existingVariant.key());
        }

        Map<Long, Integer> seenIdToIndex = new HashMap<>();
        Set<Long> seenVariantIds = new HashSet<>();
        for (int i = 0; i < variants.size(); i++) {
            ProductUpdateVariantRequest variant = variants.get(i);
            Long variantId = variant.getId();

            if (variantId == null) {
                continue;
            }

            Integer previousIndex = seenIdToIndex.putIfAbsent(variantId, i);
            if (previousIndex != null) {
                throw new BadRequestException("Dữ liệu không hợp lệ",
                        List.of(new ErrorDetail("variants[" + i + "].id", "ID phân loại bị trùng trong payload")));
            }

            if (!existingKeysById.containsKey(variantId)) {
                throw new BadRequestException("Dữ liệu không hợp lệ",
                        List.of(new ErrorDetail("variants[" + i + "].id", "ID phân loại của sản phẩm không tồn tại")));
            }

            seenVariantIds.add(variantId);
        }

        Set<Long> missingExistingVariantIds = new HashSet<>(existingKeysById.keySet());
        missingExistingVariantIds.removeAll(seenVariantIds);
        if (!missingExistingVariantIds.isEmpty()) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("variants", "Không thể xóa phân loại đã tồn tại")));
        }

        for (Long variantId : seenVariantIds) {
            finalKeys.remove(existingKeysById.get(variantId));
        }

        for (ProductUpdateVariantRequest variant : variants) {
            String newKey = normalizeVariantKey(variant.getColor(), variant.getSize());
            Long variantId = variant.getId();

            if (variantId != null && existingKeysById.containsKey(variantId)) {
                if (!finalKeys.add(newKey)) {
                    throwDuplicateVariantKeyError();
                }
                continue;
            }

            if (!finalKeys.add(newKey)) {
                throwDuplicateVariantKeyError();
            }
        }
    }

    private void throwDuplicateVariantKeyError() {
        throw new BadRequestException("Dữ liệu không hợp lệ",
                List.of(new ErrorDetail("variants", "Phân loại color + size bị trùng")));
    }

    private void throwDuplicateImageSortOrderError(int index) {
        throw new BadRequestException("Dữ liệu không hợp lệ",
                List.of(new ErrorDetail("images[" + index + "].sortOrder", "Thứ tự ảnh (sortOrder) bị trùng")));
    }

    private void throwDuplicateImageSortOrderError(String field) {
        throw new BadRequestException("Dữ liệu không hợp lệ",
                List.of(new ErrorDetail(field, "Thứ tự ảnh (sortOrder) bị trùng")));
    }

    private void throwDuplicateImageIdError(int index) {
        throw new BadRequestException("Dữ liệu không hợp lệ",
                List.of(new ErrorDetail("images[" + index + "].id", "ID ảnh bị trùng trong payload")));
    }

    private String normalizeVariantKey(String color, String size) {
        return color.trim().toLowerCase(Locale.ROOT) + "|" + size.trim().toLowerCase(Locale.ROOT);
    }

    private int resolveImageSortOrderForCreate(int index, ProductCreateImageRequest image) {
        return image.getSortOrder() == null ? (index + 1) : image.getSortOrder();
    }

    private int resolveImageSortOrderForUpdate(int index,
                                               ProductUpdateImageRequest image,
                                               Map<Long, Integer> existingSortById) {
        if (image.getSortOrder() != null) {
            return image.getSortOrder();
        }

        Long imageId = image.getId();
        if (imageId != null && existingSortById.containsKey(imageId)) {
            return existingSortById.get(imageId);
        }

        return index + 1;
    }

    private ProductImageResponse toProductImageResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImage(),
                image.getSortOrder()
        );
    }

    private ProductVariantResponse toProductVariantResponse(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getColor(),
                variant.getSize(),
                variant.getStock(),
                variant.getPriceOverride()
        );
    }

    private ProductGender parseNullableGenderForQuery(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        try {
            return ProductGender.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Dữ liệu truy vấn không hợp lệ",
                    List.of(new ErrorDetail("gender", "Giới tính (gender) không hợp lệ (chỉ hỗ trợ: male, female, unisex)")));
        }
    }

    private ProductGender parseGender(String value) {
        if (value == null) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("gender", "Giới tính không hợp lệ")));
        }

        try {
            return ProductGender.valueOf(value.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("gender", "Giới tính không hợp lệ")));
        }
    }

    private void insertImages(Long productId, List<ProductCreateImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (int i = 0; i < images.size(); i++) {
            ProductCreateImageRequest image = images.get(i);
            int sortOrder = resolveImageSortOrderForCreate(i, image);
            jdbcTemplate.update(
                    "INSERT INTO product_images(product_id, image, sort_order) VALUES (?, ?, ?)",
                    productId,
                    image.getImage().trim(),
                    sortOrder
            );
        }
    }

    private void insertVariants(Long productId, List<ProductCreateVariantRequest> variants) {
        for (ProductCreateVariantRequest variant : variants) {
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

    private void upsertImages(Long productId, List<ProductUpdateImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        List<ProductImage> existingImages = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
        Map<Long, Integer> existingSortById = new HashMap<>();
        for (ProductImage existingImage : existingImages) {
            existingSortById.put(existingImage.getId(), existingImage.getSortOrder());
        }

        List<ImageUpsertInstruction> instructions = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            ProductUpdateImageRequest image = images.get(i);
            int sortOrder = resolveImageSortOrderForUpdate(i, image, existingSortById);
            instructions.add(new ImageUpsertInstruction(image.getId(), image.getImage().trim(), sortOrder));
        }

        Map<Long, Integer> temporarySortByImageId = new HashMap<>();
        Set<Integer> occupiedSortOrders = new HashSet<>(existingSortById.values());
        Set<Integer> assignedTemporarySortOrders = new HashSet<>();
        int temporarySort = -1;

        for (ImageUpsertInstruction instruction : instructions) {
            Long imageId = instruction.imageId();
            if (imageId == null || !existingSortById.containsKey(imageId)) {
                continue;
            }

            int currentSortOrder = existingSortById.get(imageId);
            if (currentSortOrder == instruction.sortOrder()) {
                continue;
            }

            while (occupiedSortOrders.contains(temporarySort)
                    || assignedTemporarySortOrders.contains(temporarySort)) {
                temporarySort--;
            }

            temporarySortByImageId.put(imageId, temporarySort);
            assignedTemporarySortOrders.add(temporarySort);
            temporarySort--;
        }

        for (Map.Entry<Long, Integer> entry : temporarySortByImageId.entrySet()) {
            jdbcTemplate.update(
                    "UPDATE product_images SET sort_order = ? WHERE id = ? AND product_id = ?",
                    entry.getValue(),
                    entry.getKey(),
                    productId
            );
        }

        for (ImageUpsertInstruction instruction : instructions) {
            Long imageId = instruction.imageId();
            if (imageId != null && existingSortById.containsKey(imageId)) {
                jdbcTemplate.update(
                        """
                                UPDATE product_images
                                SET image = ?, sort_order = ?
                                WHERE id = ? AND product_id = ?
                                """,
                        instruction.imageUrl(),
                        instruction.sortOrder(),
                        imageId,
                        productId
                );
                continue;
            }

            jdbcTemplate.update(
                    "INSERT INTO product_images(product_id, image, sort_order) VALUES (?, ?, ?)",
                    productId,
                    instruction.imageUrl(),
                    instruction.sortOrder()
            );
        }
    }

    private void upsertVariants(Long productId, List<ProductUpdateVariantRequest> variants) {
        for (int i = 0; i < variants.size(); i++) {
            ProductUpdateVariantRequest variant = variants.get(i);
            if (variant.getId() != null) {
                int updated = jdbcTemplate.update(
                        """
                                UPDATE product_variants
                                SET color = ?, size = ?, stock = ?, price_override = ?
                                WHERE id = ? AND product_id = ?
                                """,
                        variant.getColor().trim(),
                        variant.getSize().trim(),
                        variant.getStock(),
                        variant.getPriceOverride(),
                        variant.getId(),
                        productId
                );
                if (updated > 0) {
                    continue;
                }

                throw new BadRequestException("Dữ liệu không hợp lệ",
                        List.of(new ErrorDetail("variants[" + i + "].id", "ID phân loại của sản phẩm không tồn tại")));
            }

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
