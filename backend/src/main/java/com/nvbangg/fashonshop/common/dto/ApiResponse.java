package com.nvbangg.fashonshop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final List<ErrorDetail> errors;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Collections.emptyList());
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, Collections.emptyList());
    }

    public static ApiResponse<Void> error(String message, List<ErrorDetail> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}
