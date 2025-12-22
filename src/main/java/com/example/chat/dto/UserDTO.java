package com.example.chat.dto;

import com.example.chat.model.User;
import com.example.chat.model.UserStatus;

/**
 * 面向前端展示的用户数据传输对象。
 */
public class UserDTO {
    private final String id;
    private final String username;
    private final String displayName;
    private final String avatarUrl;
    private final String signature;
    private final UserStatus status;

    public UserDTO(String id, String username, String displayName, String avatarUrl, String signature, UserStatus status) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.signature = signature;
        this.status = status;
    }

    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getSignature(),
                user.getStatus()
        );
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getSignature() {
        return signature;
    }

    public UserStatus getStatus() {
        return status;
    }
}
