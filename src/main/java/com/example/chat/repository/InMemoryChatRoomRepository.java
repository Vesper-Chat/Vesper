package com.example.chat.repository;

import com.example.chat.model.ChatRoom;
import com.example.chat.model.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 房间仓库的内存实现。
 */
public class InMemoryChatRoomRepository implements ChatRoomRepository {
    private final ConcurrentMap<String, ChatRoom> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<ChatRoom> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void save(ChatRoom room) {
        storage.put(room.getId(), room);
    }

    @Override
    public List<ChatRoom> findByMemberId(String userId) {
        List<ChatRoom> result = new ArrayList<>();
        for (ChatRoom room : storage.values()) {
            if (room.hasMember(userId)) {
                result.add(room);
            }
        }
        return result;
    }

    @Override
    public Optional<ChatRoom> findPrivateRoom(String userAId, String userBId) {
        return storage.values().stream()
                .filter(room -> room.getType() == RoomType.PRIVATE)
                .filter(room -> room.hasMember(userAId) && room.hasMember(userBId))
                .findFirst();
    }

    @Override
    public List<ChatRoom> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<ChatRoom> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String lower = keyword.toLowerCase();
        List<ChatRoom> result = new ArrayList<>();
        for (ChatRoom room : storage.values()) {
            if (room.getName() != null && room.getName().toLowerCase().contains(lower)) {
                result.add(room);
            }
        }
        return result;
    }
}
