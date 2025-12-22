package com.example.chat.config;


public final class ServerConfig {
    /** WebSocket 服务器端口，部署时可统一调整。 */
    public static final int WS_PORT = 8881;
    /** 单个房间默认返回的最近消息数量。 */
    public static final int RECENT_MESSAGE_LIMIT = 50;
    /** AI 助手的固定用户 ID。 */
    public static final String AI_BOT_ID = "ai-bot";
    /** AI 助手默认展示名称。 */
    public static final String AI_BOT_DISPLAY_NAME = "小爱同学";
    /** AI 助手用户名，便于搜索。 */
    public static final String AI_BOT_USERNAME = "ai-bot";
    /** AI 助手签名文案。 */
    public static final String AI_BOT_SIGNATURE = "你好我是小爱同学，欢迎随时和我聊天";
    /** AI 助手头像 URL。 */
    public static final String AI_BOT_AVATAR = "https://2bpic.oss-cn-beijing.aliyuncs.com/2025/12/04/6930f7d6d5a18.jpg";
    /** 用户默认头像 URL。 */
    public static final String DEFAULT_AVATAR = "https://2bpic.oss-cn-beijing.aliyuncs.com/2025/12/04/6930f7a2c12fc.png";
    /** Lsky图床API。 */
    public static final String IMG_API_BASE = System.getenv().getOrDefault("IMG_API_BASE", "https://yourdomain/api/v1");  // 需要填入自己的图床api地址
    public static final String IMG_API_EMAIL = System.getenv().getOrDefault("IMG_API_EMAIL", "114514@onlyfans.com");  // 自己的图床账号
    public static final String IMG_API_PASSWORD = System.getenv().getOrDefault("IMG_API_PASSWORD", "yourpassword"); // 自己的图床密码
    /** AI 群聊总结时读取的消息上限。 */
    public static final int AI_RECENT_SUMMARY_LIMIT = 30;
    /** 树洞默认返回的最大条数 */
    public static final int TREE_HOLE_RECENT_LIMIT = 100;

    /** OpenAI API Key */
    public static final String OPENAI_API_KEY = System.getenv().getOrDefault("OPENAI_API_KEY", "sk-KFCVme50");
    /** OpenAI Base URL，默认官方地址，可指向第三方兼容服务。 */
    public static final String OPENAI_BASE_URL = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com");
    /** OpenAI 模型名 */
    public static final String OPENAI_MODEL = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-5.1-mini");

    private ServerConfig() {
    }
}
