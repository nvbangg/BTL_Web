package com.example.fashonshop.util;

import java.util.Set;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    public static final int DEFAULT_PRODUCT_PAGE = 1;
    public static final int DEFAULT_PRODUCT_PAGE_SIZE = 16;
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final Set<String> ALLOWED_SORTS = Set.of("best_selling", "newest", "price_asc", "price_desc");
    public static final Set<String> ALLOWED_ORDER_STATUSES = Set.of("pending", "processing", "shipped", "delivered", "cancelled");
    public static final Set<String> ALLOWED_GENDERS = Set.of("male", "female", "unisex");
    public static final Set<String> ALLOWED_ROLES = Set.of(ROLE_ADMIN, ROLE_USER);
}
