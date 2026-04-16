package com.nvbangg.fashonshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String uri = request.getRequestURI();
        String method = request.getMethod();

        ApiResponse<Void> body;
        if ("/api/cart".equals(uri) && "POST".equalsIgnoreCase(method)) {
            body = ApiResponse.error(
                    "Thao tác thất bại",
                    List.of(new ErrorDetail("authorization", "Vui lòng đăng nhập để thêm vào giỏ hàng"))
            );
        } else {
            body = ApiResponse.error(
                    "Chưa xác thực hoặc phiên đăng nhập hết hạn",
                    List.of(new ErrorDetail("authorization", "Vui lòng đăng nhập để thực hiện chức năng này"))
            );
        }

        objectMapper.writeValue(response.getWriter(), body);
    }
}
