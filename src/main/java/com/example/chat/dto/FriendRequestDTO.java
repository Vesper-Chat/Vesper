package com.example.chat.dto;

import com.example.chat.model.FriendRequest;
import com.example.chat.model.User;

/**
 * DTO used by the frontend to display incoming friend requests.
 */
public class FriendRequestDTO {
    private final String id;
    private final String fromUserId;
    private final String fromDisplayName;
    private final String fromAvatarUrl;
    private final long createdAt;

    public FriendRequestDTO(String id, String fromUserId, String fromDisplayName, String fromAvatarUrl, long createdAt) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.fromDisplayName = fromDisplayName;
        this.fromAvatarUrl = fromAvatarUrl;
        this.createdAt = createdAt;
    }

    public static FriendRequestDTO from(FriendRequest request, User fromUser) {
        String displayName = fromUser != null ? fromUser.getDisplayName() : "";
        String avatar = fromUser != null ? fromUser.getAvatarUrl() : null;
        long createdAt = request.getCreatedAt().toEpochMilli();
        return new FriendRequestDTO(request.getId(), request.getFromUserId(), displayName, avatar, createdAt);
    }

    public String getId() {
        return id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getFromDisplayName() {
        return fromDisplayName;
    }

    public String getFromAvatarUrl() {
        return fromAvatarUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
