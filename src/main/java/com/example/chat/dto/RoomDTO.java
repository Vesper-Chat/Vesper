package com.example.chat.dto;

import com.example.chat.model.ChatRoom;
import com.example.chat.model.RoomType;

import java.util.Set;

/**
 * 房间数据传输对象，减少前端需要解析的字段数量。
 */
public class RoomDTO {
    private final String id;
    private final String name;
    private final RoomType type;
    private final Set<String> memberIds;
    private final String ownerId;
    private final String description;
    private final String avatarUrl;
    private final boolean allowPublicJoin;

    public RoomDTO(String id, String name, RoomType type, Set<String> memberIds,
                   String ownerId, String description, String avatarUrl, boolean allowPublicJoin) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.memberIds = memberIds;
        this.ownerId = ownerId;
        this.description = description;
        this.avatarUrl = avatarUrl;
        this.allowPublicJoin = allowPublicJoin;
    }

    public static RoomDTO from(ChatRoom room) {
        return new RoomDTO(room.getId(), room.getName(), room.getType(), room.getMemberIds(),
                room.getOwnerId(), room.getDescription(), room.getAvatarUrl(), room.isAllowPublicJoin());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RoomType getType() {
        return type;
    }

    public Set<String> getMemberIds() {
        return memberIds;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getDescription() {
        return description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isAllowPublicJoin() {
        return allowPublicJoin;
    }
}
