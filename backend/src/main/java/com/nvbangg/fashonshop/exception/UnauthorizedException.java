package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.List;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message, List<ErrorDetail> errors) {
        super(HttpStatus.UNAUTHORIZED, message, errors);
    }

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message,
                List.of(new ErrorDetail("authorization", "Vui lòng đăng nhập để thực hiện chức năng này")));
    }
}
