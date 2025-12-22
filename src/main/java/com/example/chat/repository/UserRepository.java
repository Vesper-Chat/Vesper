package com.example.chat.repository;

import com.example.chat.model.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓库接口，后期可切换为数据库实现。
 */
public interface UserRepository {
    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    void save(User user);

    List<User> searchByKeyword(String keyword);

    List<User> findAll();
}
