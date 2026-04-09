package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ConflictException extends AppException {
    public ConflictException(String message, List<ErrorDetail> errors) {
        super(HttpStatus.CONFLICT, message, errors);
    }

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message, List.of(new ErrorDetail(null, message)));
    }
}
