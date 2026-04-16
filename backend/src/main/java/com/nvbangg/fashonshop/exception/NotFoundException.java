package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.List;

public class NotFoundException extends AppException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message,
                List.of(new ErrorDetail("id", "Dữ liệu không tồn tại trong hệ thống hoặc đã bị ẩn/xóa")));
    }

    public NotFoundException(String message, List<ErrorDetail> errors) {
        super(HttpStatus.NOT_FOUND, message, errors);
    }
}
