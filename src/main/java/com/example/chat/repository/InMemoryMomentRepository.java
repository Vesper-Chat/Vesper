package com.example.chat.repository;

import com.example.chat.model.Moment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 朋友圈动态的内存实现，按用户分桶存储。
 */
public class InMemoryMomentRepository implements MomentRepository {
    private final ConcurrentMap<String, Moment> momentsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<Moment>> momentsByUser = new ConcurrentHashMap<>();

    @Override
    public void save(Moment moment) {
        momentsById.put(moment.getId(), moment);
        List<Moment> list = momentsByUser.computeIfAbsent(
                moment.getUserId(),
                key -> Collections.synchronizedList(new ArrayList<>())
        );
        if (!list.contains(moment)) {
            list.add(moment);
        }
    }

    @Override
    public Optional<Moment> findById(String id) {
        return Optional.ofNullable(momentsById.get(id));
    }

    @Override
    public List<Moment> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Moment> results = new ArrayList<>();
        for (String userId : userIds) {
            List<Moment> list = momentsByUser.get(userId);
            if (list != null && !list.isEmpty()) {
                results.addAll(list);
            }
        }
        return results;
    }
}
