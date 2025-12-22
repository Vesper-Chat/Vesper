package com.example.chat.model;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 聊天房间实体，负责维护成员列表和房间属性。
 */
public class ChatRoom {
    private final String id;
    private String name;
    private final RoomType type;
    private final Set<String> memberIds = ConcurrentHashMap.newKeySet();
    private final AtomicInteger aiReplyStreak = new AtomicInteger(0);
    private String ownerId;
    private String description;
    private String avatarUrl;
    private boolean allowPublicJoin = true;

    public ChatRoom(String id, String name, RoomType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoomType getType() {
        return type;
    }

    public Set<String> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    public void addMember(String userId) {
        memberIds.add(userId);
    }

    public void removeMember(String userId) {
        memberIds.remove(userId);
    }

    public boolean hasMember(String userId) {
        return memberIds.contains(userId);
    }

    public int getAiReplyStreak() {
        return aiReplyStreak.get();
    }

    public void resetAiReplyStreak() {
        aiReplyStreak.set(0);
    }

    public int incrementAiReplyStreak() {
        return aiReplyStreak.incrementAndGet();
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isAllowPublicJoin() {
        return allowPublicJoin;
    }

    public void setAllowPublicJoin(boolean allowPublicJoin) {
        this.allowPublicJoin = allowPublicJoin;
    }
}
