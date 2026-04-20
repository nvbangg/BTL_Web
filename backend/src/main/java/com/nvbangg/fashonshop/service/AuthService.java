package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.LoginRequest;
import com.nvbangg.fashonshop.dto.request.RegisterRequest;
import com.nvbangg.fashonshop.dto.response.LoginResponse;
import com.nvbangg.fashonshop.dto.response.RegisterResponse;
import com.nvbangg.fashonshop.entity.User;
import com.nvbangg.fashonshop.entity.UserRole;
import com.nvbangg.fashonshop.exception.ConflictException;
import com.nvbangg.fashonshop.exception.UnauthorizedException;
import com.nvbangg.fashonshop.repository.UserRepository;
import com.nvbangg.fashonshop.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Đăng ký thất bại",
                    List.of(new ErrorDetail("email", "Email này đã tồn tại trong hệ thống")));
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setRole(UserRole.user);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);
        return new RegisterResponse(
                token,
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getRole().name().toLowerCase(Locale.ROOT),
                saved.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Đăng nhập thất bại",
                        List.of(new ErrorDetail(null, "Sai email hoặc mật khẩu"))));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Đăng nhập thất bại",
                    List.of(new ErrorDetail(null, "Sai email hoặc mật khẩu")));
        }

        return toLoginResponse(user);
    }

    private LoginResponse toLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name().toLowerCase(Locale.ROOT)
        );
    }
}
