package com.example.fashonshop.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageData<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;
}
