package com.example.chat.dto;

import com.example.chat.model.User;

/**
 * AI 分身的展示对象，前端使用它来渲染列表和标记归属关系。
 */
public class AiCloneDTO {
    private final String id;
    private final String displayName;
    private final String avatarUrl;
    private final String signature;
    private final String ownerUserId;
    private final String ownerDisplayName;
    private final boolean ownedByCurrentUser;
    private final boolean friend;
    private final String prompt;

    public AiCloneDTO(String id,
                      String displayName,
                      String avatarUrl,
                      String signature,
                      String ownerUserId,
                      String ownerDisplayName,
                      boolean ownedByCurrentUser,
                      boolean friend,
                      String prompt) {
        this.id = id;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.signature = signature;
        this.ownerUserId = ownerUserId;
        this.ownerDisplayName = ownerDisplayName;
        this.ownedByCurrentUser = ownedByCurrentUser;
        this.friend = friend;
        this.prompt = prompt;
    }

    public static AiCloneDTO from(User clone, User owner, String currentUserId, boolean isFriend) {
        boolean ownedByMe = currentUserId != null && currentUserId.equals(clone.getOwnerUserId());
        String prompt = ownedByMe ? clone.getPrompt() : null;
        String ownerName = owner == null ? "" : (owner.getDisplayName() != null ? owner.getDisplayName() : owner.getUsername());
        return new AiCloneDTO(
                clone.getId(),
                clone.getDisplayName(),
                clone.getAvatarUrl(),
                clone.getSignature(),
                clone.getOwnerUserId(),
                ownerName,
                ownedByMe,
                isFriend,
                prompt
        );
    }

    public String getId() {
        return id;
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

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public boolean isOwnedByCurrentUser() {
        return ownedByCurrentUser;
    }

    public boolean isFriend() {
        return friend;
    }

    public String getPrompt() {
        return prompt;
    }
}
