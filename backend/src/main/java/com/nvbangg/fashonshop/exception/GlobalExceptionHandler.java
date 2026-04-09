package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex,
                                                                       HttpServletRequest request) {
        List<ErrorDetail> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorDetail)
                .toList();

        String message = resolveValidationMessage(request);
        return ResponseEntity.badRequest().body(ApiResponse.error(message, errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest request) {
        String uri = request.getRequestURI();
        String field = ex.getName();

        if ("/api/products".equals(uri)) {
            if ("minPrice".equals(field)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Dữ liệu truy vấn không hợp lệ",
                        List.of(new ErrorDetail("minPrice", "Giá trị minPrice phải là số nguyên và lớn hơn hoặc bằng 0"))
                ));
            }
            if ("maxPrice".equals(field)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Dữ liệu truy vấn không hợp lệ",
                        List.of(new ErrorDetail("maxPrice", "Giá trị maxPrice phải là số nguyên, lớn hơn hoặc bằng 0, và không được nhỏ hơn minPrice"))
                ));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Dữ liệu truy vấn không hợp lệ",
                    List.of(new ErrorDetail(field, "Giá trị truy vấn không hợp lệ"))
            ));
        }

        if ("/api/admin/products".equals(uri)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Lỗi truy vấn",
                    List.of(new ErrorDetail("minPrice", "Giá trị khoảng giá không hợp lệ"))
            ));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Dữ liệu đầu vào không hợp lệ",
                List.of(new ErrorDetail(field, "Giá trị không hợp lệ"))
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống", List.of(new ErrorDetail("server", "Vui lòng thử lại sau"))));
    }

    private ErrorDetail toErrorDetail(FieldError error) {
        return new ErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private String resolveValidationMessage(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if ("/api/auth/login".equals(uri)) {
            return "Thiếu thông tin đăng nhập";
        }
        if ("/api/users/me".equals(uri) && "PUT".equalsIgnoreCase(method)) {
            return "Dữ liệu không hợp lệ";
        }
        if ("/api/cart".equals(uri) && "POST".equalsIgnoreCase(method)) {
            return "Dữ liệu không hợp lệ";
        }
        if ("/api/orders".equals(uri) && "POST".equalsIgnoreCase(method)) {
            return "Tạo đơn hàng thất bại";
        }
        if (uri.startsWith("/api/cart/") && "PUT".equalsIgnoreCase(method)) {
            return "Cập nhật thất bại";
        }
        if ("/api/users/password".equals(uri) && "PUT".equalsIgnoreCase(method)) {
            return "Đổi mật khẩu thất bại";
        }
        if ("/api/admin/products".equals(uri) && "POST".equalsIgnoreCase(method)) {
            return "Dữ liệu sản phẩm không hợp lệ";
        }
        if (uri.startsWith("/api/admin/products/") && "PUT".equalsIgnoreCase(method)) {
            return "Dữ liệu cập nhật không hợp lệ";
        }
        if (uri.startsWith("/api/admin/orders/") && "PUT".equalsIgnoreCase(method)) {
            return "Cập nhật thất bại";
        }
        if (uri.startsWith("/api/admin/users/") && "PUT".equalsIgnoreCase(method)) {
            return "Cập nhật thất bại";
        }

        return "Dữ liệu đầu vào không hợp lệ";
    }
}
