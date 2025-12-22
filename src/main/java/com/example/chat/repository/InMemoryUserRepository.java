package com.example.chat.repository;

import com.example.chat.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版用户仓库，当前阶段直接使用 ConcurrentMap 存储。
 */
public class InMemoryUserRepository implements UserRepository {
    private final ConcurrentMap<String, User> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(user -> username.equalsIgnoreCase(user.getUsername()))
                .findFirst();
    }

    @Override
    public void save(User user) {
        storage.put(user.getId(), user);
    }

    @Override
    public List<User> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String lower = keyword.toLowerCase();
        List<User> result = new ArrayList<>();
        for (User user : storage.values()) {
            if ((user.getUsername() != null && user.getUsername().toLowerCase().contains(lower)) ||
                    (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(lower))) {
                result.add(user);
            }
        }
        return result;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }
}
