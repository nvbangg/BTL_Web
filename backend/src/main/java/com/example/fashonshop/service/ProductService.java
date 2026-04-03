package com.example.fashonshop.service;

import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.admin.CategoryItemResponse;
import com.example.fashonshop.dto.product.ProductDetailResponse;
import com.example.fashonshop.dto.product.ProductFiltersResponse;
import com.example.fashonshop.dto.product.ProductImageResponse;
import com.example.fashonshop.dto.product.ProductListItemResponse;
import com.example.fashonshop.dto.product.ProductVariantResponse;
import com.example.fashonshop.entity.Category;
import com.example.fashonshop.entity.Product;
import com.example.fashonshop.entity.ProductVariant;
import com.example.fashonshop.exception.BadRequestException;
import com.example.fashonshop.exception.NotFoundException;
import com.example.fashonshop.repository.CategoryRepository;
import com.example.fashonshop.repository.ProductRepository;
import com.example.fashonshop.repository.ProductVariantRepository;
import com.example.fashonshop.util.AppConstants;
import com.example.fashonshop.util.PageRequestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PageData<ProductListItemResponse> getProducts(String keyword,
                                                         Long category,
                                                         String gender,
                                                         String color,
                                                         String size,
                                                         BigDecimal minPrice,
                                                         BigDecimal maxPrice,
                                                         String sort,
                                                         int page,
                                                         int pageSize,
                                                         Boolean isActive,
                                                         boolean adminView) {
        validatePriceRange(minPrice, maxPrice);

        String normalizedGender = normalizeOrNull(gender);
        if (normalizedGender != null && !AppConstants.ALLOWED_GENDERS.contains(normalizedGender)) {
            throw new BadRequestException("INVALID_FILTER", "Invalid gender filter");
        }

        String normalizedSort = normalizeOrNull(sort);
        if (normalizedSort == null) {
            normalizedSort = "best_selling";
        }
        if (!AppConstants.ALLOWED_SORTS.contains(normalizedSort)) {
            throw new BadRequestException("INVALID_FILTER", "Invalid sort filter");
        }

        String normalizedColor = normalizeOrNull(color);
        String normalizedSize = normalizeOrNull(size);

        Specification<Product> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (!adminView || isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), adminView ? isActive : true));
            }

            if (hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%"));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category").get("id"), category));
            }

            if (normalizedGender != null) {
                predicates.add(cb.equal(cb.lower(root.get("gender")), normalizedGender));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (normalizedColor != null || normalizedSize != null) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                if (normalizedColor != null) {
                    predicates.add(cb.equal(cb.lower(variantJoin.get("color")), normalizedColor));
                }
                if (normalizedSize != null) {
                    predicates.add(cb.equal(cb.upper(variantJoin.get("size")), normalizedSize.toUpperCase(Locale.ROOT)));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequestUtil.create(page, pageSize, resolveSort(normalizedSort));
        Page<Product> productPage = productRepository.findAll(specification, pageable);

        List<ProductListItemResponse> items = productPage.getContent().stream()
                .map(product -> new ProductListItemResponse(
                        product.getId(),
                        product.getName(),
                        product.getThumbnail(),
                        product.getPrice(),
                        product.getGender(),
                        product.getCategory().getId(),
                        adminView ? product.getIsActive() : null
                ))
                .toList();

        return PageData.<ProductListItemResponse>builder()
                .items(items)
                .page(Math.max(1, page))
                .pageSize(Math.max(1, Math.min(pageSize, 100)))
                .total(productPage.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductFiltersResponse getFilters(boolean onlyActive) {
        List<Category> categories = onlyActive
                ? categoryRepository.findAllByActiveProducts()
                : categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        List<CategoryItemResponse> categoryItems = categories.stream()
                .map(c -> new CategoryItemResponse(c.getId(), c.getName()))
                .toList();

        return new ProductFiltersResponse(
                categoryItems,
                productVariantRepository.findDistinctColors(onlyActive),
                productVariantRepository.findDistinctSizes(onlyActive),
                productRepository.findDistinctGenders(onlyActive)
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicProductDetail(Long id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        return toProductDetail(product, false);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getAdminProductDetail(Long id) {
        Product product = productRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        return toProductDetail(product, true);
    }

    private ProductDetailResponse toProductDetail(Product product, boolean includeIsActive) {
        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparing(i -> i.getSortOrder() == null ? Integer.MAX_VALUE : i.getSortOrder()))
                .map(i -> new ProductImageResponse(i.getId(), i.getImage(), i.getSortOrder()))
                .toList();

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getColor).thenComparing(ProductVariant::getSize))
                .map(v -> new ProductVariantResponse(v.getId(), v.getSize(), v.getColor(), v.getStock(), v.getPriceOverride()))
                .toList();

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getThumbnail(),
                product.getPrice(),
                product.getGender(),
                product.getCategory().getId(),
                includeIsActive ? product.getIsActive() : null,
                images,
                variants
        );
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("INVALID_FILTER", "minPrice must be >= 0");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("INVALID_FILTER", "maxPrice must be >= 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("INVALID_FILTER", "minPrice must be <= maxPrice");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeOrNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
