package com.example.chat.model;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户实体，封装用户的基础资料及好友关系。
 */
public class User {
    private final String id;
    private final String username;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String signature;
    private volatile UserStatus status = UserStatus.OFFLINE;
    private final Set<String> friendIds = ConcurrentHashMap.newKeySet();
    private boolean aiClone;
    private String ownerUserId;
    private String prompt;

    public User(String id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = username;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Set<String> getFriendIds() {
        return Collections.unmodifiableSet(friendIds);
    }

    public void addFriend(String userId) {
        friendIds.add(userId);
    }

    public void removeFriend(String userId) {
        friendIds.remove(userId);
    }

    public boolean isFriend(String userId) {
        return friendIds.contains(userId);
    }

    public boolean isAiClone() {
        return aiClone;
    }

    public void setAiClone(boolean aiClone) {
        this.aiClone = aiClone;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
