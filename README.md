# Vesper Chat

> 使用 Java 24 + Gradle + Java-WebSocket 搭建的AI在线聊天室，使用面向对象分层设计以及浏览器原生 WebSocket 实时通信。

## ✨ 功能特性
本项目为一个轻量级 Java WebSocket 聊天室，包含：
- 用户注册 / 登录
- 好友搜索、发送好友申请、添加好友
- 私聊与群聊（房间创建/搜索/加入/历史消息回放）
- 发送文件（图片可预览）和 emoji
- 聊天界面进行小游戏互动（扔番茄、掷骰子、石头剪刀布）
- 树洞（匿名贴）
- 朋友圈（可发图，最多九张），支持点赞、文字评论
- 自定义AI分身（支持自定义昵称、头像、提示词设定语言风格），也可以总结用户聊天习惯生成提示词，可在广场添加他人创建的分身
- AI助手在私聊和群聊中可以主动或被动回复，通过AI多选器实现自动智能回复


## 🤝 Team Members


<!-- 🎨 居中容器 -->

<div align="center">

<table>
  <tr>
    <td align="center" width="120">
      <a href="https://github.com/lixu10">
        <img src="https://wsrv.nl/?url=github.com/lixu10.png&w=160&h=160&mask=circle" width="80" alt="lixu10"/>
        <br />
        <sub><b>lixu10</b></sub>
      </a>
    </td>
    <td align="center" width="120">
      <a href="https://github.com/MontesquieuE">
        <img src="https://wsrv.nl/?url=github.com/MontesquieuE.png&w=160&h=160&mask=circle" width="80" alt="MontesquieuE"/>
        <br />
        <sub><b>MontesquieuE</b></sub>
      </a>
    </td>
    <td align="center" width="120">
      <a href="https://github.com/pxrmiraitowa">
        <img src="https://wsrv.nl/?url=github.com/pxrmiraitowa.png&w=160&h=160&mask=circle" width="80" alt="pxrmiraitowa"/>
        <br />
        <sub><b>pxrmiraitowa</b></sub>
      </a>
    </td>
    <td align="center" width="120">
      <a href="https://github.com/Camellia-0110">
        <img src="https://wsrv.nl/?url=github.com/Camellia-0110.png&w=160&h=160&mask=circle" width="80" alt="Camellia-0110"/>
        <br />
        <sub><b>Camellia-0110</b></sub>
      </a>
    </td>
  </tr>
</table>

</div>

## ☁️项目运行环境

**运行/开发环境**：

- 操作系统：Windows / macOS / Linux 任意支持 Java 的系统
- JDK： JDK 21 及以上，建议版本为JDK 24。
- Gradle：本地可使用已安装的 Gradle 或使用项目 wrapper（若存在），通常使用 `gradle` 命令即可。推荐版本 8.14 。

**第三方API**：

- OpenAI格式接口、API Key：用于调用大模型完成AI功能。
- Lsky图床API：用于头像、图片上传：[lsky-org/lsky-pro: 兰空图床(Lsky Pro)](https://github.com/lsky-org/lsky-pro)

**访问**：

- 支持 Google Chrome、Microsoft Edge、Firefox、Safari 等主流浏览器的**最新版本**访问，不建议使用 IE 浏览器。

## 🚀 快速开始

### 1. 环境变量

环境变量修改位于`src\main\java\com\example\chat\config\ServerConfig.java` ，其中建议配置的有：

- `WS_PORT`：WebSocket服务器端口，默认8881；
- `IMG_API_BASE`：图片上传API地址，需要为lsky图床API格式；
- `IMG_API_EMAIL`：图片上传API，lsky系统用户名；
- `IMG_API_PASSWORD`：图片上传API，lsky系统密码；
- `OPENAI_API_KEY`：OpenAI 或兼容服务的 API Key（不配置时 AI 回退为本地规则回复）
- `OPENAI_BASE_URL`：可选，OpenAI 兼容服务地址（例如自建或第三方兼容 API）
- `OPENAI_MODEL`：模型名，默认可使用 `gpt-5.2` 或项目中指定的默认值

详细配置文件如下：

```java
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
/** 头像与图片上传API。 */
public static final String IMG_API_BASE = System.getenv().getOrDefault("IMG_API_BASE", "https://yourdomain/api/v1");
public static final String IMG_API_EMAIL = System.getenv().getOrDefault("IMG_API_EMAIL", "name@youremail");
public static final String IMG_API_PASSWORD = System.getenv().getOrDefault("IMG_API_PASSWORD", "yourpassword");
/** AI 群聊总结时读取的消息上限。 */
public static final int AI_RECENT_SUMMARY_LIMIT = 30;
/** 树洞默认返回的最大条数 */
public static final int TREE_HOLE_RECENT_LIMIT = 100;

/** OpenAI API Key，读取环境变量 OPENAI_API_KEY。 */
public static final String OPENAI_API_KEY = System.getenv().getOrDefault("OPENAI_API_KEY", "sk-kfcvme50");
/** OpenAI Base URL，默认官方地址，可指向第三方兼容服务。 */
public static final String OPENAI_BASE_URL = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com");
/** OpenAI 模型名，默认 gpt-4o-mini。 */
public static final String OPENAI_MODEL = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-5.2");
```

### 2. 本地开发部署

环境变量（可选，但在启用 AI 功能时必须配置）：

- `OPENAI_API_KEY`：OpenAI 或兼容服务的 API Key（不配置时 AI 回退为本地规则回复）
- `OPENAI_BASE_URL`：可选，OpenAI 兼容服务地址（例如自建或第三方兼容 API）
- `OPENAI_MODEL`：模型名，默认可使用 `gpt-4.1-mini` 或项目中指定的默认值

构建与运行：

```powershell
gradle build    # 编译并运行测试
gradle run      # 启动 WebSocket 服务器
gradle test     # 运行单元测试
```

启动后默认监听地址：`ws://localhost:8080`（端口来自 `ServerConfig.WS_PORT`，可在 `src/main/java/com/example/chat/config/ServerConfig.java` 中修改）。

前端使用方式：打开 `src/main/resources/static/index.html`（本地文件直接用浏览器打开即可），页面会尝试连接服务端的 `ws://localhost:8080`，若后端不在本地或端口不同，请在页面中修改连接地址或启用静态服务器并确保跨域设置正确。

### 3. 使用Docker部署

1. 完成上述配置文件修改、本地Gradle构建完整过程。成功构建后在 `build\libs`文件夹下会生成`chat-server.jar` 文件。

2. 修改项目根目录下的`Dockerfile`：

   ```dockerfile
   FROM container-registry.oracle.com/java/openjdk:21
   
   WORKDIR /app
   COPY chat-server.jar app.jar
   EXPOSE 8881
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

3. 确保 Docker 已正确安装并启动：

    ```bash
    # 检查 Docker 状态
    sudo systemctl status docker
    # 如果未启动，则启动 Docker
    sudo systemctl start docker
    ```

4. 在服务器端新建项目目录（例如`chatroom`），并且在项目目录下创建`backend`、`frontend`文件夹。

   ```bash
   mkdir chatroom
   cd chatroom
   mkdir backend
   mkdir frontend
   ```

   接着，把 `chat-server.jar` 和写好的 `Dockerfile`，这两个文件发送到服务器的 `backend` 文件夹。

   创建Json数据文件，用于数据持久化存储：

   ```bash
   # 先在宿主机创建快照文件
   mkdir -p /your/host/path/chat-data
   touch /your/host/path/chat-data/data-backup.json
   ```

5. 启动后端：

    ``` bash
    cd backend
    
    # 构建镜像
    docker build -t my-chat-server .
    # 运行容器
    docker run -d --name chat-app --restart always \
      -p 8881:8881 \
      -v /your/host/path/chat-data/data-backup.json:/app/data-backup.json \
      my-chat-server
    ```

    现在后端已经成功运行了！若需要停止容器：

    ```bash
    docker rm -f chat-app
    ```

6. 前端使用Ngnix作为网页服务器。首先，把前端的 `html/css/js` 发送到服务器的 `frontend` 文件夹。

    修改`app.js`第6-7行：

    ```javascript
    const WS_PORT = 8881; // 修改为后端端口
    const WS_HOST = 127.0.0.1; // 填写你服务器的ip
    ```

    之后修改 Nginx 配置，把 `root` 指向 `/home/lighthouse/chat-project/frontend`。

此时访问前端Ngnix前端绑定的域名即可访问了！

## 🛠 技术栈
| 分类 | 技术 | 说明 |
| --- | --- | --- |
| 语言 & 构建 | Java 24、Gradle `application` 插件 | 纯 Java 程序，无需 Servlet 容器 |
| WebSocket | [`org.java-websocket:Java-WebSocket`](https://github.com/TooTallNate/Java-WebSocket) | 轻量服务器实现 onOpen/onMessage/onClose |
| JSON | Gson | 统一消息 envelope 序列化/反序列化 |
| 前端 | 原生 HTML/CSS/JS | 状态管理 + WebSocket 事件处理 |

## 🗂️ 代码结构
```
chatroom/
├── build.gradle                    # 依赖 & UTF-8 JVM 参数
├── settings.gradle
├── src/main/java/com/example/chat
│   ├── Main.java                   # 程序入口
│   ├── config/ServerConfig.java    # 端口/消息条数配置
│   ├── util/                       # JsonUtil, IdGenerator
│   ├── model/                      # User/ChatRoom/Message/FriendRequest 等实体
│   ├── dto/                        # UserDTO/MessageDTO/RoomDTO/FriendRequestDTO
│   ├── repository/                 # 内存仓库（用户/房间/消息/好友申请）
│   ├── service/                    # Auth/User/Friend/Room/Chat 业务逻辑
│   └── websocket/                  # ChatWebSocketServer, ClientConnection
└── src/main/resources/static       # 前端静态资源
    ├── index.html
    ├── styles.css
    └── app.js
```

## 🔌 WebSocket 消息（部分）
| Type | 方向 | 说明 |
| --- | --- | --- |
| `AUTH_REGISTER` / `AUTH_LOGIN` | C→S | 注册 / 登录，返回操作结果与用户信息 |
| `FRIEND_SEARCH` | C→S | 根据关键字搜索用户 |
| `FRIEND_ADD` | C→S | 发送好友申请，后台仅记录待审批状态 |
| `FRIEND_REQUEST_LIST` / `FRIEND_REQUEST_RESPOND` | 双向 | 拉取/处理好友申请，成功后自动推送好友列表 |
| `FRIEND_LIST` | 双向 | 获取/推送好友列表（登录、审批后自动刷新）|
| `PRIVATE_OPEN` / `PRIVATE_MESSAGE` | 双向 | 获取私聊房间 & 发送私聊消息 |
| `ROOM_CREATE` / `ROOM_JOIN` / `ROOM_SEARCH` | 双向 | 创建群聊、搜索房间、加入房间均可即时推送房间列表 |
| `ROOM_MESSAGE` / `FILE_MESSAGE` | 双向 | 群聊文本/文件链接消息，统一广播 `NEW_MESSAGE` |
| `PROFILE_GET` / `PROFILE_UPDATE` / `PROFILE_VIEW` | 双向 | 获取/修改自身资料；查看好友资料 |

所有消息统一 envelope：
```json
{ "type": "ROOM_MESSAGE", "payload": { ... } }
```



