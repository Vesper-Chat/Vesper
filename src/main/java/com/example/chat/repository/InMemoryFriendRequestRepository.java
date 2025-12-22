package com.example.chat.repository;

import com.example.chat.model.FriendRequest;
import com.example.chat.model.FriendRequestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版好友申请仓库。
 */
public class InMemoryFriendRequestRepository implements FriendRequestRepository {
    private final ConcurrentMap<String, FriendRequest> storage = new ConcurrentHashMap<>();

    @Override
    public void save(FriendRequest request) {
        storage.put(request.getId(), request);
    }

    @Override
    public Optional<FriendRequest> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<FriendRequest> findPendingBetween(String fromUserId, String toUserId) {
        return storage.values().stream()
                .filter(req -> req.getStatus() == FriendRequestStatus.PENDING)
                .filter(req -> req.getFromUserId().equals(fromUserId) && req.getToUserId().equals(toUserId))
                .findFirst();
    }

    @Override
    public List<FriendRequest> findPendingForUser(String userId) {
        List<FriendRequest> result = new ArrayList<>();
        for (FriendRequest request : storage.values()) {
            if (request.getToUserId().equals(userId) && request.getStatus() == FriendRequestStatus.PENDING) {
                result.add(request);
            }
        }
        return result;
    }
}
