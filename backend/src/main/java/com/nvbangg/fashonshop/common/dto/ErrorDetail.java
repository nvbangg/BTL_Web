package com.nvbangg.fashonshop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorDetail {
    private final String field;
    private final String message;
}
