package com.nvbangg.fashonshop.exception;

import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_MESSAGE = "Dữ liệu không hợp lệ";
    private static final String SYSTEM_MESSAGE = "Lỗi hệ thống";

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
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName();
        if (field == null || field.isBlank()) {
            field = "request";
        }
        return badRequest(VALIDATION_MESSAGE, field, "Giá trị không hợp lệ");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadableBody(HttpMessageNotReadableException ex) {
        return badRequest(VALIDATION_MESSAGE, "body", "JSON không hợp lệ hoặc sai định dạng");
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
            return badRequest(VALIDATION_MESSAGE, "data", "Dữ liệu bị trùng hoặc xung đột");
        }

        return serverError();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return badRequest(VALIDATION_MESSAGE, "file", "Tệp quá lớn (tối đa 5MB)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return serverError();
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String message, String field, String detail) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, List.of(new ErrorDetail(field, detail))));
    }

    private ResponseEntity<ApiResponse<Void>> serverError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(SYSTEM_MESSAGE));
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
