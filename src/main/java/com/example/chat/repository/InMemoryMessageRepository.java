package com.example.chat.repository;

import com.example.chat.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 消息仓库的内存实现，按房间分组保存。
 */
public class InMemoryMessageRepository implements MessageRepository {
    private final ConcurrentMap<String, List<Message>> messagesByRoom = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Message> messagesById = new ConcurrentHashMap<>();

    @Override
    public void save(Message message) {
        List<Message> messages = messagesByRoom.computeIfAbsent(
                message.getRoomId(),
                key -> Collections.synchronizedList(new ArrayList<>())
        );
        messages.add(message);
        messagesById.put(message.getId(), message);
    }

    @Override
    public List<Message> findRecentMessages(String roomId, int limit) {
        List<Message> messages = messagesByRoom.get(roomId);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(messages.subList(fromIndex, size));
    }

    @Override
    public Message findById(String roomId, String messageId) {
        Message cached = messagesById.get(messageId);
        if (cached != null && cached.getRoomId().equals(roomId)) {
            return cached;
        }
        List<Message> messages = messagesByRoom.get(roomId);
        if (messages == null) {
            return null;
        }
        return messages.stream().filter(m -> m.getId().equals(messageId)).findFirst().orElse(null);
    }

    @Override
    public List<Message> findMessagesByUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        List<Message> results = new ArrayList<>();
        for (List<Message> messages : messagesByRoom.values()) {
            if (messages == null) {
                continue;
            }
            synchronized (messages) {
                for (Message message : messages) {
                    if (message != null && Objects.equals(userId, message.getFromUserId())) {
                        results.add(message);
                    }
                }
            }
        }
        results.sort(Comparator.comparingLong(Message::getTimestamp));
        return results;
    }

    @Override
    public Message findById(String messageId) {
        return messagesById.get(messageId);
    }
}
