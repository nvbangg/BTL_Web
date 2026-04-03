package com.example.fashonshop.util;

import com.example.fashonshop.entity.ProductVariant;

import java.math.BigDecimal;

public final class PriceUtil {

    private PriceUtil() {
    }

    public static BigDecimal effectiveVariantPrice(ProductVariant variant) {
        if (variant.getPriceOverride() != null) {
            return variant.getPriceOverride();
        }
        return variant.getProduct().getPrice();
    }
}
