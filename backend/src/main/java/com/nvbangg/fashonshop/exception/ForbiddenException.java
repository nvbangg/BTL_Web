package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ForbiddenException extends AppException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message, List.of(new ErrorDetail(null, message)));
    }
}
