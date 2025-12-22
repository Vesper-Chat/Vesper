package com.example.chat.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 朋友圈动态实体，记录发布者、内容、点赞和评论。
 */
public class Moment {
    private final String id;
    private final String userId;
    private final String content;
    private final long timestamp;
    private final List<String> imageUrls;
    private final Set<String> likes = ConcurrentHashMap.newKeySet();
    private final List<Comment> comments = Collections.synchronizedList(new ArrayList<>());

    public Moment(String id, String userId, String content, List<String> imageUrls, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public Set<String> getLikes() {
        return likes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public static class Comment {
        private final String userId;
        private final String content;
        private final long timestamp;

        public Comment(String userId, String content, long timestamp) {
            this.userId = userId;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getUserId() {
            return userId;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
