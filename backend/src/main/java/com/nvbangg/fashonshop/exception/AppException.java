package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final List<ErrorDetail> errors;

    public AppException(HttpStatus status, String message, List<ErrorDetail> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }
}
