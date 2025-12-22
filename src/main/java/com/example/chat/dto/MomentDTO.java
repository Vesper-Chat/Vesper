package com.example.chat.dto;

import com.example.chat.model.Moment;
import com.example.chat.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 朋友圈动态的传输对象，包含发布者信息、点赞状态和评论。
 */
public class MomentDTO {
    private final String id;
    private final String userId;
    private final String userDisplayName;
    private final String userAvatarUrl;
    private final String content;
    private final long timestamp;
    private final List<String> imageUrls;
    private final Set<String> likes;
    private final boolean likedByMe;
    private final List<CommentDTO> comments;

    public MomentDTO(String id,
                     String userId,
                     String userDisplayName,
                     String userAvatarUrl,
                     String content,
                     List<String> imageUrls,
                     long timestamp,
                     Set<String> likes,
                     boolean likedByMe,
                     List<CommentDTO> comments) {
        this.id = id;
        this.userId = userId;
        this.userDisplayName = userDisplayName;
        this.userAvatarUrl = userAvatarUrl;
        this.content = content;
        this.imageUrls = imageUrls;
        this.timestamp = timestamp;
        this.likes = likes;
        this.likedByMe = likedByMe;
        this.comments = comments;
    }

    public static MomentDTO from(Moment moment, User owner, Function<String, User> userResolver, String currentUserId) {
        if (moment == null) {
            return null;
        }
        User author = owner;
        if (author == null && userResolver != null) {
            author = userResolver.apply(moment.getUserId());
        }
        List<Moment.Comment> rawComments = new ArrayList<>(moment.getComments());
        List<CommentDTO> commentDTOs = rawComments.stream()
                .map(comment -> {
                    User commenter = userResolver == null ? null : userResolver.apply(comment.getUserId());
                    return CommentDTO.from(comment, commenter);
                })
                .collect(Collectors.toList());
        boolean liked = currentUserId != null && moment.getLikes().contains(currentUserId);
        return new MomentDTO(
                moment.getId(),
                moment.getUserId(),
                author != null ? author.getDisplayName() : "",
                author != null ? author.getAvatarUrl() : null,
                moment.getContent(),
                moment.getImageUrls(),
                moment.getTimestamp(),
                Set.copyOf(moment.getLikes()),
                liked,
                commentDTOs
        );
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Set<String> getLikes() {
        return likes;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public List<CommentDTO> getComments() {
        return comments;
    }

    public static class CommentDTO {
        private final String userId;
        private final String userDisplayName;
        private final String userAvatarUrl;
        private final String content;
        private final long timestamp;

        public CommentDTO(String userId, String userDisplayName, String userAvatarUrl, String content, long timestamp) {
            this.userId = userId;
            this.userDisplayName = userDisplayName;
            this.userAvatarUrl = userAvatarUrl;
            this.content = content;
            this.timestamp = timestamp;
        }

        public static CommentDTO from(Moment.Comment comment, User commenter) {
            return new CommentDTO(
                comment.getUserId(),
                commenter != null ? commenter.getDisplayName() : "",
                commenter != null ? commenter.getAvatarUrl() : null,
                comment.getContent(),
                comment.getTimestamp()
            );
        }

        public String getUserId() {
            return userId;
        }

        public String getUserDisplayName() {
            return userDisplayName;
        }

        public String getUserAvatarUrl() {
            return userAvatarUrl;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
