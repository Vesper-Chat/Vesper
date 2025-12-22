package com.example.chat.service;

import com.example.chat.dto.MessageDTO;
import com.example.chat.model.ChatRoom;
import com.example.chat.model.Message;
import com.example.chat.model.MessageType;
import com.example.chat.model.User;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 聊天逻辑
 */
public class ChatService {
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public ChatService(MessageRepository messageRepository, ChatRoomRepository chatRoomRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
    }

    // 方便调用
    public MessageDTO sendMessage(String roomId, String fromUserId, String targetUserId, String content, MessageType messageType) {
        return sendMessage(roomId, fromUserId, targetUserId, content, messageType, null, null);
    }

    public MessageDTO sendMessage(String roomId, String fromUserId, String targetUserId, String content, MessageType messageType, Integer burnDelay) {
        return sendMessage(roomId, fromUserId, targetUserId, content, messageType, burnDelay, null);
    }

    public MessageDTO sendMessage(String roomId, String fromUserId, String content, MessageType messageType) {
        return sendMessage(roomId, fromUserId, null, content, messageType, null, null);
    }

    public MessageDTO sendMessage(String roomId, String fromUserId, String content, MessageType messageType, Integer burnDelay) {
        return sendMessage(roomId, fromUserId, null, content, messageType, burnDelay, null);
    }

    // 发送消息逻辑
    public MessageDTO sendMessage(String roomId, String fromUserId, String targetUserId, String content, MessageType messageType, Integer burnDelay, Message.QuoteInfo quote) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
        if (!room.hasMember(fromUserId)) {
            throw new IllegalStateException("用户不在该房间中");
        }
        Integer finalBurnDelay = messageType == MessageType.BURN_AFTER_READING
                ? ((burnDelay == null || burnDelay <= 0) ? 10 : burnDelay): null;
        Message message = new Message(
                IdGenerator.newId(),
                roomId,
                fromUserId,
                targetUserId,
                content,
                System.currentTimeMillis(),
                messageType == null ? MessageType.TEXT : messageType,
                finalBurnDelay,
                quote
        );
        messageRepository.save(message);
        User sender = userRepository.findById(fromUserId).orElse(null);
        return MessageDTO.from(message, sender);
    }

    //进入房间时加载历史消息
    public List<MessageDTO> getRecentMessages(String roomId, int limit) {
        return messageRepository.findRecentMessages(roomId, limit).stream()
                .filter(message -> !isBurnExpired(message))
                .map(message -> MessageDTO.from(message, userRepository.findById(message.getFromUserId()).orElse(null)))
                .collect(Collectors.toList());
    }

    //查出该用户发过的所有消息，用于构建分身
    public List<Message> findMessagesByUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        return messageRepository.findMessagesByUser(userId).stream()
                .filter(message -> !isBurnExpired(message))
                .filter(message -> !message.isRecalled())
                .collect(Collectors.toList());
    }

    public MessageDTO sendGameResult(String roomId, String fromUserId, String gameType) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
        if (!room.hasMember(fromUserId)) {
            throw new IllegalStateException("用户不在该房间中");
        }
        String content;
        MessageType type;
        if ("DICE".equalsIgnoreCase(gameType)) {
            int dice = ThreadLocalRandom.current().nextInt(1, 7);
            content = String.valueOf(dice);
            type = MessageType.DICE_RESULT;
        } else if ("RPS".equalsIgnoreCase(gameType)) {
            String[] rps = {"ROCK", "PAPER", "SCISSORS"};
            content = rps[ThreadLocalRandom.current().nextInt(rps.length)];
            type = MessageType.RPS_RESULT;
        } else {
            throw new IllegalArgumentException("未知游戏类型");
        }
        Message message = new Message(
                IdGenerator.newId(),
                roomId,
                fromUserId,
                null,
                content,
                System.currentTimeMillis(),
                type,
                null,
                null
        );
        messageRepository.save(message);
        User sender = userRepository.findById(fromUserId).orElse(null);
        return MessageDTO.from(message, sender);
    }

    // 撤回
    public MessageDTO recallMessage(String roomId, String messageId, String requesterId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
        if (!room.hasMember(requesterId)) {
            throw new IllegalStateException("用户不在该房间中");
        }
        Message message = messageRepository.findById(roomId, messageId);
        if (message == null) {
            throw new IllegalArgumentException("消息不存在");
        }
        if (!message.getFromUserId().equals(requesterId)) {
            throw new IllegalStateException("只能撤回自己的消息");
        }
        long now = System.currentTimeMillis();
        if (now - message.getTimestamp() > 2 * 60 * 1000L) {
            throw new IllegalStateException("只能撤回两分钟内的消息");
        }
        message.setRecalled(true);
        message.setRecalledBy(requesterId);
        User sender = userRepository.findById(message.getFromUserId()).orElse(null);
        return MessageDTO.from(message, sender);
    }

    public Message findMessageById(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return null;
        }
        return messageRepository.findById(messageId);
    }

    public List<Message> findMessagesByIds(List<String> messageIds) {
        List<Message> results = new ArrayList<>();
        if (messageIds == null) {
            return results;
        }
        for (String id : messageIds) {
            Message message = findMessageById(id);
            if (message != null) {
                results.add(message);
            }
        }
        return results;
    }

    private boolean isBurnExpired(Message message) {
        if (message == null || message.getMessageType() != MessageType.BURN_AFTER_READING) {
            return false;
        }
        Integer delay = message.getBurnDelay();
        int effectiveDelay = (delay == null || delay <= 0) ? 10 : delay;
        long expireAt = message.getTimestamp() + effectiveDelay * 1000L;
        return System.currentTimeMillis() >= expireAt;
    }
}
