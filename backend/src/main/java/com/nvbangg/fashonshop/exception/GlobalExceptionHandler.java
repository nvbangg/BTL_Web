package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_MESSAGE = "Dữ liệu không hợp lệ";

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorDetail)
                .toList();

        return ResponseEntity.badRequest().body(ApiResponse.error(VALIDATION_MESSAGE, errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest request) {
        String uri = request.getRequestURI();
        String field = ex.getName();

        if ("/api/products".equals(uri)) {
            if ("minPrice".equals(field)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        VALIDATION_MESSAGE,
                        List.of(new ErrorDetail("minPrice", "Giá trị minPrice phải là số nguyên và lớn hơn hoặc bằng 0"))
                ));
            }
            if ("maxPrice".equals(field)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        VALIDATION_MESSAGE,
                        List.of(new ErrorDetail("maxPrice", "Giá trị maxPrice phải là số nguyên, lớn hơn hoặc bằng 0, và không được nhỏ hơn minPrice"))
                ));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    VALIDATION_MESSAGE,
                    List.of(new ErrorDetail(field, "Giá trị truy vấn không hợp lệ"))
            ));
        }

        if ("/api/admin/products".equals(uri)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    VALIDATION_MESSAGE,
                    List.of(new ErrorDetail("minPrice", "Giá trị khoảng giá không hợp lệ"))
            ));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error(
                VALIDATION_MESSAGE,
                List.of(new ErrorDetail(field, "Giá trị không hợp lệ"))
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                VALIDATION_MESSAGE,
                List.of(new ErrorDetail("body", "JSON không hợp lệ hoặc sai định dạng"))
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMessage = extractRootMessage(ex).toLowerCase(Locale.ROOT);
        if (rootMessage.contains("users.email") || rootMessage.contains("email")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "Đăng ký thất bại",
                            List.of(new ErrorDetail("email", "Email này đã tồn tại trong hệ thống"))
                    ));
        }

        if (rootMessage.contains("duplicate") || rootMessage.contains("unique")) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    VALIDATION_MESSAGE,
                    List.of(new ErrorDetail("data", "Dữ liệu bị trùng hoặc xung đột"))
            ));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống", List.of(new ErrorDetail("server", "Vui lòng thử lại sau"))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống", List.of(new ErrorDetail("server", "Vui lòng thử lại sau"))));
    }

    private ErrorDetail toErrorDetail(FieldError error) {
        return new ErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private String extractRootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? "" : root.getMessage();
    }
}
