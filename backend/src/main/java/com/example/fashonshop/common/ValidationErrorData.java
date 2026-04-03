package com.example.fashonshop.common;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ValidationErrorData {
    private final Map<String, String> fieldErrors;
}
