package com.example.chat.dto;

import com.example.chat.model.TreeHoleMessage;

/**
 * 树洞消息的传输对象。
 */
public class TreeHoleMessageDTO {
    private final String id;
    private final String content;
    private final String senderAlias;
    private final long timestamp;

    public TreeHoleMessageDTO(String id, String content, String senderAlias, long timestamp) {
        this.id = id;
        this.content = content;
        this.senderAlias = senderAlias;
        this.timestamp = timestamp;
    }

    public static TreeHoleMessageDTO from(TreeHoleMessage message) {
        return new TreeHoleMessageDTO(
                message.getId(),
                message.getContent(),
                message.getSenderAlias(),
                message.getTimestamp()
        );
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
