package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.List;

public class BadRequestException extends AppException {
    public BadRequestException(String message, List<ErrorDetail> errors) {
        super(HttpStatus.BAD_REQUEST, message, errors);
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message, List.of(new ErrorDetail(null, message)));
    }
}
