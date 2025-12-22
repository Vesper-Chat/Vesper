package com.example.chat.model;

/**
 * 消息类型，方便后端和前端统一处理展示。
 */
public enum MessageType {
    TEXT,
    EMOJI,
    FILE_LINK,
    DICE_RESULT,
    RPS_RESULT,
    POST_TREE_HOLE,
    FETCH_TREE_HOLE,
    BURN_AFTER_READING,
    FORWARD_CARD,
    ACTION
}
