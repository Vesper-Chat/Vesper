package com.example.chat.service;

import com.example.chat.dto.FriendRequestDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.model.FriendRequest;
import com.example.chat.model.FriendRequestStatus;
import com.example.chat.model.User;
import com.example.chat.config.ServerConfig;
import com.example.chat.repository.FriendRequestRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.IdGenerator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 好友相关业务：搜索、申请、审批、列表
 */
public class FriendService {
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserService userService;

    public FriendService(UserRepository userRepository, FriendRequestRepository friendRequestRepository, UserService userService) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userService = userService;
    }

    public List<UserDTO> searchFriends(String keyword) {
        return userRepository.searchByKeyword(keyword).stream().map(userService::toDTO).collect(Collectors.toList());
    }

    // 发送好友申请，等待对方确认。
    public void sendFriendRequest(String fromUserId, String toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不存在"));

        if (fromUser.isFriend(toUserId)) {
            throw new IllegalStateException("已经是好友了");
        }
        // AI 类型用户无需人工审核，直接连结好友
        if (isAiUser(toUser)) {
            linkFriends(fromUserId, toUserId);
            return;
        }
        if (friendRequestRepository.findPendingBetween(fromUserId, toUserId).isPresent()) {
            throw new IllegalStateException("已发送申请，请等待对方处理");
        }
        friendRequestRepository.findPendingBetween(toUserId, fromUserId)
                .ifPresent(request -> {
                    throw new IllegalStateException("对方已有待处理申请，请到申请列表处理");
                });

        FriendRequest request = new FriendRequest(IdGenerator.newId(), fromUserId, toUserId);
        friendRequestRepository.save(request);
    }

    public List<FriendRequestDTO> listIncomingRequests(String userId) {
        return friendRequestRepository.findPendingForUser(userId).stream()
                .map(request -> FriendRequestDTO.from(request,
                        userRepository.findById(request.getFromUserId()).orElse(null)))
                .collect(Collectors.toList());
    }

    public FriendRequest respondToRequest(String userId, String requestId, boolean accepted) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申请不存在"));
        if (!request.getToUserId().equals(userId)) {
            throw new IllegalStateException("无权处理该申请");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("申请已处理");
        }
        request.setStatus(accepted ? FriendRequestStatus.ACCEPTED : FriendRequestStatus.REJECTED);
        friendRequestRepository.save(request);
        if (accepted) {
            linkFriends(request.getFromUserId(), request.getToUserId());
        }
        return request;
    }

    // 构建好友
    public void linkFriends(String userAId, String userBId) {
        User userA = userRepository.findById(userAId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        User userB = userRepository.findById(userBId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!userA.isFriend(userBId)) {
            userA.addFriend(userBId);
            userRepository.save(userA);
        }
        if (!userB.isFriend(userAId)) {
            userB.addFriend(userAId);
            userRepository.save(userB);
        }
    }

    // 好友列表
    public List<UserDTO> listFriends(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        List<UserDTO> friends = user.getFriendIds().stream()
                .map(userService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(userService::toDTO)
                .collect(Collectors.toList());
        // 确保 AI 助手出现在好友列表中（无需申请流程）。
        boolean hasBot = friends.stream().anyMatch(dto -> ServerConfig.AI_BOT_ID.equals(dto.getId()));
        if (!hasBot) {
            userRepository.findById(ServerConfig.AI_BOT_ID).ifPresent(bot -> friends.add(userService.toDTO(bot)));
        }
        return friends;
    }

    private boolean isAiUser(User user) {
        if (user == null) {
            return false;
        }
        if (ServerConfig.AI_BOT_ID.equals(user.getId())) {
            return true;
        }
        return user.isAiClone() || (user.getId() != null && user.getId().startsWith("ai_"));
    }
}
