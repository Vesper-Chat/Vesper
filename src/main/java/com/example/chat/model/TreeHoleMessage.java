package com.example.chat.model;

/**
 * 树洞消息，脱敏的匿名内容，仅包含展示需要的字段。
 */
public class TreeHoleMessage {
    private final String id;
    private final String content;
    private final String senderAlias;
    private final long timestamp;

    public TreeHoleMessage(String id, String content, String senderAlias, long timestamp) {
        this.id = id;
        this.content = content;
        this.senderAlias = senderAlias;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getSenderAlias() {
        return senderAlias;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
