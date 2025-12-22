package com.example.chat.repository;

import com.example.chat.model.ChatRoom;

import java.util.List;
import java.util.Optional;

/**
 * 房间仓库接口，后续可扩展数据库实现。
 */
public interface ChatRoomRepository {
    Optional<ChatRoom> findById(String id);

    void save(ChatRoom room);

    List<ChatRoom> findByMemberId(String userId);

    Optional<ChatRoom> findPrivateRoom(String userAId, String userBId);

    List<ChatRoom> findAll();

    List<ChatRoom> searchByName(String keyword);
}
