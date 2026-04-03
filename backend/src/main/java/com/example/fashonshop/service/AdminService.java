package com.example.fashonshop.service;

import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.admin.AdminCategoryRequest;
import com.example.fashonshop.dto.admin.AdminOrderItemResponse;
import com.example.fashonshop.dto.admin.AdminOrderStatusRequest;
import com.example.fashonshop.dto.admin.AdminProductUpsertRequest;
import com.example.fashonshop.dto.admin.AdminStatisticsResponse;
import com.example.fashonshop.dto.admin.AdminUserItemResponse;
import com.example.fashonshop.dto.admin.AdminUserRoleRequest;
import com.example.fashonshop.dto.admin.CategoryItemResponse;
import com.example.fashonshop.dto.admin.MonthlyRevenueResponse;
import com.example.fashonshop.dto.admin.ProductImageRequest;
import com.example.fashonshop.dto.admin.ProductVariantRequest;
import com.example.fashonshop.entity.Category;
import com.example.fashonshop.entity.OrderEntity;
import com.example.fashonshop.entity.Product;
import com.example.fashonshop.entity.ProductImage;
import com.example.fashonshop.entity.ProductVariant;
import com.example.fashonshop.entity.User;
import com.example.fashonshop.exception.BadRequestException;
import com.example.fashonshop.exception.ConflictException;
import com.example.fashonshop.exception.NotFoundException;
import com.example.fashonshop.repository.CategoryRepository;
import com.example.fashonshop.repository.OrderRepository;
import com.example.fashonshop.repository.ProductRepository;
import com.example.fashonshop.repository.RevenueByMonthProjection;
import com.example.fashonshop.repository.UserRepository;
import com.example.fashonshop.util.AppConstants;
import com.example.fashonshop.util.PageRequestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createProduct(AdminProductUpsertRequest request) {
        Product product = new Product();
        applyProductRequest(product, request);
        Product savedProduct = productRepository.save(product);
        return savedProduct.getId();
    }

    @Transactional
    public String updateProduct(Long productId, AdminProductUpsertRequest request) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        applyProductRequest(product, request);
        productRepository.save(product);
        return "Product updated";
    }

    @Transactional
    public String deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        productRepository.delete(product);
        return "Product deleted";
    }

    public PageData<AdminOrderItemResponse> getOrders(String keyword, String status, int page, int pageSize) {
        String normalizedStatus = normalizeOrNull(status);
        if (normalizedStatus != null && !AppConstants.ALLOWED_ORDER_STATUSES.contains(normalizedStatus)) {
            throw new BadRequestException("INVALID_STATUS", "Invalid status");
        }

        Specification<OrderEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedStatus != null) {
                predicates.add(cb.equal(cb.lower(root.get("status")), normalizedStatus));
            }

            String normalizedKeyword = normalizeOrNull(keyword);
            if (normalizedKeyword != null) {
                Predicate byEmail = cb.like(cb.lower(root.get("user").get("email")), "%" + normalizedKeyword + "%");
                Predicate byId = cb.disjunction();
                try {
                    Long idValue = Long.valueOf(normalizedKeyword);
                    byId = cb.equal(root.get("id"), idValue);
                } catch (NumberFormatException ignored) {
                    // keyword is not numeric; keep email search only.
                }
                predicates.add(cb.or(byEmail, byId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequestUtil.create(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderEntity> orderPage = orderRepository.findAll(specification, pageable);

        List<AdminOrderItemResponse> items = orderPage.getContent().stream()
                .map(order -> new AdminOrderItemResponse(
                        order.getId(),
                        order.getUser().getId(),
                        order.getStatus(),
                        order.getTotalPrice()
                ))
                .toList();

        return PageData.<AdminOrderItemResponse>builder()
                .items(items)
                .page(Math.max(1, page))
                .pageSize(Math.max(1, Math.min(pageSize, 100)))
                .total(orderPage.getTotalElements())
                .build();
    }

    @Transactional
    public String updateOrderStatus(Long orderId, AdminOrderStatusRequest request) {
        String normalizedStatus = normalizeOrNull(request.status());
        if (normalizedStatus == null || !AppConstants.ALLOWED_ORDER_STATUSES.contains(normalizedStatus)) {
            throw new BadRequestException("INVALID_STATUS", "Invalid status");
        }

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        order.setStatus(normalizedStatus);
        orderRepository.save(order);
        return "Order status updated";
    }

    public PageData<CategoryItemResponse> getCategories(String keyword, int page, int pageSize) {
        Pageable pageable = PageRequestUtil.create(page, pageSize, Sort.by(Sort.Direction.ASC, "name"));
        Page<Category> categoryPage = categoryRepository.search(normalizeOrNull(keyword), pageable);

        List<CategoryItemResponse> items = categoryPage.getContent().stream()
                .map(c -> new CategoryItemResponse(c.getId(), c.getName()))
                .toList();

        return PageData.<CategoryItemResponse>builder()
                .items(items)
                .page(Math.max(1, page))
                .pageSize(Math.max(1, Math.min(pageSize, 100)))
                .total(categoryPage.getTotalElements())
                .build();
    }

    @Transactional
    public CategoryItemResponse createCategory(AdminCategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("CATEGORY_NAME_EXISTS", "Category name already exists");
        }

        Category category = new Category();
        category.setName(name);

        Category saved = categoryRepository.save(category);
        return new CategoryItemResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public String updateCategory(Long categoryId, AdminCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        String newName = request.name().trim();
        if (!newName.equalsIgnoreCase(category.getName()) && categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new ConflictException("CATEGORY_NAME_EXISTS", "Category name already exists");
        }

        category.setName(newName);
        categoryRepository.save(category);
        return "Category updated";
    }

    public PageData<AdminUserItemResponse> getUsers(String keyword, String role, int page, int pageSize) {
        String normalizedRole = normalizeOrNull(role);
        if (normalizedRole != null && !AppConstants.ALLOWED_ROLES.contains(normalizedRole)) {
            throw new BadRequestException("INVALID_ROLE", "Invalid role");
        }

        Pageable pageable = PageRequestUtil.create(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.search(normalizeOrNull(keyword), normalizedRole, pageable);

        List<AdminUserItemResponse> items = userPage.getContent().stream()
                .map(user -> new AdminUserItemResponse(user.getId(), user.getEmail(), user.getRole()))
                .toList();

        return PageData.<AdminUserItemResponse>builder()
                .items(items)
                .page(Math.max(1, page))
                .pageSize(Math.max(1, Math.min(pageSize, 100)))
                .total(userPage.getTotalElements())
                .build();
    }

    @Transactional
    public String updateUserRole(Long userId, AdminUserRoleRequest request) {
        String normalizedRole = normalizeOrNull(request.role());
        if (normalizedRole == null || !AppConstants.ALLOWED_ROLES.contains(normalizedRole)) {
            throw new BadRequestException("INVALID_ROLE", "Invalid role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        user.setRole(normalizedRole);
        userRepository.save(user);
        return "User updated";
    }

    @Transactional
    public String deleteUser(Long actorUserId, Long targetUserId) {
        if (actorUserId.equals(targetUserId)) {
            throw new BadRequestException("CANNOT_DELETE_SELF", "Cannot delete yourself");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        userRepository.delete(user);
        return "User deleted";
    }

    public AdminStatisticsResponse getStatistics() {
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();

        List<MonthlyRevenueResponse> revenueByMonth = orderRepository.revenueByMonth().stream()
                .map(this::toMonthlyRevenue)
                .toList();

        return new AdminStatisticsResponse(totalRevenue, totalOrders, totalUsers, revenueByMonth);
    }

    private MonthlyRevenueResponse toMonthlyRevenue(RevenueByMonthProjection projection) {
        return new MonthlyRevenueResponse(projection.getMonth(), projection.getRevenue());
    }

    private void applyProductRequest(Product product, AdminProductUpsertRequest request) {
        String normalizedGender = normalizeOrNull(request.gender());
        if (normalizedGender == null || !AppConstants.ALLOWED_GENDERS.contains(normalizedGender)) {
            throw new BadRequestException("INVALID_GENDER", "Invalid gender");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        product.setName(request.name().trim());
        product.setCategory(category);
        product.setDescription(request.description());
        product.setThumbnail(request.thumbnail().trim());
        product.setGender(normalizedGender);
        product.setPrice(request.price());
        product.setIsActive(request.isActive());

        replaceProductImages(product, request.images());
        replaceProductVariants(product, request.variants());
    }

    private void replaceProductImages(Product product, List<ProductImageRequest> images) {
        product.getImages().clear();
        if (images == null) {
            return;
        }

        for (ProductImageRequest reqImage : images) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImage(reqImage.image().trim());
            image.setSortOrder(reqImage.sortOrder() == null ? 0 : reqImage.sortOrder());
            product.getImages().add(image);
        }
    }

    private void replaceProductVariants(Product product, List<ProductVariantRequest> variants) {
        product.getVariants().clear();
        if (variants == null) {
            return;
        }

        Set<String> uniqueVariantKeys = new HashSet<>();
        for (ProductVariantRequest reqVariant : variants) {
            String normalizedColor = reqVariant.color().trim().toLowerCase(Locale.ROOT);
            String normalizedSize = reqVariant.size().trim().toUpperCase(Locale.ROOT);
            Integer stock = reqVariant.stock() == null ? 0 : reqVariant.stock();

            if (stock < 0) {
                throw new BadRequestException("INVALID_STOCK", "Stock must be >= 0");
            }

            String key = normalizedColor + "::" + normalizedSize;
            if (!uniqueVariantKeys.add(key)) {
                throw new BadRequestException("DUPLICATE_VARIANT", "Duplicate size/color variant");
            }

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setColor(normalizedColor);
            variant.setSize(normalizedSize);
            variant.setStock(stock);
            variant.setPriceOverride(reqVariant.priceOverride());
            product.getVariants().add(variant);
        }
    }

    private String normalizeOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
