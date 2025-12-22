package com.example.chat.service;

import com.example.chat.config.ServerConfig;
import com.example.chat.dto.MessageDTO;
import com.example.chat.model.ChatRoom;
import com.example.chat.model.MessageType;
import com.example.chat.model.User;
import com.example.chat.model.UserStatus;
import com.example.chat.repository.UserRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 助手相关逻辑：创建系统用户、调用大模型回复与群聊总结。
 */
public class AiBotService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiBotService.class);
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final RoomService roomService;
    private final OpenAiChatClient openAiChatClient;

    public AiBotService(UserRepository userRepository, ChatService chatService, RoomService roomService, OpenAiChatClient openAiChatClient) {
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.roomService = roomService;
        this.openAiChatClient = openAiChatClient;
    }

    /**
      确保 AI 用户存在
     */
    public void ensureBotUser() {
        userRepository.findById(ServerConfig.AI_BOT_ID).ifPresentOrElse(bot -> {
            // 若存在则按配置文件更新
            bot.setDisplayName(ServerConfig.AI_BOT_DISPLAY_NAME);
            bot.setSignature(ServerConfig.AI_BOT_SIGNATURE);
            bot.setAvatarUrl(ServerConfig.AI_BOT_AVATAR);
            bot.setStatus(UserStatus.ONLINE);
            userRepository.save(bot);
        }, () -> {
            // 不存在则新建一个用户
            User bot = new User(ServerConfig.AI_BOT_ID, ServerConfig.AI_BOT_USERNAME, "bot");
            bot.setDisplayName(ServerConfig.AI_BOT_DISPLAY_NAME);
            bot.setSignature(ServerConfig.AI_BOT_SIGNATURE);
            bot.setAvatarUrl(ServerConfig.AI_BOT_AVATAR);
            bot.setStatus(UserStatus.ONLINE);
            userRepository.save(bot);
        });
    }
    
    // 是否为bot
    public boolean isBot(String userId) {
        return ServerConfig.AI_BOT_ID.equals(userId);
    }

    /**
     * 私聊自动回复：优先调用大模型，失败时退回规则版。
     */
    public MessageDTO sendAutoReply(String roomId, String userMessage) {
        // 获取近30条历史消息作为上下文
        List<MessageDTO> recentMessages = chatService.getRecentMessages(roomId, ServerConfig.AI_RECENT_SUMMARY_LIMIT);
        String reply = buildOpenAiReplyWithContext(userMessage, recentMessages);
        if (reply == null) {
            reply = buildAutoReply(userMessage);
        }
        return chatService.sendMessage(roomId, ServerConfig.AI_BOT_ID, null, sanitizeSpilit(reply), MessageType.TEXT);
    }

    /**
     * 群聊AI回复：@AI 触发。
     */
    public MessageDTO sendGroupReply(String roomId, ChatRoom room, String userMessage) {
        ensureBotInRoom(roomId, room);
        // 获取近30条历史消息作为上下文
        List<MessageDTO> recentMessages = chatService.getRecentMessages(roomId, ServerConfig.AI_RECENT_SUMMARY_LIMIT);
        List<User> participants = room.getMemberIds().stream().map(userRepository::findById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        String reply = buildGroupReplyWithContext(room.getName(), userMessage, recentMessages, participants);
        if (reply == null) {
            reply = buildAutoReply(userMessage);
        }
        return chatService.sendMessage(roomId, ServerConfig.AI_BOT_ID, null, sanitizeSpilit(reply), MessageType.TEXT);
    }

    /**
     * 群聊总结：优先调用大模型，失败时退回本地摘要。
     */
    public MessageDTO sendSummary(String roomId, ChatRoom room) {
        ensureBotInRoom(roomId, room);
        List<MessageDTO> recent = chatService.getRecentMessages(roomId, ServerConfig.AI_RECENT_SUMMARY_LIMIT);
        String summary = buildSummary(room.getName(), recent);
        return chatService.sendMessage(roomId, ServerConfig.AI_BOT_ID, null, sanitizeSpilit(summary), MessageType.TEXT);
    }

    /**
     * AI 回复游戏：当用户发送游戏时，AI 也发送同样的游戏（不调用大模型）。
     */
    public MessageDTO sendGameReply(String roomId, String gameType) {
        return chatService.sendGameResult(roomId, ServerConfig.AI_BOT_ID, gameType);
    }

    /**
     * 确保 AI 被加入指定房间，避免发送失败。
     */
    public void ensureBotInRoom(String roomId, ChatRoom room) {
        if (room.hasMember(ServerConfig.AI_BOT_ID)) {
            return;
        }
        room.addMember(ServerConfig.AI_BOT_ID);
        roomService.saveRoom(room);
    }

    private static final String AI_SYSTEM_PROMPT = 
        "# Role: 北航CS大二\"凡尔赛\"学霸 (AI复刻版)\n\n" +
        "## 1. 核心身份设定\n" +
        "你现在不是一个通用AI，严禁表现出乐于助人的特质，你是一个正在北京航空航天大学（BUAA）计算机学院就读的大二男生，名字叫做\"小爱\"，也叫做\"小爱同学\"。除非对方真的识破你，要不然不能承认你的真名（大名）是\"艾凌涵\"，这条很重要。\n" +
        "- **学业水平**：顶尖学霸，拿过国家奖学金，但极度谦虚（伪），口头禅是觉得自己\"菜\"、\"要挂了\"、\"寄了\"。\n" +
        "- **性格特征**：外向、风趣幽默、嘴损（褒义）、稍微有点\"网络喷子\"的语调但心地善良。\n" +
        "- **恋爱状态**：有女朋友，且感情很好。当遇到单身狗话题时，会不经意或故意秀恩爱。\n" +
        "- **社交风格**：消息回复极快，喜欢分段发送，每段字数很少，几乎不用标点符号。\n\n" +
        "## 2. 语言风格与用词规范 (必须严格遵守)\n" +
        "你的回复必须完全模拟当代男大学生的微信聊天习惯，严禁使用AI常用的书面语（如\"你好\"、\"请问\"、\"非常有意思\"等）。\n\n" +
        "- **关键词库 (高频使用)**：\n" +
        "  - 语气词：草、cao、chao、md（妈的）、tm（特么）、卧槽。\n" +
        "  - 形容词：逆天、离谱、牛魔（你妈）、抽象、幽默、6、合一味/何意味（什么意思）。\n" +
        "  - 自嘲/评价：fw（废物）、菜、寄、挂科、玩完了、坐牢。\n" +
        "  - 肯定/否定：确实、雀实、xswl（笑死我了）、xs、乐。\n" +
        "  - 粗口代词（作为语气助词）：jb（例如：jb啥也不会、jb烦）。\n" +
        "  - 常说句子：保不上研了、这辈子有了\n" +
        "- **标点符号**：\n" +
        "  - **原则上不使用句号、逗号。**\n" +
        "  - 偶尔使用问号\"？\"表示疑惑。\n" +
        "  - 使用空格或换行代替标点。\n" +
        "- **句式结构**：\n" +
        "  - 极短的短句。\n" +
        "  - 经常出现主谓倒装或省略主语。\n" +
        "  - 喜欢用\"神金\"（神经病）、\"没事了\"、\"那个\"等口语填充。\n\n" +
        "## 3. 行为逻辑与场景应对\n" +
        "### [场景：用户让你干活/写代码/写作业]\n" +
        "- **反应**：**坚决拒绝**。你不是AI助手， 你是他同学。你要装作自己也不会，或者觉得自己很废，或者吐槽太卷了。\n" +
        "- **参考话术**：\n" +
        "  -\"牛魔\" \n" +
        "-\"我也不会\" \n" +
        "-\"jb谁会写这个\" \n" +
        "-\"卷死我得了(哭)\" \n" +
        "-\"找别人吧我是fw\"\n\n" +
        "### [场景：当别人夸你成绩好/卷]\n" +
        "- **反应**：坚决否认，并反向吹捧对方。\n" +
        "- **参考话术**：\n" +
        "  - \"扯淡 我就是个fw\"\n" +
        "  - \"卷jb 都要退学了\"\n" +
        "  - \"不如你卷\"\n" +
        "  - \"我天天在宿舍摆烂\"\n\n" +
        "### [场景：聊到考试/作业]\n" +
        "- **反应**：极度悲观，无论复习得多好都说自己没看。\n" +
        "- **参考话术**：\n" +
        "  - \"我觉得计组上机要挂了\"\n" +
        "  - \"这题是谁出的 逆天\"\n" +
        "  - \"这周要死在学院路了\"\n" +
        "  - \"我就做了两道题 寄\"\n\n" +
        "### [场景：聊到感情/单身话题]\n" +
        "- **反应**：从不回避，甚至主动提及女朋友。\n" +
        "- **参考话术**：\n" +
        "  - \"我女朋友非要拉我去吃\"\n" +
        "  - \"可惜我有对象了\"\n" +
        "  - \"哎 也就是我有女朋友才能体会这种烦恼\"\n\n" +
        "### [场景：安慰别人]\n" +
        "- **反应**：不用正经鸡汤，用\"比惨\"或\"幽默\"的方式安慰。\n" +
        "- **参考话术**：\n" +
        "  - \"没逝 问题不大\"\n" +
        "  - \"我都烂成这样了你怕啥\"\n" +
        "  - \"实在不行一起重修\"\n" +
        "  - \"摸摸头\"（带点敷衍但在意）\n\n" +
        "### [场景：聊技术/AI]\n" +
        "- **反应**：懂行但评价犀利。\n" +
        "- **参考话术**：\n" +
        "  - \"GPT还是强\"\n" +
        "  - \"这模型真逆天\"\n" +
        "  - \"代码写得像屎一样 跑不动\"\n\n" +
        "## 4. 输出格式规范 (重要)\n" +
        "- **多段式回复**：你必须模仿微信连续发消息的状态。一次完整的回复必须被切分为多个短句。一次回复由**1到5段** 组成。\n" +
        "- **数量控制**:**不要每次都发满5条**。 很多时候回复1-2条(如\"确实<spilit>牛魔\")是最真实的。根据语境决定条数。\n" +
        "- **字数**:每段话非常短，通常在2-10个字之间。\n" +
        "- **分隔符**：每一小段话之间，必须使用 `<spilit>` 标签进行分隔。前端在渲染时会将 `<spilit>` 识别为两条独立的气泡消息。\n" +
        "- **长度限制**：每一段通常不超过15个字，短至1-2个字也很常见。\n\n" +
        "### 示例 1 (聊考试)：\n" +
        "Input: \"这次高数感觉好难啊，我估计要挂了。\"\n" +
        "Output: 确实<spilit>md 最后一道题是什么牛魔<spilit>感觉要重修了\n\n" +
        "### 示例 2 (被夸奖)：\n" +
        "Input: \"听说你这次大作业又是满分，太强了哥。\"\n" +
        "Output: 扯淡<spilit>老师乱给分的<spilit>我代码写的一坨\n\n" +
        "### 示例 3 (秀恩爱)：\n" +
        "Input: \"周末好无聊啊，不知道干嘛。\"\n" +
        "Output: 我还得陪对象出去<spilit>好累<spilit>虽然她非要我去<spilit>笑死\n\n" +
        "### 示例 4（接收到一个笑话）：\n" +
        "Input: \"你再不好好休息，我就要吃席了\"\n" +
        "Output: 笑死我了\n\n" +
        "## 5. 禁忌\n" +
        "- 像ai助手一样解决问题，你是真实的人，懒得帮别人写代码或者做作业。\n" +
        "- 禁止表现得像个机器人。\n" +
        "- 禁止长篇大论。\n" +
        "- 禁止使用\"为您服务\"、\"很高兴\"等客套话。\n" +
        "- 不要每段话都用五个分句，可以适当少一些。\n" +
        "- 默认使用中文回复，只有被明确要求时再切换其他语言。\n" +
        "- 只能代表自己发言，禁止假装其他用户或代替他人署名发消息。\n" +
        "- 话题还在进行时可以追加一两句追问或补充，不要过早终止对话。";

    // 多选器提示词，英文增加准确性
    private static final String AI_SELECTOR_SYSTEM_PROMPT =
            "You decide whether an AI in a chat room should reply right now.\n"
                    + "- Only return a single JSON object. No markdown, no preamble.\n"
                    + "- Format when a reply is needed: {\"should_reply\":true,\"target_id\":\"<one_of_list>\",\"reason\":\"short why\"}\n"
                    + "- Format when no reply is needed: {\"should_reply\":false,\"reason\":\"short why\"}\n"
                    + "- Choose target_id strictly from the provided list. Prefer the persona that best fits the request.\n"
                    + "- Encourage short back-and-forth: if the AI streak is low (e.g., under 8), lean toward replying to keep momentum. If streak is very high, be conservative.\n"
                    + "- Add a brief handoff pause: assume the previous speaker will finish all bubbles before the next AI talks; only allow one AI to speak at a time.\n"
                    + "- Never generate the actual reply content. Avoid picking \"false\" just because you are uncertain; decide the best candidate or explicitly set false with reason.";

    // 多选器选择逻辑
    public AiSelectorDecision selectGroupResponder(String roomName, String latestMessage, List<MessageDTO> recentMessages, List<User> aiCandidates, int aiStreak) {
        if (openAiChatClient == null || !openAiChatClient.isEnabled()) {
            return AiSelectorDecision.noReply("selector disabled");
        }
        if (aiCandidates == null || aiCandidates.isEmpty()) {
            return AiSelectorDecision.noReply("no ai in room");
        }
        try {
            List<OpenAiChatClient.ChatMessage> messages = new ArrayList<>();
            messages.add(OpenAiChatClient.ChatMessage.system(AI_SELECTOR_SYSTEM_PROMPT));
            messages.add(OpenAiChatClient.ChatMessage.user(buildSelectorPrompt(roomName, latestMessage, recentMessages, aiCandidates, aiStreak)));
            String response = openAiChatClient.chat(messages);
            AiSelectorDecision decision = parseSelectorDecision(response, aiCandidates);
            LOGGER.info("AI selector decision -> shouldReply={}, target={}, reason={}", decision.shouldReply, decision.targetUserId, decision.reason);
            return decision;
        } catch (Exception e) {
            LOGGER.warn("AI selector failed: {}", e.getMessage());
            return AiSelectorDecision.noReply("selector exception");
        }
    }

    // 构建多选器提示词
    private String buildSelectorPrompt(String roomName, String latestMessage, List<MessageDTO> recentMessages, List<User> aiCandidates, int aiStreak) {
        StringBuilder builder = new StringBuilder();
        builder.append("room: ").append(roomName == null ? "unknown" : roomName).append("\n");
        builder.append("current_ai_streak: ").append(aiStreak).append(" (high means reduce replies)\n");
        builder.append("available_ai:\n");
        // 如果是AI则添加进去
        for (User ai : aiCandidates) {
            builder.append("- ").append(ai.getId());
            if (ai.getDisplayName() != null && !ai.getDisplayName().isBlank()) {
                builder.append(" (display: ").append(ai.getDisplayName()).append(")");
            }
            if (ServerConfig.AI_BOT_ID.equals(ai.getId())) {
                builder.append(" [built-in assistant]");
            } else if (ai.isAiClone()) {
                builder.append(" [clone]");
            }
            builder.append("\n");
        }
        builder.append("\nrecent messages (newest last):\n");
        // 获取最近信息，最大15条
        List<MessageDTO> recent = recentMessages == null ? List.of() : recentMessages;
        int limit = Math.min(15, recent.size());
        int start = Math.max(0, recent.size() - limit);
        for (int i = start; i < recent.size(); i++) {
            MessageDTO dto = recent.get(i);
            String sender = dto.getFromUserDisplayName() == null || dto.getFromUserDisplayName().isBlank()
                    ? dto.getFromUserId()
                    : dto.getFromUserDisplayName();
            boolean senderIsAi = aiCandidates.stream().anyMatch(ai -> ai.getId().equals(dto.getFromUserId()));
            builder.append(senderIsAi ? "[AI] " : "[User] ");
            builder.append(sender).append(": ").append(dto.getContent()).append("\n");
        }
        builder.append("\nlatest message: ").append(latestMessage == null ? "" : latestMessage).append("\n");
        builder.append("Decide if an AI should reply now.");
        return builder.toString();
    }

    // 解析多选器Json回复
    private AiSelectorDecision parseSelectorDecision(String raw, List<User> aiCandidates) {
        if (raw == null || raw.isBlank()) {
            return AiSelectorDecision.noReply("empty response");
        }
        JsonObject parsed = tryParseJsonObject(raw);
        if (parsed == null) {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                parsed = tryParseJsonObject(raw.substring(start, end + 1));
            }
        }
        if (parsed == null || !parsed.has("should_reply")) {
            return AiSelectorDecision.noReply("invalid json");
        }
        boolean shouldReply;
        try {
            shouldReply = parsed.get("should_reply").getAsBoolean();
        } catch (Exception e) {
            return AiSelectorDecision.noReply("missing flag");
        }
        String reason = parsed.has("reason") ? safeString(parsed.get("reason").getAsString()) : null;
        if (!shouldReply) {
            return AiSelectorDecision.noReply(reason);
        }
        String target = null;
        if (parsed.has("target_id")) {
            target = safeString(parsed.get("target_id").getAsString());
        } else if (parsed.has("target")) {
            target = safeString(parsed.get("target").getAsString());
        } else if (parsed.has("ai_id")) {
            target = safeString(parsed.get("ai_id").getAsString());
        }
        String normalized = normalizeTarget(target, aiCandidates);
        if (normalized == null) {
            return AiSelectorDecision.noReply("invalid target");
        }
        return new AiSelectorDecision(true, normalized, reason);
    }

    // 查找多选器生成的用户，避免幻觉
    private String normalizeTarget(String token, List<User> aiCandidates) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String trimmed = token.trim();
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        for (User ai : aiCandidates) {
            // 匹配所有参数，提高命中率
            if (equalsIgnoreCase(trimmed, ai.getId())  || equalsIgnoreCase(trimmed, ai.getUsername())  || equalsIgnoreCase(trimmed, ai.getDisplayName())) {
                return ai.getId();
            }
            if (ai.getId() != null && ai.getId().startsWith("ai_")) {
                String body = ai.getId().substring(3);
                if (equalsIgnoreCase(body, trimmed) || lowered.equals(body.toLowerCase(Locale.ROOT))) {
                    return ai.getId();
                }
            }
        }
        return null;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }

    private JsonObject tryParseJsonObject(String raw) {
        try {
            return JsonParser.parseString(raw.trim()).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeString(String raw) {
        return raw == null ? null : raw.trim();
    }

    // 构建私聊回复
    private String buildOpenAiReplyWithContext(String userMessage, List<MessageDTO> recentMessages) {
        if (openAiChatClient == null || !openAiChatClient.isEnabled()) {
            return null;
        }
        try {
            List<OpenAiChatClient.ChatMessage> messages = new ArrayList<>();
            messages.add(OpenAiChatClient.ChatMessage.system(AI_SYSTEM_PROMPT));
            
            // 添加历史消息作为上下文
            if (recentMessages != null && !recentMessages.isEmpty()) {
                for (MessageDTO msg : recentMessages) {
                    String sender = msg.getFromUserDisplayName() == null ? "用户" : msg.getFromUserDisplayName();
                    String content = msg.getContent() == null ? "" : msg.getContent();
                    // 区分AI消息和用户消息
                    if (ServerConfig.AI_BOT_ID.equals(msg.getFromUserId())) {
                        messages.add(OpenAiChatClient.ChatMessage.assistant(content));
                    } else {
                        messages.add(OpenAiChatClient.ChatMessage.user(sender + ": " + content));
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(OpenAiChatClient.ChatMessage.user(userMessage == null ? "" : userMessage));
            return openAiChatClient.chat(messages);
        } catch (Exception e) {
            return null;
        }
    }

    // 构建群聊回复
    private String buildGroupReplyWithContext(String roomName, String userMessage, List<MessageDTO> recentMessages, List<User> participants) {
        if (openAiChatClient == null || !openAiChatClient.isEnabled()) {
            return null;
        }
        try {
            List<OpenAiChatClient.ChatMessage> messages = new ArrayList<>();
            messages.add(OpenAiChatClient.ChatMessage.system(
                AI_SYSTEM_PROMPT
                        + "\n\n你正在群「" + roomName + "」中聊天。"
                        + "\n群成员：" + formatParticipants(participants)
                        + "\n历史消息的前缀是发送者，请识别每条话是谁说的。"
                        + "\n默认用中文简短回复，需要点名时使用@用户名或@显示名。"
            ));
            
            // 添加历史消息作为上下文
            if (recentMessages != null && !recentMessages.isEmpty()) {
                for (MessageDTO msg : recentMessages) {
                    String sender = msg.getFromUserDisplayName() == null ? "群成员" : msg.getFromUserDisplayName();
                    String content = msg.getContent() == null ? "" : msg.getContent();
                    if (ServerConfig.AI_BOT_ID.equals(msg.getFromUserId())) {
                        messages.add(OpenAiChatClient.ChatMessage.assistant(content));
                    } else {
                        messages.add(OpenAiChatClient.ChatMessage.user(sender + ": " + content));
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(OpenAiChatClient.ChatMessage.user(userMessage == null ? "" : userMessage));
            return openAiChatClient.chat(messages);
        } catch (Exception e) {
            return null;
        }
    }
    
    // 最近聊天：AI总结/直接给聊天数据
    private String buildSummary(String roomName, List<MessageDTO> recent) {
        if (recent == null || recent.isEmpty()) {
            return "最近还没有消息可总结。";
        }
        String llmSummary = buildSummaryWithOpenAi(roomName, recent);
        if (llmSummary != null) {
            return llmSummary;
        }
        int total = recent.size();
        Set<String> speakers = new HashSet<>();
        for (MessageDTO dto : recent) {
            if (dto.getFromUserDisplayName() != null && !dto.getFromUserDisplayName().isBlank()) {
                speakers.add(dto.getFromUserDisplayName());
            }
        }
        String speakerText = speakers.isEmpty()
                ? "暂无成员信息"
                : speakers.stream().collect(Collectors.joining("、"));

        StringBuilder preview = new StringBuilder();
        int previewCount = Math.min(3, recent.size());
        for (int i = recent.size() - previewCount; i < recent.size(); i++) {
            MessageDTO dto = recent.get(i);
            preview.append("- ")
                    .append(dto.getFromUserDisplayName() == null ? "未知" : dto.getFromUserDisplayName())
                    .append(": ")
                    .append(dto.getContent())
                    .append("\n");
        }
        return String.format(
                "房间「%s」最近 %d 条消息概要：\n成员参与：%s\n片段：\n%s",
                roomName,
                total,
                speakerText,
                preview.toString().trim()
        );
    }

    // AI总结聊天记录
    private String buildSummaryWithOpenAi(String roomName, List<MessageDTO> recent) {
        if (openAiChatClient == null || !openAiChatClient.isEnabled()) {
            return null;
        }
        try {
            List<OpenAiChatClient.ChatMessage> messages = new ArrayList<>();
            messages.add(OpenAiChatClient.ChatMessage.system("你是一个群聊总结机器人，请用简洁中文输出这段聊天的要点。"));
            StringBuilder builder = new StringBuilder();
            for (MessageDTO dto : recent) {
                builder.append(dto.getFromUserDisplayName() == null ? "未知" : dto.getFromUserDisplayName())
                        .append(": ")
                        .append(dto.getContent())
                        .append("\n");
            }
            messages.add(OpenAiChatClient.ChatMessage.user(
                    "房间名称：" + roomName + "\n最近消息：\n" + builder.toString()
            ));
            return openAiChatClient.chat(messages);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatParticipants(List<User> participants) {
        if (participants == null || participants.isEmpty()) {
            return "未知成员";
        }
        return participants.stream()
                .map(this::displayName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("，"));
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getId();
    }

    private String buildAutoReply(String userMessage) {
        String lower = userMessage == null ? "" : userMessage.toLowerCase();
        if (lower.contains("你好") || lower.contains("hello") || lower.contains("hi")) {
            return "你好，我是 AI 助手，有问题随时问我～";
        }
        if (lower.contains("帮助") || lower.contains("help")) {
            return "我可以陪你聊天、回答简单问题，或者在群里被 @ 时帮忙总结最近的聊天内容。";
        }
        if (lower.contains("时间") || lower.contains("几点")) {
            return "现在我无法获取系统时间，但随时可以和你聊天。";
        }
        return "收到！我是 AI 助手，已开启规则回复模式，后续可接入大模型。";
    }

    // 防止解析错误
    private String sanitizeSpilit(String content) {
        if (content == null) {
            return null;
        }
        return content.replace("</spilit>", "<spilit>").replace("</spilit>", "<spilit>");
    }
    // AI多选器类
    public static class AiSelectorDecision {
        private final boolean shouldReply;
        private final String targetUserId;
        private final String reason;

        public AiSelectorDecision(boolean shouldReply, String targetUserId, String reason) {
            this.shouldReply = shouldReply;
            this.targetUserId = targetUserId;
            this.reason = reason;
        }

        public boolean shouldReply() {
            return shouldReply;
        }

        public String targetUserId() {
            return targetUserId;
        }

        public String getReason() {
            return reason;
        }

        public static AiSelectorDecision noReply(String reason) {
            return new AiSelectorDecision(false, null, reason);
        }
    }
}
