package com.example.chat;

import com.example.chat.config.ServerConfig;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.FriendRequestRepository;
import com.example.chat.repository.InMemoryChatRoomRepository;
import com.example.chat.repository.InMemoryFriendRequestRepository;
import com.example.chat.repository.InMemoryMessageRepository;
import com.example.chat.repository.InMemoryMomentRepository;
import com.example.chat.repository.InMemoryTreeHoleRepository;
import com.example.chat.repository.InMemoryUserRepository;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.MomentRepository;
import com.example.chat.repository.TreeHoleRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.AiBotService;
import com.example.chat.service.AuthService;
import com.example.chat.service.ChatService;
import com.example.chat.service.FriendService;
import com.example.chat.service.MomentService;
import com.example.chat.service.OpenAiChatClient;
import com.example.chat.service.RoomService;
import com.example.chat.service.AiCloneService;
import com.example.chat.service.TreeHoleService;
import com.example.chat.service.UserService;
import com.example.chat.websocket.ChatWebSocketServer;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * 程序入口，负责组装依赖并启动 WebSocket 服务器。
 */
public class Main {
    public static void main(String[] args) {
        UserRepository userRepository = new InMemoryUserRepository();
        ChatRoomRepository chatRoomRepository = new InMemoryChatRoomRepository();
        FriendRequestRepository friendRequestRepository = new InMemoryFriendRequestRepository();
        MessageRepository messageRepository = new InMemoryMessageRepository();
        MomentRepository momentRepository = new InMemoryMomentRepository();
        TreeHoleRepository treeHoleRepository = new InMemoryTreeHoleRepository();

        AuthService authService = new AuthService(userRepository);
        UserService userService = new UserService(userRepository);
        FriendService friendService = new FriendService(userRepository, friendRequestRepository, userService);
        RoomService roomService = new RoomService(chatRoomRepository, userRepository);
        ChatService chatService = new ChatService(messageRepository, chatRoomRepository, userRepository);
        MomentService momentService = new MomentService(momentRepository, friendService, userService);
        TreeHoleService treeHoleService = new TreeHoleService(treeHoleRepository, ServerConfig.TREE_HOLE_RECENT_LIMIT);

        OpenAiChatClient openAiChatClient = new OpenAiChatClient(
                ServerConfig.OPENAI_API_KEY,
                ServerConfig.OPENAI_BASE_URL,
                ServerConfig.OPENAI_MODEL
        );
        AiBotService aiBotService = new AiBotService(userRepository, chatService, roomService, openAiChatClient);
        aiBotService.ensureBotUser();
        AiCloneService aiCloneService = new AiCloneService(userRepository, chatService, roomService, openAiChatClient, friendService);

        WebSocketServer server = new ChatWebSocketServer(
                new InetSocketAddress(ServerConfig.WS_PORT),
                authService,
                userService,
                friendService,
                momentService,
                roomService,
                chatService,
                aiBotService,
                aiCloneService,
                treeHoleService
        );

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        System.out.printf("ChatRoom WebSocket 服务已启动，端口 %d%n", ServerConfig.WS_PORT);
        try {
            new java.util.concurrent.CountDownLatch(1).await(); // block main thread
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
