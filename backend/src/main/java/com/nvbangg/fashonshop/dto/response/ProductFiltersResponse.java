package com.nvbangg.fashonshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProductFiltersResponse {
    private final List<String> categories;
    private final List<String> genders;
    private final List<String> colors;
    private final List<String> sizes;
}
