package com.example.chat.repository;

import com.example.chat.model.Message;

import java.util.List;

/**
 * 消息仓库接口，负责统一的存取操作。
 */
public interface MessageRepository {
    void save(Message message);

    List<Message> findRecentMessages(String roomId, int limit);

    Message findById(String roomId, String messageId);

    /**
     * Find all messages sent by the specified user across rooms.
     */
    List<Message> findMessagesByUser(String userId);

    /**
     * Find message by global id regardless of room, for scenarios like forward card aggregation.
     */
    Message findById(String messageId);
}
