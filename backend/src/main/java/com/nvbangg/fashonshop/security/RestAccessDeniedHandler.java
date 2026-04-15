package com.nvbangg.fashonshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nvbangg.fashonshop.common.dto.ApiResponse;
import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> body = ApiResponse.error(
                "Không có quyền truy cập",
                List.of(new ErrorDetail("role", "Tài khoản của bạn không có quyền thực hiện thao tác này"))
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}
