package com.example.fashonshop.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestUtil {

    private PageRequestUtil() {
    }

    public static Pageable create(int page, int pageSize, Sort sort) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return PageRequest.of(safePage - 1, safePageSize, sort);
    }
}
