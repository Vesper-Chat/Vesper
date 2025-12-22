package com.example.chat.service;

import com.example.chat.dto.RoomDTO;
import com.example.chat.model.ChatRoom;
import com.example.chat.model.RoomType;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.IdGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 房间相关业务：创建、查询、成员校验。
 */
public class RoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public RoomService(ChatRoomRepository chatRoomRepository, UserRepository userRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
    }

    // 私聊get或者create一个
    public ChatRoom getOrCreatePrivateRoom(String userAId, String userBId) {
        return chatRoomRepository.findPrivateRoom(userAId, userBId)
                .orElseGet(() -> {
                    ChatRoom room = new ChatRoom(IdGenerator.newId(), buildPrivateRoomName(userAId, userBId), RoomType.PRIVATE);
                    room.addMember(userAId);
                    room.addMember(userBId);
                    chatRoomRepository.save(room);
                    return room;
                });
    }

    public ChatRoom createGroupRoom(String ownerId, String name, List<String> memberIds) {
        return createGroupRoom(ownerId, name, memberIds, null, null, true);
    }

    public ChatRoom createGroupRoom(String ownerId, String name, List<String> memberIds,
                                    String description, String avatarUrl, boolean allowPublicJoin) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("房间名称不能为空");
        }
        ChatRoom room = new ChatRoom(IdGenerator.newId(), name, RoomType.GROUP);
        room.setOwnerId(ownerId);
        room.setDescription(description);
        room.setAvatarUrl(avatarUrl);
        room.setAllowPublicJoin(allowPublicJoin);
        Set<String> uniqueMembers = new HashSet<>(memberIds);
        uniqueMembers.add(ownerId);
        for (String memberId : uniqueMembers) {
            ensureUserExists(memberId);
            room.addMember(memberId);
        }
        chatRoomRepository.save(room);
        return room;
    }

    public List<RoomDTO> listRoomsForUser(String userId) {
        return chatRoomRepository.findByMemberId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ChatRoom getRoom(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
    }

    public List<RoomDTO> searchRooms(String keyword, String requesterId) {
        return chatRoomRepository.searchByName(keyword).stream()
                .filter(room -> room.getType() == RoomType.GROUP)
                .filter(room -> room.isAllowPublicJoin() || room.hasMember(requesterId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ChatRoom joinRoom(String roomId, String userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
        ensureUserExists(userId);
        if (room.getType() == RoomType.GROUP && !room.isAllowPublicJoin() && !room.hasMember(userId)) {
            throw new IllegalStateException("该群不允许搜索加入，请联系群主邀请");
        }
        room.addMember(userId);
        chatRoomRepository.save(room);
        return room;
    }

    public ChatRoom updateGroupInfo(String requesterId, String roomId, String name,
                                    String description, String avatarUrl, Boolean allowPublicJoin) {
        ChatRoom room = getRoom(roomId);
        ensureGroupOwner(requesterId, room);
        if (name != null && !name.isBlank()) {
            room.setName(name.trim());
        }
        if (description != null) {
            room.setDescription(description.trim());
        }
        if (avatarUrl != null) {
            room.setAvatarUrl(avatarUrl.trim());
        }
        if (allowPublicJoin != null) {
            room.setAllowPublicJoin(allowPublicJoin);
        }
        chatRoomRepository.save(room);
        return room;
    }

    public ChatRoom addMembers(String requesterId, String roomId, List<String> memberIds) {
        ChatRoom room = getRoom(roomId);
        ensureGroupOwner(requesterId, room);
        for (String memberId : memberIds) {
            ensureUserExists(memberId);
            ensureFriend(requesterId, memberId);
            room.addMember(memberId);
        }
        chatRoomRepository.save(room);
        return room;
    }

    public ChatRoom removeMember(String requesterId, String roomId, String targetUserId) {
        ChatRoom room = getRoom(roomId);
        ensureGroupOwner(requesterId, room);
        if (room.getOwnerId() != null && room.getOwnerId().equals(targetUserId)) {
            throw new IllegalStateException("不能移除群主");
        }
        room.removeMember(targetUserId);
        chatRoomRepository.save(room);
        return room;
    }

    public RoomDTO toDTO(ChatRoom room) {
        return RoomDTO.from(room);
    }

    public void saveRoom(ChatRoom room) {
        chatRoomRepository.save(room);
    }

    public void ensureMember(String roomId, String userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
        if (!room.hasMember(userId)) {
            throw new IllegalStateException("用户不在该房间中");
        }
    }

    private void ensureUserExists(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    }

    private void ensureGroupOwner(String requesterId, ChatRoom room) {
        if (room.getType() != RoomType.GROUP) {
            throw new IllegalStateException("仅群聊支持该操作");
        }
        if (room.getOwnerId() == null || !room.getOwnerId().equals(requesterId)) {
            throw new IllegalStateException("只有群主可以管理群聊");
        }
    }

    private void ensureFriend(String requesterId, String targetUserId) {
        userRepository.findById(requesterId).ifPresentOrElse(owner -> {
            if (!owner.isFriend(targetUserId)) {
                throw new IllegalStateException("只能添加自己的好友进群");
            }
        }, () -> {
            throw new IllegalArgumentException("用户不存在: " + requesterId);
        });
    }

    private String buildPrivateRoomName(String userAId, String userBId) {
        List<String> names = new ArrayList<>();
        userRepository.findById(userAId).ifPresent(user -> names.add(user.getDisplayName()));
        userRepository.findById(userBId).ifPresent(user -> names.add(user.getDisplayName()));
        return names.isEmpty() ? "私聊" : String.join(" & ", names);
    }
}
