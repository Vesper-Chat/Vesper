package com.example.chat.repository;

import com.example.chat.model.TreeHoleMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 树洞消息的内存存储实现。
 */
public class InMemoryTreeHoleRepository implements TreeHoleRepository {
    private final List<TreeHoleMessage> messages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void save(TreeHoleMessage message) {
        messages.add(message);
    }

    @Override
    public List<TreeHoleMessage> findRecent(int limit) {
        if (messages.isEmpty()) {
            return List.of();
        }
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(messages.subList(fromIndex, size));
    }
}
