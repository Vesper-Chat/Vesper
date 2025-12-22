package com.example.chat.repository;

import com.example.chat.model.TreeHoleMessage;

import java.util.List;

/**
 * 树洞消息仓库接口。
 */
public interface TreeHoleRepository {
    void save(TreeHoleMessage message);

    List<TreeHoleMessage> findRecent(int limit);
}
