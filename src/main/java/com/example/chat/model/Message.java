package com.example.chat.model;

/**
 * 消息实体，记录房间、发送者、内容及类型。
 */
public class Message {
    private final String id;
    private final String roomId;
    private final String fromUserId;
    private final String targetUserId;
    private final String content;
    private final long timestamp;
    private final MessageType messageType;
    private final Integer burnDelay;
    private final QuoteInfo quote;
    private boolean recalled;
    private String recalledBy;

    public Message(String id,
                   String roomId,
                   String fromUserId,
                   String targetUserId,
                   String content,
                   long timestamp,
                   MessageType messageType,
                   Integer burnDelay,
                   QuoteInfo quote) {
        this.id = id;
        this.roomId = roomId;
        this.fromUserId = fromUserId;
        this.targetUserId = targetUserId;
        this.content = content;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.burnDelay = burnDelay;
        this.quote = quote;
        this.recalled = false;
        this.recalledBy = null;
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

    public QuoteInfo getQuote() {
        return quote;
    }

    public boolean isRecalled() {
        return recalled;
    }

    public void setRecalled(boolean recalled) {
        this.recalled = recalled;
    }

    public String getRecalledBy() {
        return recalledBy;
    }

    public void setRecalledBy(String recalledBy) {
        this.recalledBy = recalledBy;
    }

    public static class QuoteInfo {
        private final String messageId;
        private final String roomId;
        private final String sender;
        private final String content;

        public QuoteInfo(String messageId, String roomId, String sender, String content) {
            this.messageId = messageId;
            this.roomId = roomId;
            this.sender = sender;
            this.content = content;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getSender() {
            return sender;
        }

        public String getContent() {
            return content;
        }
    }
}
