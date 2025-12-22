package com.example.chat.model;

import java.time.Instant;

/**
 * 好友申请实体，尽管当前流程简化，保留该对象以体现 OOP 设计。
 */
public class FriendRequest {
    private final String id;
    private final String fromUserId;
    private final String toUserId;
    private final Instant createdAt;
    private FriendRequestStatus status;

    public FriendRequest(String id, String fromUserId, String toUserId) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.createdAt = Instant.now();
        this.status = FriendRequestStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }
}
