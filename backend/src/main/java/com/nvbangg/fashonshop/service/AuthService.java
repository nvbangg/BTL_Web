package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.LoginRequest;
import com.nvbangg.fashonshop.dto.request.RegisterRequest;
import com.nvbangg.fashonshop.dto.response.AuthResponse;
import com.nvbangg.fashonshop.dto.response.LoginUserResponse;
import com.nvbangg.fashonshop.dto.response.RegisterAuthResponse;
import com.nvbangg.fashonshop.dto.response.RegisterResponse;
import com.nvbangg.fashonshop.entity.User;
import com.nvbangg.fashonshop.exception.ConflictException;
import com.nvbangg.fashonshop.exception.UnauthorizedException;
import com.nvbangg.fashonshop.repository.UserRepository;
import com.nvbangg.fashonshop.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterAuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Đăng ký thất bại",
                    List.of(new ErrorDetail("email", "Email này đã tồn tại trong hệ thống")));
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setRole("user");

        User saved = userRepository.save(user);
        RegisterResponse registerUser = new RegisterResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getRole(),
                saved.getCreatedAt()
        );

        String token = jwtService.generateToken(saved);
        return new RegisterAuthResponse(token, registerUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("Đăng nhập thất bại",
                List.of(new ErrorDetail(null, "Sai email hoặc mật khẩu"))));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Đăng nhập thất bại",
                List.of(new ErrorDetail(null, "Sai email hoặc mật khẩu")));
        }

        return toLoginResponse(user);
    }

    private AuthResponse toLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        LoginUserResponse userResponse = new LoginUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        return new AuthResponse(token, userResponse);
    }
}
