package com.example.chat.repository;

import com.example.chat.model.Moment;

import java.util.List;
import java.util.Optional;

/**
 * 朋友圈动态仓库接口，负责基本的存取。
 */
public interface MomentRepository {
    void save(Moment moment);

    Optional<Moment> findById(String id);

    List<Moment> findByUserIds(List<String> userIds);
}
