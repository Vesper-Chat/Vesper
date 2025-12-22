package com.example.chat.service;

import com.example.chat.dto.MomentDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.model.Moment;
import com.example.chat.model.User;
import com.example.chat.repository.MomentRepository;
import com.example.chat.util.IdGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 朋友圈：发布、点赞、评论、时间线。
 */
public class MomentService {
    private final MomentRepository momentRepository;
    private final FriendService friendService;
    private final UserService userService;

    public MomentService(MomentRepository momentRepository, FriendService friendService, UserService userService) {
        this.momentRepository = momentRepository;
        this.friendService = friendService;
        this.userService = userService;
    }

    // 发布
    public MomentDTO post(String userId, String content, List<String> imageUrls) {
        List<String> safeImages = sanitizeImages(imageUrls);
        if ((content == null || content.isBlank()) && safeImages.isEmpty()) {
            throw new IllegalArgumentException("动态内容或图片至少填写一项");
        }
        Moment moment = new Moment(
                IdGenerator.newId(),
                userId,
                content == null ? "" : content.trim(),
                safeImages,
                System.currentTimeMillis()
        );
        momentRepository.save(moment);
        return toDTO(moment, userId);
    }

    public List<MomentDTO> listTimeline(String userId) {
        List<String> related = collectUserAndFriends(userId);
        List<Moment> moments = momentRepository.findByUserIds(related);
        return moments.stream()
                .sorted(Comparator.comparingLong(Moment::getTimestamp).reversed())
                .map(moment -> toDTO(moment, userId))
                .collect(Collectors.toList());
    }

    public MomentDTO toggleLike(String userId, String momentId) {
        Moment moment = getMomentOrThrow(momentId);
        Set<String> likes = moment.getLikes();
        if (!likes.add(userId)) {
            likes.remove(userId);
        }
        momentRepository.save(moment);
        return toDTO(moment, userId);
    }

    public MomentDTO comment(String userId, String momentId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论不能为空");
        }
        Moment moment = getMomentOrThrow(momentId);
        moment.addComment(new Moment.Comment(userId, content.trim(), System.currentTimeMillis()));
        momentRepository.save(moment);
        return toDTO(moment, userId);
    }

    public MomentDTO find(String momentId, String currentUserId) {
        return momentRepository.findById(momentId)
                .map(moment -> toDTO(moment, currentUserId))
                .orElse(null);
    }

    private List<String> sanitizeImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .distinct()
                .limit(9)
                .collect(Collectors.toList());
        return cleaned;
    }

    public List<String> collectUserAndFriends(String userId) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(userId);
        List<UserDTO> friends = friendService.listFriends(userId);
        for (UserDTO friend : friends) {
            ids.add(friend.getId());
        }
        return new ArrayList<>(ids);
    }

    private Moment getMomentOrThrow(String momentId) {
        return momentRepository.findById(momentId)
                .orElseThrow(() -> new IllegalArgumentException("动态不存在"));
    }

    private MomentDTO toDTO(Moment moment, String currentUserId) {
        Function<String, User> resolver = id -> userService.findById(id).orElse(null);
        User owner = resolver.apply(moment.getUserId());
        return MomentDTO.from(moment, owner, resolver, currentUserId);
    }
}
