package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.dto.request.ChangePasswordRequest;
import com.nvbangg.fashonshop.dto.request.UpdateProfileRequest;
import com.nvbangg.fashonshop.dto.response.UserResponse;
import com.nvbangg.fashonshop.entity.User;
import com.nvbangg.fashonshop.exception.BadRequestException;
import com.nvbangg.fashonshop.exception.UnauthorizedException;
import com.nvbangg.fashonshop.repository.UserRepository;
import com.nvbangg.fashonshop.security.AuthUser;
import com.nvbangg.fashonshop.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getMyProfile() {
        User user = getCurrentUserEntity();
        return toUserResponse(user);
    }

    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();

        user.setName(request.getName().trim());
        user.setPhone(request.getPhone().trim());
        user.setAddress(request.getAddress().trim());

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("currentPassword", "Mật khẩu hiện tại không chính xác")));
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("Dữ liệu không hợp lệ",
                    List.of(new ErrorDetail("newPassword", "Mật khẩu mới không được trùng với mật khẩu cũ và phải >= 6 ký tự")));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User getCurrentUserEntity() {
        AuthUser currentUser = SecurityUtils.getCurrentUser();
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Chưa xác thực hoặc phiên đăng nhập hết hạn"));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAddress(),
                user.getRole().name().toLowerCase(Locale.ROOT)
        );
    }
}
