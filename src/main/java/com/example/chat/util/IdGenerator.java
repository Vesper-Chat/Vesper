package com.example.chat.util;

import java.util.UUID;

/**
 * ID 生成工具
 */
public final class IdGenerator {
    private IdGenerator() {
    }

    /**
     * 生成一个随机 ID，直接复用 UUID。
     */
    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
