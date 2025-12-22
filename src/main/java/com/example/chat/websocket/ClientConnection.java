package com.example.chat.websocket;

import org.java_websocket.WebSocket;

import java.util.HashSet;
import java.util.Set;

/**
 * 封装单个连接的上下文信息。
 */
public class ClientConnection {
    private final WebSocket socket;
    private String userId;
    private final Set<String> joinedRoomIds = new HashSet<>();

    public ClientConnection(WebSocket socket) {
        this.socket = socket;
    }

    public WebSocket getSocket() {
        return socket;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<String> getJoinedRoomIds() {
        return joinedRoomIds;
    }

    public void joinRoom(String roomId) {
        joinedRoomIds.add(roomId);
    }

    public void leaveRoom(String roomId) {
        joinedRoomIds.remove(roomId);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }
}
