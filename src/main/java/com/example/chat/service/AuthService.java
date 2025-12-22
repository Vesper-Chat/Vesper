package com.example.chat.service;

import com.example.chat.config.ServerConfig;
import com.example.chat.model.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.IdGenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 认证业务：注册与登录逻辑。
 */
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 注册
    public User register(String username, String plainPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        userRepository.findByUsername(username)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("用户名已存在");
                });
        String id = IdGenerator.newId();
        String passwordHash = hashPassword(plainPassword);
        User user = new User(id, username, passwordHash);
        user.setAvatarUrl(ServerConfig.DEFAULT_AVATAR);
        userRepository.save(user);
        return user;
    }

    // 登录
    public Optional<User> login(String username, String plainPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getPasswordHash().equals(hashPassword(plainPassword)));
    }

    private String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法初始化密码哈希算法", e);
        }
    }
}
