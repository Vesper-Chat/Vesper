package com.example.chat.dto;

import com.example.chat.model.Message;
import com.example.chat.model.MessageType;
import com.example.chat.model.User;

/**
 * 消息数据传输对象，包含用户展示信息，方便前端直接渲染。
 */
public class MessageDTO {
    private final String id;
    private final String roomId;
    private final String fromUserId;
    private final String targetUserId;
    private final String fromUserDisplayName;
    private final String fromUserAvatarUrl;
    private final String content;
    private final long timestamp;
    private final MessageType messageType;
    private final Integer burnDelay;
    private final Message.QuoteInfo quote;
    private final boolean recalled;
    private final String recalledBy;

    public MessageDTO(String id, String roomId, String fromUserId, String targetUserId, String fromUserDisplayName,
                      String fromUserAvatarUrl, String content, long timestamp, MessageType messageType,
                      Integer burnDelay, Message.QuoteInfo quote, boolean recalled, String recalledBy) {
        this.id = id;
        this.roomId = roomId;
        this.fromUserId = fromUserId;
        this.targetUserId = targetUserId;
        this.fromUserDisplayName = fromUserDisplayName;
        this.fromUserAvatarUrl = fromUserAvatarUrl;
        this.content = content;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.burnDelay = burnDelay;
        this.quote = quote;
        this.recalled = recalled;
        this.recalledBy = recalledBy;
    }

    public static MessageDTO from(Message message, User fromUser) {
        return new MessageDTO(
                message.getId(),
                message.getRoomId(),
                message.getFromUserId(),
                message.getTargetUserId(),
                fromUser != null ? fromUser.getDisplayName() : "",
                fromUser != null ? fromUser.getAvatarUrl() : null,
                message.getContent(),
                message.getTimestamp(),
                message.getMessageType(),
                message.getBurnDelay(),
                message.getQuote(),
                message.isRecalled(),
                message.getRecalledBy()
        );
    }

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    /**
     * 兼容字段，前端历史逻辑使用 toUser 标识目标用户
     */
    public String getToUser() {
        return targetUserId;
    }

    /**
     * 冗余字段，便于前端通过 targetUser 读取
     */
    public String getTargetUser() {
        return targetUserId;
    }

    public String getFromUserDisplayName() {
        return fromUserDisplayName;
    }

    public String getFromUserAvatarUrl() {
        return fromUserAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Integer getBurnDelay() {
        return burnDelay;
    }

    public Message.QuoteInfo getQuote() {
        return quote;
    }

    public boolean isRecalled() {
        return recalled;
    }

    public String getRecalledBy() {
        return recalledBy;
    }
}
