package com.example.fashonshop.service;

import com.example.fashonshop.dto.user.ChangePasswordRequest;
import com.example.fashonshop.dto.user.UserMeResponse;
import com.example.fashonshop.entity.User;
import com.example.fashonshop.exception.BadRequestException;
import com.example.fashonshop.exception.NotFoundException;
import com.example.fashonshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserMeResponse getMe(Long userId) {
        User user = getUser(userId);
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAddress(),
                user.getRole()
        );
    }

    @Transactional
    public String changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("CURRENT_PASSWORD_INCORRECT", "Current password is incorrect");
        }

        if (request.newPassword().length() < 6) {
            throw new BadRequestException("INVALID_PASSWORD", "New password must be at least 6 characters");
        }

        if (request.newPassword().equals(request.currentPassword())) {
            throw new BadRequestException("INVALID_PASSWORD", "New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return "Password updated successfully";
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }
}
