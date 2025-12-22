package com.example.chat.service;

import com.example.chat.dto.UserDTO;
import com.example.chat.model.User;
import com.example.chat.model.UserStatus;
import com.example.chat.repository.UserRepository;

import java.util.Optional;

/**
 * 用户相关的业务逻辑，例如资料更新、DTO 转换等。
 */
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public UserDTO toDTO(User user) {
        return UserDTO.from(user);
    }

    public UserDTO getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toDTO(user);
    }

    public UserDTO updateProfile(String userId, String displayName, String avatarUrl, String signature) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName);
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        if (signature != null) {
            user.setSignature(signature);
        }
        userRepository.save(user);
        return toDTO(user);
    }

    public void updateStatus(String userId, UserStatus status) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }
}
