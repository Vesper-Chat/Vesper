package com.example.chat.repository;

import com.example.chat.model.FriendRequest;
import com.example.chat.model.FriendRequestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 好友申请仓库接口。
 */
public interface FriendRequestRepository {
    void save(FriendRequest request);

    Optional<FriendRequest> findById(String id);

    Optional<FriendRequest> findPendingBetween(String fromUserId, String toUserId);

    List<FriendRequest> findPendingForUser(String userId);
}
