package com.example.chat.websocket;

import com.example.chat.config.ServerConfig;
import com.example.chat.dto.AiCloneDTO;
import com.example.chat.dto.FriendRequestDTO;
import com.example.chat.dto.MomentDTO;
import com.example.chat.dto.MessageDTO;
import com.example.chat.dto.RoomDTO;
import com.example.chat.dto.TreeHoleMessageDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.model.ChatRoom;
import com.example.chat.model.Message;
import com.example.chat.model.MessageType;
import com.example.chat.model.RoomType;
import com.example.chat.model.UserStatus;
import com.example.chat.model.User;
import com.example.chat.service.AiBotService;
import com.example.chat.service.AiCloneService;
import com.example.chat.service.AuthService;
import com.example.chat.service.ChatService;
import com.example.chat.service.FriendService;
import com.example.chat.service.MomentService;
import com.example.chat.service.RoomService;
import com.example.chat.service.TreeHoleService;
import com.example.chat.service.AiBotService.AiSelectorDecision;
import com.example.chat.service.UserService;
import com.example.chat.util.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket 服务器
 */
public class ChatWebSocketServer extends WebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatWebSocketServer.class);
    private static final int AI_MAX_CONSECUTIVE_REPLIES = 50;
    private static final long AI_SELECTOR_REPLY_DELAY_MS = 1200L;
    private static final long AI_MIN_GAP_AFTER_AI_MS = 1200L;

    private final AuthService authService;
    private final UserService userService;
    private final FriendService friendService;
    private final MomentService momentService;
    private final RoomService roomService;
    private final ChatService chatService;
    private final AiBotService aiBotService;
    private final AiCloneService aiCloneService;
    private final TreeHoleService treeHoleService;
    private final Map<WebSocket, ClientConnection> connections = new ConcurrentHashMap<>(); // 成功连接列表
    private final Map<String, ClientConnection> activeUsers = new ConcurrentHashMap<>(); // 登录连接列表
    private final Gson gson = JsonUtil.gson();

    public ChatWebSocketServer(InetSocketAddress address, AuthService authService, UserService userService, FriendService friendService, MomentService momentService, RoomService roomService, ChatService chatService, AiBotService aiBotService, AiCloneService aiCloneService, TreeHoleService treeHoleService) {
        super(address);
        this.authService = authService;
        this.userService = userService;
        this.friendService = friendService;
        this.momentService = momentService;
        this.roomService = roomService;
        this.chatService = chatService;
        this.aiBotService = aiBotService;
        this.aiCloneService = aiCloneService;
        this.treeHoleService = treeHoleService;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("客户端连接建立 {}", conn.getRemoteSocketAddress());
        connections.put(conn, new ClientConnection(conn));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("连接关闭: {}", conn.getRemoteSocketAddress());
        ClientConnection context = connections.remove(conn);
        if (context != null && context.getUserId() != null) {
            activeUsers.remove(context.getUserId());
            userService.updateStatus(context.getUserId(), UserStatus.OFFLINE);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject envelope = JsonParser.parseString(message).getAsJsonObject();
            String type = envelope.get("type").getAsString();
            type = type == null ? "" : type.trim().toUpperCase();
            JsonObject payload = envelope.has("payload") && envelope.get("payload").isJsonObject()
                    ? envelope.getAsJsonObject("payload")
                    : new JsonObject();
            LOGGER.info("收到消息 type={} payload={}", type, payload);
            if (type.startsWith("MOMENT")) {
                switch (type) {
                    case "MOMENT_POST" -> handleMomentPost(conn, payload);
                    case "MOMENT_LIKE" -> handleMomentLike(conn, payload);
                    case "MOMENT_COMMENT" -> handleMomentComment(conn, payload);
                    default -> handleMomentList(conn);
                }
                return;
            }
            switch (type) {
                case "CONFIG_GET" -> handleConfigGet(conn);
                case "AUTH_REGISTER" -> handleAuthRegister(conn, payload);
                case "AUTH_LOGIN" -> handleAuthLogin(conn, payload);
                case "FRIEND_SEARCH" -> handleFriendSearch(conn, payload);
                case "FRIEND_ADD" -> handleFriendAdd(conn, payload);
                case "FRIEND_LIST" -> handleFriendList(conn);
                case "FRIEND_REQUEST_LIST" -> handleFriendRequestList(conn);
                case "FRIEND_REQUEST_RESPOND" -> handleFriendRequestRespond(conn, payload);
                case "PRIVATE_OPEN" -> handlePrivateOpen(conn, payload);
                case "PRIVATE_MESSAGE" -> handlePrivateMessage(conn, payload);
                case "ROOM_CREATE" -> handleRoomCreate(conn, payload);
                case "ROOM_LIST" -> handleRoomList(conn);
                case "ROOM_JOIN" -> handleRoomJoin(conn, payload);
                case "ROOM_SEARCH" -> handleRoomSearch(conn, payload);
                case "ROOM_UPDATE" -> handleRoomUpdate(conn, payload);
                case "ROOM_INVITE" -> handleRoomInvite(conn, payload);
                case "ROOM_REMOVE_MEMBER" -> handleRoomRemoveMember(conn, payload);
                case "ROOM_MESSAGE" -> handleRoomMessage(conn, payload);
                case "FILE_MESSAGE" -> handleFileMessage(conn, payload);
                case "GAME_REQUEST" -> handleGameRequest(conn, payload);
                case "MESSAGE_RECALL" -> handleMessageRecall(conn, payload);
                case "MOMENT_POST" -> handleMomentPost(conn, payload);
                case "MOMENT_LIST" -> handleMomentList(conn);
                case "MOMENT_LIKE" -> handleMomentLike(conn, payload);
                case "MOMENT_COMMENT" -> handleMomentComment(conn, payload);
                case "POST_TREE_HOLE" -> handlePostTreeHole(conn, payload);
                case "FETCH_TREE_HOLE" -> handleFetchTreeHole(conn, payload);
                case "AI_CLONE_CREATE" -> handleAiCloneCreate(conn, payload);
                case "AI_CLONE_LIST" -> handleAiCloneList(conn);
                case "AI_CLONE_SELF_PROMPT" -> handleAiCloneSelfPrompt(conn);
                case "PROFILE_GET" -> handleProfileGet(conn);
                case "PROFILE_VIEW" -> handleProfileView(conn, payload);
                case "PROFILE_UPDATE" -> handleProfileUpdate(conn, payload);
                case "CREATE_FORWARD" -> handleCreateForward(conn, payload);
                default -> {
                    if ("CREATE_FORWARD".equalsIgnoreCase(type)) {
                        handleCreateForward(conn, payload);
                    } else if ("GAME_REQUEST".equalsIgnoreCase(type)) {
                        LOGGER.warn("触发 GAME_REQUEST 兼容分支");
                        handleGameRequest(conn, payload);
                    } else if (type != null && type.toUpperCase().startsWith("MOMENT")) {
                        handleMomentList(conn);
                    } else {
                        LOGGER.warn("收到未知的消息类型 {}", type);
                        sendError(conn, "未知的消息类型 " + type);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("处理消息时发生异常", e);
            sendError(conn, "服务器解析消息失败");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("WebSocket 错误", ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("WebSocket 服务器启动成功，端口 {}", getPort());
    }

    private void handleConfigGet(WebSocket conn) {
        Map<String, Object> config = new HashMap<>();
        config.put("imgApiBase", ServerConfig.IMG_API_BASE);
        config.put("imgApiEmail", ServerConfig.IMG_API_EMAIL);
        config.put("imgApiPassword", ServerConfig.IMG_API_PASSWORD);
        send(conn, "CONFIG_RESULT", config);
    }

    private void handleAuthRegister(WebSocket conn, JsonObject payload) {
        String username = getRequiredString(payload, "username");
        String password = getRequiredString(payload, "password");
        try {
            UserDTO dto = userService.toDTO(authService.register(username, password));
            send(conn, "AUTH_REGISTER_RESULT", Map.of("success", true, "user", dto));
        } catch (IllegalArgumentException e) {
            send(conn, "AUTH_REGISTER_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleAuthLogin(WebSocket conn, JsonObject payload) {
        String username = getRequiredString(payload, "username");
        String password = getRequiredString(payload, "password");
        authService.login(username, password).ifPresentOrElse(user -> {
            ClientConnection context = connections.get(conn);
            context.setUserId(user.getId());
            activeUsers.put(user.getId(), context);
            userService.updateStatus(user.getId(), UserStatus.ONLINE);
            send(conn, "AUTH_LOGIN_RESULT", Map.of("success", true, "user", userService.toDTO(user)));
            pushFriendList(user.getId());
            pushMomentTimeline(user.getId());
            pushRoomList(user.getId());
            pushFriendRequests(user.getId());
            pushCloneList(user.getId());
        }, () -> send(conn, "AUTH_LOGIN_RESULT", Map.of("success", false, "errorMessage", "用户名或密码错误")));
    }

    private void handleFriendSearch(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String keyword = getOptionalString(payload, "keyword");
        List<UserDTO> candidates = friendService.searchFriends(keyword);
        List<UserDTO> results = new ArrayList<>();
        for (UserDTO dto : candidates) {
            if (!dto.getId().equals(context.getUserId())) {
                results.add(dto);
            }
        }
        send(conn, "FRIEND_SEARCH_RESULT", Map.of("results", results));
    }

    private void handleFriendAdd(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String targetUserId = getRequiredString(payload, "targetUserId");
        try {
            friendService.sendFriendRequest(context.getUserId(), targetUserId);
            send(conn, "FRIEND_ADD_RESULT", Map.of("success", true));
            boolean targetIsAi = aiBotService.isBot(targetUserId) || aiCloneService.isAiClone(targetUserId);
            if (targetIsAi) {
                pushFriendList(context.getUserId());
                pushCloneList(context.getUserId());
            } else {
                pushFriendRequests(targetUserId);
            }
        } catch (Exception e) {
            send(conn, "FRIEND_ADD_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleFriendList(WebSocket conn) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        List<UserDTO> friends = friendService.listFriends(context.getUserId());
        send(conn, "FRIEND_LIST_RESULT", Map.of("friends", friends));
    }

    private void handleFriendRequestList(WebSocket conn) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        List<FriendRequestDTO> requests = friendService.listIncomingRequests(context.getUserId());
        send(conn, "FRIEND_REQUEST_LIST_RESULT", Map.of("requests", requests));
    }

    private void handleFriendRequestRespond(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String requestId = getRequiredString(payload, "requestId");
        boolean accept = payload.has("accept") && payload.get("accept").getAsBoolean();
        try {
            var request = friendService.respondToRequest(context.getUserId(), requestId, accept);
            send(conn, "FRIEND_REQUEST_RESPOND_RESULT", Map.of("success", true));
            pushFriendRequests(context.getUserId());
            if (accept) {
                pushFriendList(context.getUserId());
                pushFriendList(request.getFromUserId());
            }
        } catch (Exception e) {
            send(conn, "FRIEND_REQUEST_RESPOND_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handlePrivateOpen(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String friendUserId = getRequiredString(payload, "friendUserId");
        ChatRoom room = roomService.getOrCreatePrivateRoom(context.getUserId(), friendUserId);
        context.joinRoom(room.getId());
        List<MessageDTO> messages = chatService.getRecentMessages(room.getId(), ServerConfig.RECENT_MESSAGE_LIMIT);
        Map<String, Object> response = new HashMap<>();
        response.put("room", roomService.toDTO(room));
        response.put("recentMessages", messages);
        send(conn, "PRIVATE_OPEN_RESULT", response);
    }

    private void handlePrivateMessage(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        sendChatMessage(conn, payload, context.getUserId());
    }

    private void handleRoomCreate(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String name = getRequiredString(payload, "name");
        List<String> memberIds = getStringList(payload, "memberUserIds");
        String description = getOptionalString(payload, "description");
        String avatarUrl = getOptionalString(payload, "avatarUrl");
        boolean allowPublicJoin = true;
        if (payload.has("allowPublicJoin") && payload.get("allowPublicJoin").isJsonPrimitive()) {
            try {
                allowPublicJoin = payload.get("allowPublicJoin").getAsBoolean();
            } catch (Exception ignored) {
                allowPublicJoin = true;
            }
        }
        try {
            ChatRoom room = roomService.createGroupRoom(context.getUserId(), name, memberIds, description, avatarUrl, allowPublicJoin);
            context.joinRoom(room.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("room", roomService.toDTO(room));
            response.put("recentMessages", List.of());
            send(conn, "ROOM_CREATE_RESULT", response);
            pushRoomList(context.getUserId());
            room.getMemberIds().forEach(memberId -> {
                if (!memberId.equals(context.getUserId())) {
                    pushRoomList(memberId);
                }
            });
        } catch (Exception e) {
            send(conn, "ROOM_CREATE_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleRoomList(WebSocket conn) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        List<RoomDTO> rooms = roomService.listRoomsForUser(context.getUserId());
        send(conn, "ROOM_LIST_RESULT", Map.of("rooms", rooms));
    }

    private void handleRoomJoin(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String roomId = getRequiredString(payload, "roomId");
        try {
            ChatRoom room = roomService.joinRoom(roomId, context.getUserId());
            context.joinRoom(roomId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("room", roomService.toDTO(room));
            response.put("recentMessages", chatService.getRecentMessages(roomId, ServerConfig.RECENT_MESSAGE_LIMIT));
            send(conn, "ROOM_JOIN_RESULT", response);
            pushRoomList(context.getUserId());
        } catch (Exception e) {
            send(conn, "ROOM_JOIN_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleRoomSearch(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String keyword = getOptionalString(payload, "keyword");
        List<RoomDTO> rooms = roomService.searchRooms(keyword, context.getUserId());
        send(conn, "ROOM_SEARCH_RESULT", Map.of("rooms", rooms));
    }

    private void handleRoomUpdate(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String roomId = getRequiredString(payload, "roomId");
        String name = getOptionalString(payload, "name");
        String description = getOptionalString(payload, "description");
        String avatarUrl = getOptionalString(payload, "avatarUrl");
        Boolean allowPublicJoin = payload.has("allowPublicJoin") && payload.get("allowPublicJoin").isJsonPrimitive()
                ? payload.get("allowPublicJoin").getAsBoolean()
                : null;
        try {
            ChatRoom room = roomService.updateGroupInfo(context.getUserId(), roomId, name, description, avatarUrl, allowPublicJoin);
            RoomDTO dto = roomService.toDTO(room);
            send(conn, "ROOM_UPDATE_RESULT", Map.of("success", true, "room", dto));
            broadcastRoomUpdate(dto);
        } catch (Exception e) {
            send(conn, "ROOM_UPDATE_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleRoomInvite(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String roomId = getRequiredString(payload, "roomId");
        List<String> userIds = getStringList(payload, "memberUserIds");
        try {
            ChatRoom room = roomService.addMembers(context.getUserId(), roomId, userIds);
            RoomDTO dto = roomService.toDTO(room);
            send(conn, "ROOM_INVITE_RESULT", Map.of("success", true, "room", dto));
            broadcastRoomUpdate(dto);
            for (String userId : userIds) {
                pushRoomList(userId);
            }
        } catch (Exception e) {
            send(conn, "ROOM_INVITE_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleRoomRemoveMember(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String roomId = getRequiredString(payload, "roomId");
        String targetUserId = getRequiredString(payload, "targetUserId");
        try {
            ChatRoom room = roomService.removeMember(context.getUserId(), roomId, targetUserId);
            RoomDTO dto = roomService.toDTO(room);
            send(conn, "ROOM_REMOVE_MEMBER_RESULT", Map.of("success", true, "room", dto));
            broadcastRoomUpdate(dto);
            pushRoomList(targetUserId);
            sendToUser(targetUserId, "ROOM_REMOVED", Map.of("roomId", roomId));
        } catch (Exception e) {
            send(conn, "ROOM_REMOVE_MEMBER_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleRoomMessage(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        sendChatMessage(conn, payload, context.getUserId());
    }

    private void handleFileMessage(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String roomId = getRequiredString(payload, "roomId");
        String fileUrl = getRequiredString(payload, "fileUrl");
        String fileName = getOptionalString(payload, "fileName");
        String mimeType = getOptionalString(payload, "mimeType");
        Long fileSize = null;
        if (payload.has("fileSize") && payload.get("fileSize").isJsonPrimitive()) {
            try {
                fileSize = payload.get("fileSize").getAsLong();
            } catch (Exception ignored) {
                fileSize = null;
            }
        }
        JsonObject fileContent = new JsonObject();
        fileContent.addProperty("url", fileUrl);
        if (fileName != null) {
            fileContent.addProperty("name", fileName);
        }
        if (mimeType != null) {
            fileContent.addProperty("mimeType", mimeType);
        }
        if (fileSize != null) {
            fileContent.addProperty("size", fileSize);
        }
        JsonObject adaptedPayload = new JsonObject();
        adaptedPayload.addProperty("roomId", roomId);
        adaptedPayload.addProperty("content", gson.toJson(fileContent));
        adaptedPayload.addProperty("messageType", MessageType.FILE_LINK.name());
        sendChatMessage(conn, adaptedPayload, context.getUserId());
    }

    private void handleGameRequest(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String gameType = getRequiredString(payload, "gameType");
        String roomId = getRequiredString(payload, "roomId");
        try {
            MessageDTO result = chatService.sendGameResult(roomId, context.getUserId(), gameType);
            broadcastToRoom(roomId, Map.of("message", result));
            
            // 如果是和 AI 的私聊，AI 也发送同样的游戏
            ChatRoom room = roomService.getRoom(roomId);
            if (room != null && room.getType() == RoomType.PRIVATE) {
                if (room.hasMember(ServerConfig.AI_BOT_ID)) {
                    // 延迟一小段时间后 AI 也发送游戏结果
                    new Thread(() -> {
                        try {
                            Thread.sleep(500 + (int)(Math.random() * 500)); // 随机延迟 500-1000ms
                            MessageDTO aiResult = aiBotService.sendGameReply(roomId, gameType);
                            broadcastToRoom(roomId, Map.of("message", aiResult));
                        } catch (Exception e) {
                            LOGGER.warn("AI 游戏回复失败: {}", e.getMessage());
                        }
                    }).start();
                } else {
                    String cloneId = findCloneMember(room);
                    if (cloneId != null) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(500 + (int)(Math.random() * 500));
                                MessageDTO aiResult = aiCloneService.sendGameReply(roomId, cloneId, gameType);
                                broadcastToRoom(roomId, Map.of("message", aiResult));
                            } catch (Exception e) {
                                LOGGER.warn("AI 分身游戏回复失败: {}", e.getMessage());
                            }
                        }).start();
                    }
                }
            }
        } catch (Exception e) {
            send(conn, "GAME_REQUEST_FAILED", Map.of("errorMessage", e.getMessage()));
        }
    }

    private void handleProfileGet(WebSocket conn) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        UserDTO user = userService.getProfile(context.getUserId());
        send(conn, "PROFILE_GET_RESULT", Map.of("user", user));
    }

    private void handleProfileView(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String userId = getRequiredString(payload, "userId");
        try {
            UserDTO user = userService.getProfile(userId);
            send(conn, "PROFILE_VIEW_RESULT", Map.of("success", true, "user", user));
        } catch (Exception e) {
            send(conn, "PROFILE_VIEW_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleProfileUpdate(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String displayName = getOptionalString(payload, "displayName");
        String avatar = getOptionalString(payload, "avatarUrl");
        String signature = getOptionalString(payload, "signature");
        try {
            UserDTO updated = userService.updateProfile(context.getUserId(), displayName, avatar, signature);
            send(conn, "PROFILE_UPDATE_RESULT", Map.of("success", true, "user", updated));
        } catch (Exception e) {
            send(conn, "PROFILE_UPDATE_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleMomentPost(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String content = getOptionalString(payload, "content");
        List<String> images = getStringList(payload, "imageUrls");
        try {
            MomentDTO moment = momentService.post(context.getUserId(), content, images);
            send(conn, "MOMENT_POST_RESULT", Map.of("success", true, "moment", moment));
            broadcastMoment("MOMENT_NEW", moment);
        } catch (Exception e) {
            send(conn, "MOMENT_POST_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleMomentList(WebSocket conn) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        List<MomentDTO> moments = momentService.listTimeline(context.getUserId());
        send(conn, "MOMENT_TIMELINE", Map.of("moments", moments));
    }

    private void handleMomentLike(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String momentId = getRequiredString(payload, "momentId");
        try {
            MomentDTO updated = momentService.toggleLike(context.getUserId(), momentId);
            send(conn, "MOMENT_LIKE_RESULT", Map.of("success", true, "moment", updated));
            broadcastMoment("MOMENT_UPDATED", updated);
        } catch (Exception e) {
            send(conn, "MOMENT_LIKE_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleMomentComment(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String momentId = getRequiredString(payload, "momentId");
        String content = getRequiredString(payload, "content");
        try {
            MomentDTO updated = momentService.comment(context.getUserId(), momentId, content);
            send(conn, "MOMENT_COMMENT_RESULT", Map.of("success", true, "moment", updated));
            broadcastMoment("MOMENT_UPDATED", updated);
        } catch (Exception e) {
            send(conn, "MOMENT_COMMENT_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handlePostTreeHole(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String content = getRequiredString(payload, "content");
        try {
            TreeHoleMessageDTO message = treeHoleService.post(content);
            send(conn, "TREE_HOLE_POST_RESULT", Map.of("success", true));
            broadcastTreeHole(message);
        } catch (Exception e) {
            send(conn, "TREE_HOLE_POST_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleFetchTreeHole(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        Integer limit = null;
        if (payload != null && payload.has("limit") && payload.get("limit").isJsonPrimitive()) {
            try {
                limit = payload.get("limit").getAsInt();
            } catch (Exception ignored) {
                limit = null;
            }
        }
        List<TreeHoleMessageDTO> messages = treeHoleService.list(limit);
        send(conn, "TREE_HOLE_LIST", Map.of("messages", messages));
    }

    private void handleAiCloneCreate(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String id = getRequiredString(payload, "id");
        String displayName = getOptionalString(payload, "displayName");
        String prompt = getRequiredString(payload, "prompt");
        String avatarUrl = getOptionalString(payload, "avatarUrl");
        try {
            User clone = aiCloneService.createClone(context.getUserId(), id, displayName, prompt, avatarUrl);
            send(conn, "AI_CLONE_CREATE_RESULT", Map.of("success", true, "clone", toCloneDTO(clone, context.getUserId())));
            pushCloneList(context.getUserId());
        } catch (Exception e) {
            send(conn, "AI_CLONE_CREATE_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleAiCloneList(WebSocket conn) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        send(conn, "AI_CLONE_LIST_RESULT", buildCloneListPayload(context.getUserId()));
    }

    private void handleAiCloneSelfPrompt(WebSocket conn) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        try {
            String prompt = aiCloneService.buildSelfPrompt(context.getUserId());
            send(conn, "AI_CLONE_SELF_PROMPT_RESULT", Map.of("success", true, "prompt", prompt));
        } catch (Exception e) {
            send(conn, "AI_CLONE_SELF_PROMPT_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void sendChatMessage(WebSocket conn, JsonObject payload, String senderUserId) {
        String roomId = getRequiredString(payload, "roomId");
        String content = getRequiredString(payload, "content");
        String rawType = getOptionalString(payload, "messageType");
        MessageType messageType = parseMessageType(rawType);
        String targetUserId = getOptionalString(payload, "targetUserId");
        if (targetUserId == null) {
            targetUserId = getOptionalString(payload, "toUser");
        }
        Integer burnDelay = null;
        if (payload.has("burnDelay") && payload.get("burnDelay").isJsonPrimitive()) {
            try {
                burnDelay = payload.get("burnDelay").getAsInt();
            } catch (Exception ignored) {
                burnDelay = null;
            }
        }
        // 强制兜底：只要有 burnDelay>0 或 rawType 为空，则按阅后即焚处理
        if ((messageType == null || messageType == MessageType.TEXT) && burnDelay != null && burnDelay > 0) {
            LOGGER.info("incoming burnDelay>0 but type is {}, overriding to BURN_AFTER_READING", messageType);
            messageType = MessageType.BURN_AFTER_READING;
        }
        if (messageType == null && "BURN_AFTER_READING".equalsIgnoreCase(rawType)) {
            messageType = MessageType.BURN_AFTER_READING;
        }
        Message.QuoteInfo quote = parseQuoteInfo(payload);
        if (quote != null) {
            LOGGER.info("收到引用信息: messageId={}, sender={}, content={}", quote.getMessageId(), quote.getSender(), quote.getContent());
        }
        try {
            ChatRoom room = roomService.getRoom(roomId);
            // TOMATO 兜底：私聊未显式指定目标时，把对端作为目标，便于前端播放动画
            if (messageType == MessageType.ACTION && "TOMATO".equalsIgnoreCase(content) && targetUserId == null) {
                if (room.getType() == RoomType.PRIVATE) {
                    for (String member : room.getMemberIds()) {
                        if (!member.equals(senderUserId)) {
                            targetUserId = member;
                            break;
                        }
                    }
                }
            }

            LOGGER.info("sendChatMessage parsed -> roomId={}, sender={}, target={}, rawType={}, messageType={}, burnDelay={}, hasQuote={}",
                    roomId, senderUserId, targetUserId, rawType, messageType, burnDelay, (quote != null));
            MessageDTO message = chatService.sendMessage(roomId, senderUserId, targetUserId, content, messageType, burnDelay, quote);
            LOGGER.info("sendChatMessage saved message -> id={}, type={}, burnDelay={}", message.getId(), message.getMessageType(), message.getBurnDelay());
            if (targetUserId != null && !targetUserId.isBlank()) {
                if (!room.hasMember(targetUserId)) {
                    LOGGER.warn("targetUserId {} not in room {}", targetUserId, roomId);
                    send(conn, "MESSAGE_SEND_FAILED", Map.of("errorMessage", "目标用户不在房间内"));
                    return;
                }
                broadcastToRoom(roomId, Map.of("message", message), Set.of(senderUserId, targetUserId));
            } else {
                broadcastToRoom(roomId, Map.of("message", message));
            }
            try {
                handleBotReply(room, content, senderUserId);
            } catch (Exception botEx) {
                LOGGER.warn("AI 助手回复失败: {}", botEx.getMessage());
            }
        } catch (Exception e) {
            LOGGER.warn("发送消息失败 {}", e.getMessage());
            send(conn, "MESSAGE_SEND_FAILED", Map.of("errorMessage", e.getMessage()));
        }
    }

    private void handleCreateForward(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String targetRoomId = getRequiredString(payload, "roomId");
        List<String> messageIds = getStringList(payload, "messageIds");
        if (messageIds.isEmpty()) {
            send(conn, "MESSAGE_SEND_FAILED", Map.of("errorMessage", "请选择要转发的消息"));
            return;
        }
        try {
            ChatRoom targetRoom = roomService.getRoom(targetRoomId);
            if (!targetRoom.hasMember(context.getUserId())) {
                send(conn, "MESSAGE_SEND_FAILED", Map.of("errorMessage", "无权向该房间发送消息"));
                return;
            }
            List<Message> originals = chatService.findMessagesByIds(messageIds);
            List<Message> accessible = new ArrayList<>();
            for (Message message : originals) {
                try {
                    ChatRoom sourceRoom = roomService.getRoom(message.getRoomId());
                    if (sourceRoom.hasMember(context.getUserId())) {
                        accessible.add(message);
                    }
                } catch (Exception ignored) {
                    // ignore rooms that cannot be loaded
                }
            }
            if (accessible.isEmpty()) {
                send(conn, "MESSAGE_SEND_FAILED", Map.of("errorMessage", "未找到可转发的消息"));
                return;
            }
            accessible.sort(Comparator.comparingLong(Message::getTimestamp));
            JsonObject cardPayload = buildForwardCardPayload(accessible);
            MessageDTO dto = chatService.sendMessage(
                    targetRoomId,
                    context.getUserId(),
                    null,
                    gson.toJson(cardPayload),
                    MessageType.FORWARD_CARD
            );
            broadcastToRoom(targetRoomId, Map.of("message", dto));
        } catch (Exception e) {
            LOGGER.warn("合并转发失败: {}", e.getMessage());
            send(conn, "MESSAGE_SEND_FAILED", Map.of("errorMessage", e.getMessage()));
        }
    }

    private JsonObject buildForwardCardPayload(List<Message> messages) {
        JsonObject card = new JsonObject();
        LinkedHashSet<String> senders = new LinkedHashSet<>();
        JsonArray items = new JsonArray();
        JsonArray ids = new JsonArray();
        for (Message message : messages) {
            String sender = resolveDisplayName(message.getFromUserId());
            senders.add(sender);
            ids.add(message.getId());
            String summary = sender + ": " + summarizeForwardContent(message);
            items.add(summary);
        }
        List<String> senderList = new ArrayList<>(senders);
        String title;
        if (senderList.isEmpty()) {
            title = "聊天记录";
        } else if (senderList.size() == 1) {
            title = senderList.get(0) + "的聊天记录";
        } else if (senderList.size() == 2) {
            title = senderList.get(0) + "和" + senderList.get(1) + "的聊天记录";
        } else {
            title = String.join("、", senderList.subList(0, Math.min(3, senderList.size()))) + "的聊天记录";
        }
        card.addProperty("title", title);
        card.add("items", items);
        card.addProperty("total", messages.size());
        card.add("messageIds", ids);
        return card;
    }

    private String resolveDisplayName(String userId) {
        return userService.findById(userId)
                .map(user -> {
                    String display = user.getDisplayName();
                    if (display == null || display.isBlank()) {
                        return user.getUsername();
                    }
                    return display;
                })
                .orElseGet(() -> Objects.toString(userId, "用户"));
    }

    private String summarizeForwardContent(Message message) {
        if (message == null) {
            return "";
        }
        MessageType type = message.getMessageType();
        String content = message.getContent();
        if (type == MessageType.FILE_LINK) {
            try {
                JsonObject file = JsonParser.parseString(content).getAsJsonObject();
                if (file.has("name")) {
                    return "[文件] " + file.get("name").getAsString();
                }
                return "[文件]";
            } catch (Exception e) {
                return "[文件]";
            }
        }
        if (type == MessageType.DICE_RESULT) {
            return "掷骰子：" + content;
        }
        if (type == MessageType.RPS_RESULT) {
            return "猜拳：" + content;
        }
        if (type == MessageType.ACTION) {
            return "[互动] " + content;
        }
        if (type == MessageType.BURN_AFTER_READING) {
            return "[阅后即焚] " + content;
        }
        if (type == MessageType.FORWARD_CARD) {
            try {
                JsonObject card = JsonParser.parseString(content).getAsJsonObject();
                String title = card.has("title") ? card.get("title").getAsString() : "聊天记录";
                return "[聊天记录] " + title;
            } catch (Exception ignored) {
                return "[聊天记录]";
            }
        }
        if (content == null) {
            return "";
        }
        return content.length() > 140 ? content.substring(0, 140) + "..." : content;
    }

    private void broadcastTreeHole(TreeHoleMessageDTO message) {
        try {
            JsonObject envelope = buildEnvelope("TREE_HOLE_NEW", Map.of("message", message));
            activeUsers.values().forEach(connection -> {
                try {
                    connection.getSocket().send(envelope.toString());
                } catch (Exception e) {
                    LOGGER.warn("广播树洞消息失败: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("构建树洞广播失败: {}", e.getMessage());
        }
    }

    private void handleMessageRecall(WebSocket conn, JsonObject payload) {
        ClientConnection context = requireLogin(conn);
        if (context == null) {
            return;
        }
        String roomId = getRequiredString(payload, "roomId");
        String messageId = getRequiredString(payload, "messageId");
        try {
            MessageDTO recalled = chatService.recallMessage(roomId, messageId, context.getUserId());
            // 通知操作者
            send(conn, "MESSAGE_RECALL_RESULT", Map.of("success", true, "roomId", roomId, "messageId", messageId, "byUserId", context.getUserId()));
            // 广播给房间其他成员
            broadcastToRoom(roomId, Map.of("roomId", roomId, "messageId", messageId, "byUserId", context.getUserId()), Set.of(context.getUserId()), "MESSAGE_RECALLED");
        } catch (Exception e) {
            send(conn, "MESSAGE_RECALL_RESULT", Map.of("success", false, "errorMessage", e.getMessage()));
        }
    }

    private void handleBotReply(ChatRoom room, String content, String senderUserId) {
        handleBotReply(room, content, senderUserId, 0);
    }

    private void handleBotReply(ChatRoom room, String content, String senderUserId, int depth) {
        if (room == null) {
            return;
        }
        if (depth > 60) {
            LOGGER.warn("AI reply depth exceeded for room {}", room.getId());
            return;
        }
        boolean senderIsAi = isAiUser(senderUserId);
        boolean isGroup = room.getType() == RoomType.GROUP;
        if (isGroup) {
            if (senderIsAi) {
                room.incrementAiReplyStreak();
            } else {
                room.resetAiReplyStreak();
            }
        }
        // ????????
        if (room.getType() == RoomType.PRIVATE) {
            if (senderIsAi) {
                return;
            }
            if (room.hasMember(ServerConfig.AI_BOT_ID)) {
                MessageDTO reply = aiBotService.sendAutoReply(room.getId(), content);
                broadcastToRoom(room.getId(), Map.of("message", reply));
                return;
            }
            String cloneMemberId = findCloneMember(room);
            if (cloneMemberId != null) {
                MessageDTO reply = aiCloneService.sendPrivateReply(room.getId(), room, cloneMemberId, content);
                broadcastToRoom(room.getId(), Map.of("message", reply));
            }
            return;
        }
        if (!isGroup) {
            return;
        }
        List<User> aiMembers = listAiMembers(room);
        if (aiMembers.isEmpty()) {
            return;
        }
        if (room.getAiReplyStreak() >= AI_MAX_CONSECUTIVE_REPLIES && senderIsAi) {
            LOGGER.info("Skip AI reply because streak {} reached limit in room {}", room.getAiReplyStreak(), room.getId());
            return;
        }
        boolean mentionBot = hasBotMention(content);
        CloneMention mentionedClone = findMentionedCloneInRoom(room, content);
        boolean responded = false;

        if (mentionBot && room.hasMember(ServerConfig.AI_BOT_ID) && canSendMoreAi(room)) {
            String question = stripBotMention(content);
            MessageDTO reply = aiBotService.sendGroupReply(room.getId(), room, question.isBlank() ? content : question);
            broadcastToRoom(room.getId(), Map.of("message", reply));
            responded = true;
            handleBotReply(room, reply.getContent(), reply.getFromUserId(), depth + 1);
        }
        if (!responded && mentionedClone != null && canSendMoreAi(room)) {
            String question = stripToken(content, "@" + mentionedClone.token);
            MessageDTO reply = aiCloneService.sendGroupReply(room.getId(), room, mentionedClone.id, question.isBlank() ? content : question);
            broadcastToRoom(room.getId(), Map.of("message", reply));
            responded = true;
            handleBotReply(room, reply.getContent(), reply.getFromUserId(), depth + 1);
        }
        if (!responded && canSendMoreAi(room)) {
            waitForSelectorTurn(room);
            List<MessageDTO> recent = chatService.getRecentMessages(room.getId(), ServerConfig.AI_RECENT_SUMMARY_LIMIT);
            Set<String> aiIds = aiMembers.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
            AiStreakInfo aiStreak = findRecentAiStreak(recent, aiIds);
            AiSelectorDecision decision = aiBotService.selectGroupResponder(room.getName(), content, recent, aiMembers, room.getAiReplyStreak());
            if (decision.shouldReply() && decision.targetUserId() != null && canSendMoreAi(room)) {
                String targetId = decision.targetUserId();
                if (aiStreak != null && targetId.equals(aiStreak.lastAiId) && aiStreak.count >= 3) {
                    LOGGER.info("Skip selector reply to prevent self-loop: ai={} count={}", targetId, aiStreak.count);
                    return;
                }
                MessageDTO reply;
                if (aiBotService.isBot(targetId)) {
                    reply = aiBotService.sendGroupReply(room.getId(), room, content);
                } else {
                    reply = aiCloneService.sendGroupReply(room.getId(), room, targetId, content);
                }
                broadcastToRoom(room.getId(), Map.of("message", reply));
                handleBotReply(room, reply.getContent(), reply.getFromUserId(), depth + 1);
            }
        }
    }

    private boolean isAiUser(String userId) {
        return aiBotService.isBot(userId) || aiCloneService.isAiClone(userId);
    }

    private boolean canSendMoreAi(ChatRoom room) {
        return room.getAiReplyStreak() < AI_MAX_CONSECUTIVE_REPLIES;
    }

    private List<User> listAiMembers(ChatRoom room) {
        List<User> aiMembers = new ArrayList<>();
        for (String memberId : room.getMemberIds()) {
            if (!isAiUser(memberId)) {
                continue;
            }
            userService.findById(memberId).ifPresentOrElse(aiMembers::add, () -> {
                if (aiBotService.isBot(memberId)) {
                    User placeholder = new User(ServerConfig.AI_BOT_ID, ServerConfig.AI_BOT_USERNAME, "bot");
                    placeholder.setDisplayName(ServerConfig.AI_BOT_DISPLAY_NAME);
                    aiMembers.add(placeholder);
                }
            });
        }
        return aiMembers;
    }

    private boolean hasBotMention(String content) {
        if (content == null) {
            return false;
        }
        String lower = content.toLowerCase();
        if (lower.contains("@ai-bot") || lower.contains("@ai_bot") || lower.contains("@ai")) {
            return true;
        }
        Matcher matcher = Pattern.compile("@([^\s@]+)").matcher(content);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token.equalsIgnoreCase(ServerConfig.AI_BOT_USERNAME)
                    || token.equalsIgnoreCase(ServerConfig.AI_BOT_DISPLAY_NAME)
                    || token.equalsIgnoreCase("小爱")
                    || token.equalsIgnoreCase("小爱同学")) {
                return true;
            }
        }
        return false;
    }

    private String stripBotMention(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("(?i)@ai-bot|@ai_bot|@ai|@小爱同学|@小爱", "").trim();
    }

    private String stripToken(String content, String token) {
        if (content == null) {
            return "";
        }
        if (token == null || token.isBlank()) {
            return content.trim();
        }
        return content.replace(token, "").trim();
    }

    private AiStreakInfo findRecentAiStreak(List<MessageDTO> recentMessages, Set<String> aiIds) {
        if (recentMessages == null || recentMessages.isEmpty() || aiIds == null || aiIds.isEmpty()) {
            return null;
        }
        int count = 0;
        String lastAiId = null;
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            MessageDTO msg = recentMessages.get(i);
            if (aiIds.contains(msg.getFromUserId())) {
                if (lastAiId == null) {
                    lastAiId = msg.getFromUserId();
                    count = 1;
                } else if (lastAiId.equals(msg.getFromUserId())) {
                    count++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (lastAiId == null) {
            return null;
        }
        return new AiStreakInfo(lastAiId, count);
    }

    private void waitForSelectorTurn(ChatRoom room) {
        if (room == null) {
            return;
        }
        long slept = 0L;
        try {
            long now = System.currentTimeMillis();
            List<MessageDTO> last = chatService.getRecentMessages(room.getId(), 1);
            if (!last.isEmpty()) {
                MessageDTO latest = last.get(0);
                boolean lastIsAi = isAiUser(latest.getFromUserId());
                if (lastIsAi) {
                    long gap = now - latest.getTimestamp();
                    long need = AI_MIN_GAP_AFTER_AI_MS - gap;
                    if (need > 0) {
                        Thread.sleep(need);
                        slept += need;
                    }
                }
            }
            long remain = AI_SELECTOR_REPLY_DELAY_MS - slept;
            if (remain > 0) {
                Thread.sleep(remain);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.debug("waitForSelectorTurn skip delay: {}", e.getMessage());
        }
    }

    private static class AiStreakInfo {
        final String lastAiId;
        final int count;

        AiStreakInfo(String lastAiId, int count) {
            this.lastAiId = lastAiId;
            this.count = count;
        }
    }
    private void broadcastToRoom(String roomId, Map<String, Object> payload) {
        broadcastToRoom(roomId, payload, null, null);
    }

    private void broadcastToRoom(String roomId, Map<String, Object> payload, Set<String> targetUserIds) {
        broadcastToRoom(roomId, payload, targetUserIds, null);
    }

    private void broadcastToRoom(String roomId, Map<String, Object> payload, Set<String> targetUserIds, String type) {
        try {
            String eventType = (type == null || type.isBlank()) ? "NEW_MESSAGE" : type;
            JsonObject envelope = buildEnvelope(eventType, payload);
            Set<String> memberIds = roomService.getRoom(roomId).getMemberIds();
            for (String memberId : memberIds) {
                if (targetUserIds != null && !targetUserIds.isEmpty() && !targetUserIds.contains(memberId)) {
                    continue;
                }
                ClientConnection connection = activeUsers.get(memberId);
                if (connection != null) {
                    connection.getJoinedRoomIds().add(roomId);
                    connection.getSocket().send(envelope.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("广播房间消息失败: {}", e.getMessage());
        }
    }

    private void broadcastRoomUpdate(RoomDTO room) {
        if (room == null) {
            return;
        }
        Map<String, Object> payload = Map.of("room", room);
        for (String memberId : room.getMemberIds()) {
            sendToUser(memberId, "ROOM_UPDATED", payload);
        }
    }

    private ClientConnection requireLogin(WebSocket conn) {
        ClientConnection context = connections.get(conn);
        if (context == null || !context.isAuthenticated()) {
            sendError(conn, "请先完成登录");
            return null;
        }
        return context;
    }

    private String getRequiredString(JsonObject payload, String key) {
        if (!payload.has(key) || payload.get(key).isJsonNull()) {
            throw new IllegalArgumentException("缺少字段: " + key);
        }
        return payload.get(key).getAsString();
    }

    private String getOptionalString(JsonObject payload, String key) {
        if (!payload.has(key) || payload.get(key).isJsonNull()) {
            return null;
        }
        String value = payload.get(key).getAsString();
        return value.isBlank() ? null : value;
    }

    private Message.QuoteInfo parseQuoteInfo(JsonObject payload) {
        if (payload == null || !payload.has("quote") || !payload.get("quote").isJsonObject()) {
            return null;
        }
        JsonObject quoteObj = payload.getAsJsonObject("quote");
        String messageId = getOptionalString(quoteObj, "messageId");
        String roomId = getOptionalString(quoteObj, "roomId");
        String sender = getOptionalString(quoteObj, "sender");
        String content = getOptionalString(quoteObj, "content");
        if (messageId == null && content == null) {
            return null;
        }
        return new Message.QuoteInfo(messageId, roomId, sender, content);
    }

    private List<String> getStringList(JsonObject payload, String key) {
        List<String> values = new ArrayList<>();
        if (!payload.has(key) || !payload.get(key).isJsonArray()) {
            return values;
        }
        JsonArray array = payload.getAsJsonArray(key);
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }

    private MessageType parseMessageType(String raw) {
        if (raw == null) {
            return MessageType.TEXT;
        }
        try {
            return MessageType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }

    private String findCloneMember(ChatRoom room) {
        for (String memberId : room.getMemberIds()) {
            if (aiCloneService.isAiClone(memberId)) {
                return memberId;
            }
        }
        return null;
    }

    private CloneMention findMentionedCloneInRoom(ChatRoom room, String content) {
        if (content == null || room == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("@([^\\\\s@]+)").matcher(content);
        Set<String> members = room.getMemberIds();
        while (matcher.find()) {
            String token = matcher.group(1);
            for (String memberId : members) {
                if (!aiCloneService.isAiClone(memberId)) {
                    continue;
                }
                var cloneOpt = userService.findById(memberId);
                if (cloneOpt.isEmpty()) {
                    continue;
                }
                User clone = cloneOpt.get();
                String display = clone.getDisplayName() == null ? "" : clone.getDisplayName();
                if (token.equalsIgnoreCase(memberId)
                        || token.equalsIgnoreCase(display)
                        || token.equalsIgnoreCase(clone.getUsername())) {
                    return new CloneMention(memberId, token);
                }
            }
        }
        return null;
    }

    private static class CloneMention {
        final String id;
        final String token;

        CloneMention(String id, String token) {
            this.id = id;
            this.token = token;
        }
    }

    private void broadcastMoment(String eventType, MomentDTO moment) {
        if (moment == null) {
            return;
        }
        List<String> targets = momentService.collectUserAndFriends(moment.getUserId());
        for (String userId : targets) {
            MomentDTO view = momentService.find(moment.getId(), userId);
            sendToUser(userId, eventType, Map.of("moment", view == null ? moment : view));
        }
    }

    private void pushMomentTimeline(String userId) {
        List<MomentDTO> moments = momentService.listTimeline(userId);
        sendToUser(userId, "MOMENT_TIMELINE", Map.of("moments", moments));
    }

    private void pushFriendList(String userId) {
        List<UserDTO> friends = friendService.listFriends(userId);
        sendToUser(userId, "FRIEND_LIST_RESULT", Map.of("friends", friends));
    }

    private void pushRoomList(String userId) {
        List<RoomDTO> rooms = roomService.listRoomsForUser(userId);
        sendToUser(userId, "ROOM_LIST_RESULT", Map.of("rooms", rooms));
    }

    private void pushFriendRequests(String userId) {
        List<FriendRequestDTO> requests = friendService.listIncomingRequests(userId);
        sendToUser(userId, "FRIEND_REQUEST_LIST_RESULT", Map.of("requests", requests));
    }

    private void pushCloneList(String userId) {
        sendToUser(userId, "AI_CLONE_LIST_RESULT", buildCloneListPayload(userId));
    }

    private Map<String, Object> buildCloneListPayload(String currentUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("mine", buildCloneDTOs(aiCloneService.listClonesOfOwner(currentUserId), currentUserId));
        data.put("all", buildCloneDTOs(aiCloneService.listClones(), currentUserId));
        return data;
    }

    private List<AiCloneDTO> buildCloneDTOs(List<User> clones, String currentUserId) {
        Set<String> friendIds = userService.findById(currentUserId)
                .map(User::getFriendIds)
                .orElse(Set.of());
        List<AiCloneDTO> results = new ArrayList<>();
        for (User clone : clones) {
            User owner = clone.getOwnerUserId() == null ? null : userService.findById(clone.getOwnerUserId()).orElse(null);
            boolean isFriend = friendIds.contains(clone.getId());
            results.add(AiCloneDTO.from(clone, owner, currentUserId, isFriend));
        }
        return results;
    }

    private AiCloneDTO toCloneDTO(User clone, String currentUserId) {
        User owner = clone.getOwnerUserId() == null ? null : userService.findById(clone.getOwnerUserId()).orElse(null);
        Set<String> friendIds = userService.findById(currentUserId)
                .map(User::getFriendIds)
                .orElse(Set.of());
        boolean isFriend = friendIds.contains(clone.getId());
        return AiCloneDTO.from(clone, owner, currentUserId, isFriend);
    }

    private void sendToUser(String userId, String type, Object payload) {
        ClientConnection connection = activeUsers.get(userId);
        if (connection != null) {
            send(connection.getSocket(), type, payload);
        }
    }

    private void sendError(WebSocket conn, String message) {
        send(conn, "ERROR", Map.of("errorMessage", message));
    }

    private void send(WebSocket conn, String type, Object payload) {
        JsonObject envelope = buildEnvelope(type, payload);
        conn.send(envelope.toString());
    }

    private JsonObject buildEnvelope(String type, Object payload) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("type", type);
        envelope.add("payload", gson.toJsonTree(payload));
        return envelope;
    }
}
