(() => {
    if (window.lucide) {
        lucide.createIcons();
    }

    const WS_PORT = 8881;
    const WS_HOST = window.CHAT_WS_HOST || window.location.hostname || 'localhost'; // 部署时在此切换 WebSocket 服务器
    const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    const DEFAULT_BURN_DELAY = 10; // seconds
    let IMG_API_BASE = window.IMG_API_BASE || '';
    let IMG_API_EMAIL = window.IMG_API_EMAIL || '';
    let IMG_API_PASSWORD = window.IMG_API_PASSWORD || '';
    const TOMATO_ACTION = 'TOMATO';
    const TOMATO_IMG =
        "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 80 80'><circle cx='40' cy='40' r='30' fill='%23e53935'/><circle cx='35' cy='35' r='6' fill='%23f8c9c1'/><path d='M28 22c6 4 16 4 24 0-4-4-10-6-12-6s-8 2-12 6z' fill='%23338845'/><path d='M42 18c1 5-3 8-6 9 5-1 9 1 11 3-1-5 1-9 3-10-4 0-6-1-8-2z' fill='%23439959'/></svg>";
    const TOMATO_SPLAT_IMG =
        "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'><path d='M50 10c7 8 14 6 22 2-4 8-2 14 6 20-10 0-14 6-14 16 0 10-12 14-20 10-8 6-20 0-20-10 0-10-4-16-14-16 8-6 10-12 6-20 8 4 15 6 22-2 4 6 8 8 12 8s8-2 12-8z' fill='%23e53935' stroke='%23c62828' stroke-width='3'/></svg>";
    const state = {
        socket: null,
        reconnectTimer: null,
        currentUser: null,
        rooms: {},
        roomOrder: [],
        messagesByRoom: {},
        currentRoomId: null,
        friends: [],
        friendSearchResults: [],
        friendRequests: [],
        roomSearchResults: [],
        pendingRegister: null,
        lastLoginPayload: null,
        moments: [],
        momentDraftImages: [],
        treeHoleMessages: [],
        aiClonesMine: [],
        aiClonesAll: [],
        currentMode: 'chat',
        lastBurnDebug: null,
        currentQuote: null,
        imgApiToken: null,
        roomEnterSerial: 0,
        momentFilterUserId: null,
        viewProfileUser: null,
        selectMode: false,
        selectedMessageIds: new Set(),
        forwardTargets: new Set()
    };

    const refs = {


        // 在 app.js 的 const refs = { ... } 中添加以下内容：
        manageRoomBtn: document.getElementById('manage-room-btn'),
        manageRoomModal: document.getElementById('manage-room-modal'),
        manageRoomName: document.getElementById('manage-room-name'),
        manageRoomDesc: document.getElementById('manage-room-desc'),
        manageRoomAvatar: document.getElementById('manage-room-avatar'),
        manageRoomPublic: document.getElementById('manage-room-public'),
        manageRoomMembers: document.getElementById('manage-room-members'),
        manageRoomAddBtn: document.getElementById('manage-room-add-btn'),
        manageRoomSave: document.getElementById('manage-room-save'),
        manageRoomCancel: document.getElementById('manage-room-cancel'),
        manageRoomHint: document.getElementById('manage-room-hint'),
        manageRoomAddSelect: document.getElementById('manage-room-add-user'),


        loginPanel: document.getElementById('login-panel'),
        chatPanel: document.getElementById('chat-panel'),
        loginForm: document.getElementById('login-form'),
        loginHint: document.getElementById('login-hint'),
        usernameInput: document.getElementById('login-username'),
        passwordInput: document.getElementById('login-password'),
        registerBtn: document.getElementById('register-btn'),
        friendSearchBtn: document.getElementById('friend-search-btn'),
        friendKeyword: document.getElementById('friend-keyword'),
        friendSearchList: document.getElementById('friend-search-result'),
        friendList: document.getElementById('friend-list'),
        friendRequestList: document.getElementById('friend-request-list'),
        refreshFriends: document.getElementById('refresh-friends'),
        refreshFriendRequests: document.getElementById('refresh-friend-requests'),
        refreshAiClones: document.getElementById('refresh-ai-clones'),
        roomList: document.getElementById('room-list'),
        roomSearchList: document.getElementById('room-search-result'),
        roomSearchInput: document.getElementById('room-search-keyword'),
        roomSearchBtn: document.getElementById('room-search-btn'),
        aiCloneIdInput: document.getElementById('ai-clone-id'),
        aiCloneNameInput: document.getElementById('ai-clone-name'),
        aiClonePromptInput: document.getElementById('ai-clone-prompt'),
        aiCloneSelfPromptBtn: document.getElementById('ai-clone-self-prompt'),
        aiCloneCreateBtn: document.getElementById('ai-clone-create'),
        aiCloneHint: document.getElementById('ai-clone-hint'),
        aiCloneAvatarInput: document.getElementById('ai-clone-avatar'),
        aiCloneModal: document.getElementById('ai-clone-modal'),
        aiCloneModalCancel: document.getElementById('ai-clone-cancel'),
        openAiCloneModalBtn: document.getElementById('open-ai-clone-modal'),
        aiCloneAvatarUploadBtn: document.getElementById('ai-clone-avatar-upload'),
        aiCloneAvatarFile: document.getElementById('ai-clone-avatar-file'),
        aiCloneMineList: document.getElementById('ai-clone-mine'),
        aiCloneAllList: document.getElementById('ai-clone-all'),
        chatTitle: document.getElementById('chat-title'),
        chatSubtitle: document.getElementById('chat-subtitle'),
        modeChatBtn: document.getElementById('mode-chat-btn'),
        modeTreeHoleBtn: document.getElementById('mode-treehole-btn'),
        chatSection: document.getElementById('chat-section'),
        treeHoleSection: document.getElementById('tree-hole-section'),
        treeHoleList: document.getElementById('tree-hole-list'),
        treeHoleForm: document.getElementById('tree-hole-form'),
        treeHoleInput: document.getElementById('tree-hole-input'),
        treeHoleRefresh: document.getElementById('tree-hole-refresh'),
        momentsPanel: document.getElementById('moments-panel'),
        momentsList: document.getElementById('moments-list'),
        momentForm: document.getElementById('moment-form'),
        momentInput: document.getElementById('moment-input'),
        momentsRefresh: document.getElementById('moments-refresh'),
        momentUploadBtn: document.getElementById('moment-upload-btn'),
        momentUploadInput: document.getElementById('moment-upload-input'),
        momentUploadPreview: document.getElementById('moment-upload-preview'),
        momentUploadHint: document.getElementById('moment-upload-hint'),
        messageList: document.getElementById('message-list'),
        messageForm: document.getElementById('message-form'),
        messageInput: document.getElementById('message-input'),
        burnToggle: document.getElementById('burn-mode-toggle'),
        sendFileBtn: document.getElementById('send-file-btn'),
        fileInput: document.getElementById('file-input'),
        emojiBtn: document.getElementById('emoji-btn'),
        quotePreview: document.getElementById('quote-preview'),
        quoteSender: document.getElementById('quote-sender'),
        quoteText: document.getElementById('quote-text'),
        quoteClearBtn: document.getElementById('quote-clear-btn'),
        gameBtn: document.getElementById('game-btn'),
        gameMenu: document.getElementById('game-menu'),
        openRoomModalBtn: document.getElementById('open-room-modal'),
        roomModal: document.getElementById('room-modal'),
        closeRoomModalBtn: document.getElementById('close-room-modal'),
        createRoomConfirm: document.getElementById('create-room-confirm'),
        roomNameInput: document.getElementById('room-name'),
        roomMembersBox: document.getElementById('room-members'),
        currentAvatar: document.getElementById('current-avatar'),
        currentName: document.getElementById('current-name'),
        currentSignature: document.getElementById('current-signature'),
        editProfileBtn: document.getElementById('edit-profile-btn'),
        profileModal: document.getElementById('profile-modal'),
        profileDisplayName: document.getElementById('profile-displayName'),
        profileAvatar: document.getElementById('profile-avatar'),
        profileSignature: document.getElementById('profile-signature'),
        profileAvatarUploadBtn: document.getElementById('profile-avatar-upload'),
        profileAvatarFile: document.getElementById('profile-avatar-file'),
        profileSaveBtn: document.getElementById('profile-save'),
        profileCancelBtn: document.getElementById('profile-cancel'),
        viewProfileModal: document.getElementById('view-profile-modal'),
        viewProfileName: document.getElementById('view-profile-name'),
        viewProfileUsername: document.getElementById('view-profile-username'),
        viewProfileSignature: document.getElementById('view-profile-signature'),
        viewProfileAvatar: document.getElementById('view-profile-avatar'),
        viewProfileClose: document.getElementById('view-profile-close'),
        viewProfileMoments: document.getElementById('view-profile-moments'),
        settingsBtn: document.getElementById('settings-btn'),
        settingsMenu: document.getElementById('settings-menu'),
        logoutBtn: document.getElementById('logout-btn'),
        // 在 app.js 的 const refs = { ... } 中添加：
aiWelcomePlaceholder: document.getElementById('ai-welcome-placeholder')
    };

    const emojiMenu = document.createElement('div');
    emojiMenu.className = 'card emoji-menu hidden';
    emojiMenu.innerHTML = ['😀', '😂', '🥳', '😎', '❤️', '👍', '🎉', '🤖','']
        .map(e => `<button class="icon-btn" data-emoji="${e}">${e}</button>`)
        .join('');
    document.body.appendChild(emojiMenu);

    const contextMenu = document.createElement('div');
    contextMenu.id = 'message-context-menu';
    contextMenu.className = 'context-menu hidden';
    contextMenu.innerHTML = `
        <button type="button" data-action="copy">复制</button>
        <button type="button" data-action="quote">引用</button>
        <button type="button" data-action="select">多选</button>
        <button type="button" data-action="recall" class="danger">撤回</button>
    `;
    document.body.appendChild(contextMenu);
    let contextMenuMessage = null;

    const multiSelectBar = document.createElement('div');
    multiSelectBar.id = 'multi-select-bar';
    multiSelectBar.className = 'multi-select-bar hidden';
    multiSelectBar.innerHTML = `
        <div class="multi-select-info">已选 <span class="count">0</span> 条</div>
        <div class="multi-select-actions">
            <button type="button" class="ghost-btn" data-action="choose-targets">选择目标</button>
            <button type="button" class="ghost-btn" data-action="forward-each">逐条转发</button>
            <button type="button" class="primary-btn" data-action="forward-combine">合并转发</button>
            <button type="button" class="icon-btn small" data-action="cancel-select" title="退出多选">
                <i data-lucide="x"></i>
            </button>
        </div>
    `;
    document.body.appendChild(multiSelectBar);
    if (window.lucide) {
        lucide.createIcons();
    }

    const forwardTargetModal = document.createElement('div');
    forwardTargetModal.id = 'forward-target-modal';
    forwardTargetModal.className = 'modal hidden';
    forwardTargetModal.innerHTML = `
        <div class="modal-content forward-target-content">
            <h3>选择转发目标</h3>
            <p class="muted">可多选房间/私聊，未选择则默认当前房间</p>
            <div class="forward-target-list"></div>
            <div class="modal-actions">
                <button type="button" class="ghost-btn" data-action="cancel">取消</button>
                <button type="button" class="primary-btn" data-action="confirm">确认</button>
            </div>
        </div>
    `;
    document.body.appendChild(forwardTargetModal);

    const forwardDetailModal = document.createElement('div');
    forwardDetailModal.id = 'forward-detail-modal';
    forwardDetailModal.className = 'modal hidden';
    forwardDetailModal.innerHTML = `
        <div class="modal-content forward-detail-content">
            <div class="forward-detail-header">
                <div class="forward-detail-title"></div>
                <button type="button" class="icon-btn small" data-action="close-forward-detail" title="关闭">
                    <i data-lucide="x"></i>
                </button>
            </div>
            <div class="forward-detail-sub muted">聊天记录卡片</div>
            <div class="forward-detail-list"></div>
        </div>
    `;
    document.body.appendChild(forwardDetailModal);

    if (window.lucide) {
        lucide.createIcons();
    }

    function connect() {
        const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        state.socket = new WebSocket(`${protocol}://${WS_HOST}:${WS_PORT}`);
        state.socket.onopen = () => {
            refs.loginHint.textContent = '连接成功，请登录';
            clearTimeout(state.reconnectTimer);
            send('CONFIG_GET');
        };
        state.socket.onclose = () => {
            if (refs.loginPanel.classList.contains('hidden')) {
                alert('连接已断开，请刷新页面重新登录');
            } else {
                refs.loginHint.textContent = '连接失败，2 秒后自动重试...';
            }
            state.reconnectTimer = setTimeout(connect, 2000);
        };
        state.socket.onerror = () => (refs.loginHint.textContent = '连接异常，请稍后再试');
        state.socket.onmessage = handleServerMessage;
    }

    function handleServerMessage(event) {
        const { type, payload } = JSON.parse(event.data);
        switch (type) {
            case 'CONFIG_RESULT':
                applyImgApiConfig(payload);
                break;



            // 在 handleServerMessage 的 switch (type) 内部添加以下 case：

            case 'ROOM_UPDATE_RESULT':
                if (payload.success && payload.room) {
                    handleRoomData(payload.room);
                    toggleManageRoomModal(false);
                } else {
                    alert(payload.errorMessage || '更新群资料失败');
                }
                break;
            case 'ROOM_INVITE_RESULT':
                if (payload.success && payload.room) {
                    handleRoomData(payload.room);
                } else {
                    alert(payload.errorMessage || '拉人失败');
                }
                break;
            case 'ROOM_REMOVE_MEMBER_RESULT':
                if (payload.success && payload.room) {
                    handleRoomData(payload.room);
                } else {
                    alert(payload.errorMessage || '移除成员失败');
                }
                break;
            case 'ROOM_UPDATED':
                if (payload.room) {
                    handleRoomData(payload.room);
                }
                break;
            case 'ROOM_REMOVED':
                if (payload.roomId) {
                    removeRoomLocal(payload.roomId);
                }
                break;




            case 'AUTH_REGISTER_RESULT':
                if (payload.success) {
                    refs.loginHint.textContent = '注册成功，正在为你登录...';
                    const creds = state.pendingRegister || {
                        username: refs.usernameInput.value.trim(),
                        password: refs.passwordInput.value
                    };
                    state.pendingRegister = null;
                    if (creds.username && creds.password) {
                        state.lastLoginPayload = creds;
                        send('AUTH_LOGIN', creds);
                    } else {
                        refs.loginHint.textContent = '注册成功，请输入密码完成登录';
                    }
                } else {
                    refs.loginHint.textContent = payload.errorMessage || '注册失败，请重试';
                }
                break;
            case 'AUTH_LOGIN_RESULT':
                if (payload.success) {
                    state.currentUser = payload.user;
                    enterChatMode();
                    requestInitialData();
                } else {
                    refs.loginHint.textContent = payload.errorMessage || '登录失败，请检查账号密码';
                }
                break;
            case 'FRIEND_SEARCH_RESULT':
                state.friendSearchResults = payload.results || [];
                renderFriendSearchResults();
                break;
            case 'FRIEND_ADD_RESULT':
                if (payload.success) {
                    alert('好友申请已发送，等待对方确认');
                } else {
                    alert(payload.errorMessage || '发送好友请求失败');
                }
                break;
            case 'FRIEND_LIST_RESULT':
                state.friends = payload.friends || [];
                renderFriends();
                renderRoomMemberOptions();
                break;
            case 'FRIEND_REQUEST_LIST_RESULT':
                state.friendRequests = payload.requests || [];
                renderFriendRequests();
                break;
            case 'FRIEND_REQUEST_RESPOND_RESULT':
                if (!payload.success) {
                    alert(payload.errorMessage || '处理好友请求失败');
                }
                break;
            case 'PRIVATE_OPEN_RESULT':
                handleRoomData(payload.room, payload.recentMessages);
                setCurrentRoom(payload.room.id);
                break;
            case 'ROOM_LIST_RESULT':
                resetRooms();
                (payload.rooms || []).forEach(room => handleRoomData(room));
                break;
            case 'ROOM_CREATE_RESULT':
                if (payload.success) {
                    handleRoomData(payload.room, payload.recentMessages);
                    setCurrentRoom(payload.room.id);
                    toggleRoomModal(false);
                } else {
                    alert(payload.errorMessage || '创建房间失败');
                }
                break;
            case 'ROOM_JOIN_RESULT':
                if (payload.success) {
                    handleRoomData(payload.room, payload.recentMessages);
                    setCurrentRoom(payload.room.id);
                } else {
                    alert(payload.errorMessage || '加入房间失败');
                }
                break;
            case 'ROOM_SEARCH_RESULT':
                state.roomSearchResults = payload.rooms || [];
                renderRoomSearchResults();
                break;
            case 'NEW_MESSAGE':
                if (payload.message) {
                    normalizeTomatoTarget(payload.message);
                    console.log('[ws-recv]', {
                        type,
                        messageId: payload.message?.id,
                        messageType: payload.message?.messageType,
                        burnDelay: payload.message?.burnDelay
                    });
                    appendMessage(payload.message);
                    if (
                        getMessageType(payload.message) === 'ACTION' &&
                        (payload.message?.content || '').toUpperCase() === TOMATO_ACTION
                    ) {
                        const inCurrentRoom =
                            state.currentMode === 'chat' && state.currentRoomId === payload.message.roomId;
                        // 更激进：只要是番茄且在当前房间，且不是自己给自己，就播放
                        const isFromMe = state.currentUser && payload.message.fromUserId === state.currentUser.id;
                        if (inCurrentRoom && !isFromMe) {
                            playTomatoAnimation(payload.message);
                        } else if (inCurrentRoom && isFromMe) {
                            // 自己扔的，维持原有体验
                            playTomatoAnimation(payload.message);
                        }
                    }
                }
                break;
            case 'MOMENT_TIMELINE':
                state.moments = payload.moments || [];
                renderMoments();
                break;
            case 'MOMENT_NEW':
            case 'MOMENT_UPDATED':
                if (payload.moment) {
                    upsertMoment(payload.moment);
                }
                break;
            // case 'MOMENT_POST_RESULT':
            //     if (!payload.success) {
            //         alert(payload.errorMessage || '发布动态失败');
            //     } else if (refs.momentInput) {
            //         refs.momentInput.value = '';
            //     }
            //     break;
            case 'MOMENT_POST_RESULT':
                if (!payload.success) {
                    alert(payload.errorMessage || '发布动态失败');
                } else {
                    // 1. 清空文字输入框
                    if (refs.momentInput) {
                        refs.momentInput.value = '';
                    }
                    // 2. [新增] 清空已选图片的状态数组
                    state.momentDraftImages = []; 
                    // 3. [新增] 调用渲染函数，清空界面上的预览图和重置计数
                    renderMomentDraftImages();
                }
                break;
            case 'MOMENT_LIKE_RESULT':
            case 'MOMENT_COMMENT_RESULT':
                if (payload.success && payload.moment) {
                    upsertMoment(payload.moment);
                } else if (!payload.success) {
                    alert(payload.errorMessage || '操作失败');
                }
                break;
            case 'TREE_HOLE_LIST':
                state.treeHoleMessages = payload.messages || [];
                renderTreeHoleMessages();
                break;
            case 'TREE_HOLE_NEW':
                if (payload.message) {
                    state.treeHoleMessages.push(payload.message);
                    renderTreeHoleMessages();
                }
                break;
            case 'TREE_HOLE_POST_RESULT':
                if (!payload.success) {
                    alert(payload.errorMessage || '匿名发布失败');
                } else if (refs.treeHoleInput) {
                    refs.treeHoleInput.value = '';
                }
                break;
            case 'AI_CLONE_LIST_RESULT':
                state.aiClonesMine = payload.mine || [];
                state.aiClonesAll = payload.all || [];
                renderAiClones();
                break;
            case 'AI_CLONE_CREATE_RESULT':
                if (payload.success) {
                    if (refs.aiCloneHint) refs.aiCloneHint.textContent = '发布成功，已为你自动加好友';
                    if (refs.aiCloneIdInput) refs.aiCloneIdInput.value = '';
                    if (refs.aiCloneNameInput) refs.aiCloneNameInput.value = '';
                    if (refs.aiClonePromptInput) refs.aiClonePromptInput.value = '';
                    send('AI_CLONE_LIST');
                    toggleAiCloneModal(false);
                } else if (refs.aiCloneHint) {
                    refs.aiCloneHint.textContent = payload.errorMessage || '发布失败，请稍后再试';
                }
                break;
            case 'AI_CLONE_SELF_PROMPT_RESULT':
                if (refs.aiCloneSelfPromptBtn) refs.aiCloneSelfPromptBtn.disabled = false;
                if (payload.success) {
                    if (refs.aiClonePromptInput) refs.aiClonePromptInput.value = payload.prompt || '';
                    if (refs.aiCloneHint) refs.aiCloneHint.textContent = '提示词已生成，可继续修改后发布';
                } else if (refs.aiCloneHint) {
                    refs.aiCloneHint.textContent = payload.errorMessage || '生成提示词失败，请稍后再试';
                }
                break;
            case 'PROFILE_GET_RESULT':
                updateProfileUI(payload.user);
                break;
            case 'PROFILE_UPDATE_RESULT':
                if (payload.success) {
                    updateProfileUI(payload.user);
                    toggleProfileModal(false);
                } else {
                    alert(payload.errorMessage || '资料更新失败');
                }
                break;
            case 'PROFILE_VIEW_RESULT':
                if (payload.success) {
                    showViewProfile(payload.user);
                } else {
                    alert(payload.errorMessage || '无法获取用户资料');
                }
                break;
            case 'MESSAGE_RECALL_RESULT':
                if (payload.success) {
                    markMessageRecalled(payload.roomId, payload.messageId, payload.byUserId);
                } else {
                    alert(payload.errorMessage || '撤回失败');
                }
                break;
            case 'MESSAGE_RECALLED':
                markMessageRecalled(payload.roomId, payload.messageId, payload.byUserId);
                break;
            case 'MESSAGE_SEND_FAILED':
                alert(payload.errorMessage || '消息发送失败');
                break;
            case 'ERROR':
                alert(payload.errorMessage || '服务器异常');
                break;
            default:
                console.log('Unhandled message', type, payload);
        }
    }

function enterChatMode() {
    refs.loginPanel.classList.add('hidden');
    refs.chatPanel.classList.remove('hidden');
    state.rooms = {};
    state.roomOrder = [];
    state.messagesByRoom = {};
    state.currentRoomId = null;
    state.moments = [];
    state.treeHoleMessages = [];
    state.aiClonesAll = [];
    state.aiClonesMine = [];
    refs.loginHint.textContent = '';
    updateProfileUI(state.currentUser);
    renderAiClones();
    switchMode('chat');

    // add: 强制初始状态：聊天按钮显示，树洞按钮隐藏
    refs.modeChatBtn?.classList.remove('hidden');
    refs.modeTreeHoleBtn?.classList.add('hidden');
    
    initSidebarIcons();
    // 替换原user图标激活逻辑 → 激活第一个侧边栏图标（删除user后第一个是friends）
    document.querySelector('.sidebar-icon')?.classList.add('active');
    // 初始隐藏“当前用户”面板，仅点击图标后显示
    document.querySelector('.profile-card')?.classList.add('hidden');

    // add:welcome 显示欢迎面板，隐藏内容区域
    document.getElementById('post-login-welcome').classList.remove('hidden');
    document.getElementById('chat-content').classList.add('hidden');
    
    
// 初始隐藏关闭按钮
    document.getElementById('close-chat-btn').classList.remove('visible');
    // add: 隐藏登录容器
    document.getElementById('login-container')?.classList.add('hidden');
}

    function requestInitialData() {
        send('FRIEND_LIST');
        send('FRIEND_REQUEST_LIST');
        send('MOMENT_LIST');
        send('ROOM_LIST');
        send('PROFILE_GET');
        send('FETCH_TREE_HOLE');
        send('AI_CLONE_LIST');
    }

    function switchMode(mode) {
        if (mode !== 'chat' && mode !== 'treehole') return;
        state.currentMode = mode;
        const isTreeHole = mode === 'treehole';
        if (state.selectMode) {
            ensureSelectedMessageSet();
            state.selectMode = false;
            state.selectedMessageIds.clear();
            updateMultiSelectBar();
        }

        refs.modeChatBtn?.classList.toggle('active', !isTreeHole);
        refs.modeTreeHoleBtn?.classList.toggle('active', isTreeHole);
        
        // 隐藏和显示模式按钮
        refs.modeChatBtn?.classList.toggle('hidden', isTreeHole);
        refs.modeTreeHoleBtn?.classList.toggle('hidden', !isTreeHole);

        refs.chatSection?.classList.toggle('hidden', isTreeHole);
        refs.treeHoleSection?.classList.toggle('hidden', !isTreeHole);
        
        // 控制游戏按钮显示
        const gameBtn = document.getElementById('game-btn');
        if (gameBtn) {
            gameBtn.classList.toggle('hidden', isTreeHole);
        }

        // 控制关闭按钮显示（新增代码）
        const closeChatBtn = document.getElementById('close-chat-btn');
        if (closeChatBtn) {
            closeChatBtn.classList.toggle('visible', !isTreeHole);
        }
        
        if (isTreeHole) {
            document.getElementById('manage-room-btn').classList.add('hidden');
            refs.chatTitle.textContent = '匿名树洞';
            refs.chatSubtitle.textContent = '向所有在线用户匿名倾诉';
            if (!state.treeHoleMessages.length) {
                send('FETCH_TREE_HOLE');
            } else {
                renderTreeHoleMessages();
            }
        } else {
            const room = state.rooms[state.currentRoomId];
            // 聊天模式根据房间类型显示群管理按钮
        if (room && room.type !== 'PRIVATE') {
            document.getElementById('manage-room-btn').classList.remove('hidden');
        }
            if (room) {
                const memberCount = room.memberIds ? room.memberIds.length : 0;
                refs.chatTitle.textContent = room.name;
                refs.chatSubtitle.textContent = room.type === 'PRIVATE' ? '私聊' : `成员 ${memberCount}`;
            } else {
                refs.chatTitle.textContent = '请选择聊天对象';
                refs.chatSubtitle.textContent = '还没有打开任何房间';
            }
            renderMessages();
        }
    }

    function send(type, payload = {}) {
        if (!state.socket || state.socket.readyState !== WebSocket.OPEN) {
            alert('尚未连接到服务器');
            return;
        }
        console.log('[ws-send]', { type, payload });
        state.socket.send(JSON.stringify({ type, payload }));
    }

    function applyImgApiConfig(config) {
        if (!config) return;
        if (typeof config.imgApiBase === 'string') {
            IMG_API_BASE = config.imgApiBase.trim();
        }
        if (typeof config.imgApiEmail === 'string') {
            IMG_API_EMAIL = config.imgApiEmail.trim();
        }
        if (typeof config.imgApiPassword === 'string') {
            IMG_API_PASSWORD = config.imgApiPassword;
        }
        state.imgApiToken = null;
    }

    async function ensureImgToken() {
        if (!IMG_API_BASE || !IMG_API_EMAIL || !IMG_API_PASSWORD) {
            throw new Error('Image upload not configured.');
        }
        if (state.imgApiToken) {
            return state.imgApiToken;
        }
        const resp = await fetch(`${IMG_API_BASE}/tokens`, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: IMG_API_EMAIL,
                password: IMG_API_PASSWORD
            })
        });
        const data = await resp.json();
        if (!data.status || !data.data || !data.data.token) {
            throw new Error(data.message || '获取上传 token 失败');
        }
        state.imgApiToken = data.data.token;
        return state.imgApiToken;
    }

    async function uploadImageToCdn(file) {
        if (!file) throw new Error('未选择图片');
        if (file.size > MAX_FILE_SIZE) {
            throw new Error('文件过大，限制 2MB');
        }
        const token = await ensureImgToken();
        const form = new FormData();
        form.append('file', file);
        form.append('strategy_id', 2);
        const resp = await fetch(`${IMG_API_BASE}/upload`, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: form
        });
        const data = await resp.json();
        if (!data.status || !data.data) {
            throw new Error(data.message || '上传失败');
        }
        return data.data.url || (data.data.links ? data.data.links.url : null);
    }

    function toggleAiCloneModal(show) {
        if (!refs.aiCloneModal) return;
        refs.aiCloneModal.classList.toggle('hidden', !show);
        if (refs.aiCloneHint) refs.aiCloneHint.textContent = '';
        if (refs.aiCloneSelfPromptBtn) refs.aiCloneSelfPromptBtn.disabled = false;
        if (!show) {
            if (refs.aiCloneIdInput) refs.aiCloneIdInput.value = '';
            if (refs.aiCloneNameInput) refs.aiCloneNameInput.value = '';
            if (refs.aiClonePromptInput) refs.aiClonePromptInput.value = '';
            if (refs.aiCloneAvatarInput) refs.aiCloneAvatarInput.value = '';
        } else if (refs.aiCloneIdInput) {
            refs.aiCloneIdInput.focus();
        }
    }

    function getTargetUserIdForRoom(room) {
        if (!room || !room.memberIds || !room.memberIds.length || !state.currentUser) return null;
        if (room.type === 'PRIVATE') {
            return room.memberIds.find(id => id !== state.currentUser.id) || null;
        }
        return null;
    }

    function sendTomatoAction() {
        if (!state.currentRoomId) {
            alert('请先选择房间');
            return;
        }
        const room = state.rooms[state.currentRoomId];
        if (!room) {
            alert('房间信息获取失败');
            return;
        }
        const targetUserId = getTargetUserIdForRoom(room);
        const payload = {
            roomId: room.id,
            content: TOMATO_ACTION,
            messageType: 'ACTION'
        };
        if (targetUserId) {
            payload.targetUserId = targetUserId;
            payload.toUser = targetUserId;
        }
        send(room.type === 'PRIVATE' ? 'PRIVATE_MESSAGE' : 'ROOM_MESSAGE', payload);
    }

    function handleRoomData(room, messages = []) {
        if (!room) return;
        state.rooms[room.id] = room;
        if (!state.roomOrder.includes(room.id)) {
            state.roomOrder.push(room.id);
        }
        if (!state.messagesByRoom[room.id]) {
            state.messagesByRoom[room.id] = [];
        }
        if (messages.length) {
            // 处理历史消息中的 <spilit> 分隔符
            const processedMessages = [];
            messages.forEach(msg => {
                const splitMsgs = splitAiMessage(msg);
                processedMessages.push(...splitMsgs);
            });
            state.messagesByRoom[room.id] = processedMessages;
        }
        renderRooms();
        if (state.currentRoomId === room.id) {
            renderMessages();
        }



        // ADD: 如果管理弹窗开着，刷新数据
        if (refs.manageRoomModal && !refs.manageRoomModal.classList.contains('hidden') && state.currentRoomId === room.id) {
            renderManageRoomModal(room);
        }
    }

    function resetRooms() {
        state.rooms = {};
        state.roomOrder = [];
    }

    function renderFriendSearchResults() {
        refs.friendSearchList.innerHTML = '';
        state.friendSearchResults.forEach(user => {
            const li = document.createElement('li');
            li.innerHTML = `<strong>${user.displayName || user.username}</strong><span class="muted">@${user.username}</span>`;
            const actions = document.createElement('div');
            actions.className = 'actions';
            const viewBtn = document.createElement('button');
            viewBtn.className = 'ghost-btn';
            viewBtn.textContent = '查看';
            viewBtn.addEventListener('click', evt => {
                evt.stopPropagation();
                send('PROFILE_VIEW', { userId: user.id });
            });
            const addBtn = document.createElement('button');
            addBtn.className = 'ghost-btn';
            addBtn.textContent = '添加';
            addBtn.addEventListener('click', evt => {
                evt.stopPropagation();
                send('FRIEND_ADD', { targetUserId: user.id });
            });
            actions.appendChild(viewBtn);
            actions.appendChild(addBtn);
            li.appendChild(actions);
            refs.friendSearchList.appendChild(li);
        });
    }

    function renderFriendRequests() {
        refs.friendRequestList.innerHTML = '';
        if (!state.friendRequests.length) {
            const empty = document.createElement('li');
            empty.className = 'muted';
            empty.textContent = '暂无好友请求';
            refs.friendRequestList.appendChild(empty);
            return;
        }
        state.friendRequests.forEach(request => {
            const li = document.createElement('li');
            li.innerHTML = `<strong>${request.fromDisplayName || '好友'}</strong><span class="muted">请求添加好友</span>`;
            const actions = document.createElement('div');
            actions.className = 'actions';
            const acceptBtn = document.createElement('button');
            acceptBtn.className = 'primary-btn';
            acceptBtn.textContent = '同意';
            acceptBtn.addEventListener('click', evt => {
                evt.stopPropagation();
                send('FRIEND_REQUEST_RESPOND', { requestId: request.id, accept: true });
            });
            const rejectBtn = document.createElement('button');
            rejectBtn.className = 'ghost-btn';
            rejectBtn.textContent = '拒绝';
            rejectBtn.addEventListener('click', evt => {
                evt.stopPropagation();
                send('FRIEND_REQUEST_RESPOND', { requestId: request.id, accept: false });
            });
            actions.appendChild(acceptBtn);
            actions.appendChild(rejectBtn);
            li.appendChild(actions);
            refs.friendRequestList.appendChild(li);
        });
    }

    // function renderFriends() {
    //     refs.friendList.innerHTML = '';
    //     state.friends.forEach(friend => {
    //         const li = document.createElement('li');
    //         li.dataset.userId = friend.id;
    //         const name = document.createElement('strong');
    //         name.textContent = friend.displayName || friend.username;
    //         const status = document.createElement('span');
    //         status.className = 'muted';
    //         status.textContent = friend.status || '';
    //         const actions = document.createElement('div');
    //         actions.className = 'actions';
    //         const viewBtn = document.createElement('button');
    //         viewBtn.className = 'ghost-btn';
    //         viewBtn.textContent = '资料';
    //         viewBtn.addEventListener('click', evt => {
    //             evt.stopPropagation();
    //             send('PROFILE_VIEW', { userId: friend.id });
    //         });
    //         actions.appendChild(viewBtn);
    //         li.appendChild(name);
    //         li.appendChild(status);
    //         li.appendChild(actions);
    //         li.addEventListener('click', () => openPrivateChat(friend.id));
    //         refs.friendList.appendChild(li);
    //     });
    // }
    function renderFriends() {
    refs.friendList.innerHTML = '';
    state.friends.forEach(friend => {
        const li = document.createElement('li');
        li.dataset.userId = friend.id;

        const avatarWrap = document.createElement('span');
        avatarWrap.className = 'friend-avatar-wrap';
        const avatarImg = document.createElement('img');
        avatarImg.className = 'friend-avatar';
        avatarImg.src = friend.avatarUrl || '';
        avatarImg.alt = friend.displayName || friend.username || '';
        const statusDot = document.createElement('span');
        const statusValue = String(friend.status || '').toUpperCase();
        const isOnline = statusValue === 'ONLINE';
        statusDot.className = `friend-status-dot ${isOnline ? 'online' : 'offline'}`;
        avatarWrap.appendChild(avatarImg);
        avatarWrap.appendChild(statusDot);
        li.prepend(avatarWrap);

        const name = document.createElement('strong');
        name.textContent = friend.displayName || friend.username;
        
        const actions = document.createElement('div');
        actions.className = 'actions';
        
        const viewBtn = document.createElement('button');
        viewBtn.className = 'ghost-btn';
        viewBtn.textContent = '资料';
        viewBtn.addEventListener('click', evt => {
            evt.stopPropagation();
            send('PROFILE_VIEW', { userId: friend.id });
        });
        actions.appendChild(viewBtn);
        
        li.appendChild(name);
        li.appendChild(actions);
        li.addEventListener('click', () => openPrivateChat(friend.id));
        refs.friendList.appendChild(li);
    });
}

    function viewUserProfile(userId) {
        if (!userId) return;
        send('PROFILE_VIEW', { userId });
    }

    function renderRoomMemberOptions() {
        const scrollTop = refs.roomMembersBox.scrollTop;
        refs.roomMembersBox.innerHTML = '';
        if (!state.friends.length) {
            refs.roomMembersBox.textContent = '暂无好友可选';
            return;
        }
        state.friends.forEach((friend, index) => {
            if (index > 0) {
                const divider = document.createElement('div');
                divider.className = 'friend-divider';
                divider.dataset.role = 'friend-divider';
                divider.dataset.index = index; 
                refs.roomMembersBox.appendChild(divider);
            }
            const label = document.createElement('label');
            label.className = 'member-option';
            label.dataset.friendId = friend.id; 
            const name = document.createElement('span');
            name.textContent = friend.displayName || friend.username;
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = friend.id;
            label.appendChild(name);
            label.appendChild(checkbox);
            refs.roomMembersBox.appendChild(label);
        });
        refs.roomMembersBox.scrollTop = scrollTop;
        refs.roomMembersBox.style.opacity = '0.99';
        setTimeout(() => {
            refs.roomMembersBox.style.opacity = '1';
        }, 10);
    }
    refs.roomMembersBox.addEventListener('scroll', () => {
        document.querySelectorAll('.friend-divider').forEach(divider => {
            divider.style.display = 'none';
            setTimeout(() => {
                divider.style.display = 'block';
            }, 10);
        });
    });

    function renderRoomSearchResults() {
        refs.roomSearchList.innerHTML = '';
        state.roomSearchResults.forEach(room => {
            const li = document.createElement('li');
            li.innerHTML = `<strong>${room.name}</strong><span class="muted">${room.type}</span>`;
            const joinBtn = document.createElement('button');
            joinBtn.className = 'ghost-btn';
            joinBtn.textContent = '加入';
            joinBtn.addEventListener('click', evt => {
                evt.stopPropagation();
                send('ROOM_JOIN', { roomId: room.id });
            });
            const actions = document.createElement('div');
            actions.className = 'actions';
            actions.appendChild(joinBtn);
            li.appendChild(actions);
            refs.roomSearchList.appendChild(li);
        });
    }

    function renderAiClones() {
        renderCloneList(refs.aiCloneMineList, state.aiClonesMine, true);
        renderCloneList(refs.aiCloneAllList, state.aiClonesAll, false);
    }

    function renderCloneList(container, clones, isMineList) {
        if (!container) return;
        container.innerHTML = '';
        const list = (clones || []).filter(clone => (isMineList ? true : !clone.ownedByCurrentUser));
        if (!list.length) {
            const empty = document.createElement('li');
            empty.textContent = isMineList ? '还没有创建分身' : '暂无可添加的分身';
            empty.className = 'muted';
            container.appendChild(empty);
            return;
        }
        list.forEach(clone => {
            const li = document.createElement('li');
            li.classList.add('ai-clone-card');
            const info = document.createElement('div');
            info.className = 'ai-clone-info';
            const title = document.createElement('div');
            title.className = 'ai-clone-title';
            title.textContent = clone.displayName || clone.id;
            const meta = document.createElement('div');
            meta.className = 'ai-clone-meta muted';
            const ownerText = clone.ownedByCurrentUser
                ? '由我创建'
                : (clone.ownerDisplayName ? `创建者：${clone.ownerDisplayName}` : '');
            meta.textContent = [clone.id, ownerText].filter(Boolean).join(' · ');
            info.appendChild(title);
            info.appendChild(meta);
            if (clone.prompt) {
                const prompt = document.createElement('div');
                const maxLength = 51;//保留四行
                const displayText = clone.prompt.length > maxLength 
                    ? `提示词：${clone.prompt.slice(0, maxLength)}...` 
                    : `提示词：${clone.prompt}`;
                prompt.textContent = displayText;
                info.appendChild(prompt);
            }
            const actions = document.createElement('div');
            actions.className = 'actions ai-clone-actions';
            const actionBtn = document.createElement('button');
            actionBtn.className = 'ghost-btn';
            if (clone.friend || clone.ownedByCurrentUser) {
                actionBtn.textContent = '私聊';
                actionBtn.addEventListener('click', evt => {
                    evt.stopPropagation();
                    openPrivateChat(clone.id);
                });
            } else {
                actionBtn.textContent = '加好友';
                actionBtn.addEventListener('click', evt => {
                    evt.stopPropagation();
                    send('FRIEND_ADD', { targetUserId: clone.id });
                });
            }
            actions.appendChild(actionBtn);
            if (clone.friend || clone.ownedByCurrentUser) {
                const badge = document.createElement('span');
                badge.className = 'muted';
                badge.textContent = clone.ownedByCurrentUser ? '我的分身' : '已是好友';
                actions.appendChild(badge);
            }
            li.append(info, actions);
            container.appendChild(li);
        });
    }

    function openPrivateChat(friendUserId) {
        send('PRIVATE_OPEN', { friendUserId });
    }

//     function renderRooms() {
//     refs.roomList.innerHTML = '';
//     state.roomOrder.forEach(roomId => {
//         const room = state.rooms[roomId];
//         if (!room) return;
//         const li = document.createElement('li');
//         li.dataset.roomId = room.id;
//         if (room.id === state.currentRoomId) li.classList.add('active');

//         // ========== 新增这3行：添加房间头像 ==========
//         const avatarImg = document.createElement('img');
//         avatarImg.className = 'room-avatar';
//         avatarImg.src = room.avatarUrl || ''; // 房间的特定头像URL
//         avatarImg.alt = room.name;
//         li.prepend(avatarImg);
//         // ==============================================

//         // 以下原有代码完全保留
//         const name = document.createElement('strong');
//         name.textContent = room.name;
        
//         const memberCount = document.createElement('span');
//         memberCount.className = 'muted small';
//         memberCount.textContent = `${room.memberIds.length} 人`;
        
//         li.appendChild(name);
//         li.appendChild(memberCount);
//         li.addEventListener('click', () => joinRoom(room.id));
//         refs.roomList.appendChild(li);
//     });
// }
// function renderRooms() {
//     refs.roomList.innerHTML = '';
//     state.roomOrder.forEach(roomId => {
//         const room = state.rooms[roomId];
//         if (!room) return;
//         const li = document.createElement('li');
//         li.dataset.roomId = room.id;
//         if (room.id === state.currentRoomId) li.classList.add('active');

//         // ========== 核心修改：头像容器（兼容有/无头像） ==========
//         const avatarContainer = document.createElement('div');
//         avatarContainer.className = 'room-avatar'; // 保留原有类名
        
//         // 有头像URL则显示图片，无则显示🏠
//         if (room.avatarUrl && room.avatarUrl.trim()) {
//             const avatarImg = document.createElement('img');
//             avatarImg.src = room.avatarUrl;
//             avatarImg.alt = room.name;
//             // 图片继承容器样式，确保填充
//             avatarImg.style.width = '100%';
//             avatarImg.style.height = '100%';
//             avatarImg.style.objectFit = 'cover';
//             avatarContainer.appendChild(avatarImg);
//         } else {
//             // 无头像时显示🏠，居中
//             avatarContainer.textContent = '🏠';
//         }
//         li.prepend(avatarContainer);
//         // ======================================================

//         // 以下是你原有代码，完全保留
//         const name = document.createElement('strong');
//         name.textContent = room.name;
        
//         const memberCount = document.createElement('span');
//         memberCount.className = 'muted small';
//         memberCount.textContent = `${room.memberIds.length} 人`;
        
//         li.appendChild(name);
//         li.appendChild(memberCount);
//         li.addEventListener('click', () => joinRoom(room.id));
//         refs.roomList.appendChild(li);
//     });
// }
function renderRooms() {
    refs.roomList.innerHTML = '';
    state.roomOrder.forEach(roomId => {
        const room = state.rooms[roomId];
        if (!room) return;
        const li = document.createElement('li');
        li.dataset.roomId = room.id;
        if (room.id === state.currentRoomId) li.classList.add('active');

        // ========== 原有头像逻辑（保留不动） ==========
        const avatarContainer = document.createElement('div');
        avatarContainer.className = 'room-avatar';
        if (room.avatarUrl && room.avatarUrl.trim()) {
            const avatarImg = document.createElement('img');
            avatarImg.src = room.avatarUrl;
            avatarImg.alt = room.name;
            avatarImg.style.width = '100%';
            avatarImg.style.height = '100%';
            avatarImg.style.objectFit = 'cover';
            avatarContainer.appendChild(avatarImg);
        } else {
            avatarContainer.textContent = '🏠';
        }
        li.prepend(avatarContainer);

        // ========== 原有名称逻辑（保留不动） ==========
        const name = document.createElement('strong');
        name.textContent = room.name;

        // ========== 新增：判断并显示「私聊/群聊」 + 保留成员数 ==========
        const typeAndMember = document.createElement('span');
        typeAndMember.className = 'muted small'; // 复用原有样式类，保证视觉统一
        
        // 核心判断：成员数=2 → 私聊；>2 → 群聊
        const roomType = room.memberIds.length === 2 ? '私聊' : '群聊';
        // 拼接「类型 + 成员数」，和原有显示格式兼容
        typeAndMember.textContent = `${roomType} · ${room.memberIds.length} 人`;

        // ========== 原有追加逻辑（仅替换变量名，其余不动） ==========
        li.appendChild(name);
        li.appendChild(typeAndMember); // 替换原memberCount，改为新的类型+成员数
        li.addEventListener('click', () => joinRoom(room.id));
        refs.roomList.appendChild(li);
    });
}


    function joinRoom(roomId) {
        const room = state.rooms[roomId];
        if (room && room.type === 'PRIVATE') {
            setCurrentRoom(roomId);
        } else {
            send('ROOM_JOIN', { roomId });
        }
    }

    function setCurrentRoom(roomId) {
    state.currentRoomId = roomId;
    state.roomEnterSerial = (state.roomEnterSerial || 0) + 1;
    
    refs.aiWelcomePlaceholder?.classList.add('hidden');
    refs.chatSection?.classList.remove('hidden');
    
    if (state.selectMode) {
        ensureSelectedMessageSet();
        state.selectMode = false;
        state.selectedMessageIds.clear();
        updateMultiSelectBar();
    }
    const room = state.rooms[roomId];
    if (!room) return;
    
    // 显示关闭按钮
    document.getElementById('close-chat-btn').classList.add('visible');
    
    if (state.currentMode === 'treehole') {
        switchMode('chat');
    }
    hideContextMenu();
    clearQuote();
    const memberCount = room.memberIds ? room.memberIds.length : 0;
    refs.chatTitle.textContent = room.name;
    refs.chatSubtitle.textContent = room.type === 'PRIVATE' ? '私聊' : `成员 ${memberCount}`;



    // 找到 setCurrentRoom 函数，在 renderMessages(); scheduleTomatoPlayback(roomId); 之前插入：
    // ADD: 群管理按钮显示逻辑
    if (room.type === 'GROUP' && state.currentUser && String(room.ownerId) === String(state.currentUser.id)) {
        refs.manageRoomBtn?.classList.remove('hidden');
    } else {
        refs.manageRoomBtn?.classList.add('hidden');
        if (refs.manageRoomModal && !refs.manageRoomModal.classList.contains('hidden')) {
            toggleManageRoomModal(false);
        }
    }

    renderRooms();
    renderMessages();
    scheduleTomatoPlayback(roomId);
}

    
    function renderMessages() {
        if (state.currentMode === 'treehole') {
            return;
        }
        hideContextMenu();
        if (state.selectMode) {
            ensureSelectedMessageSet();
        }
        refs.messageList.innerHTML = '';
        const roomId = state.currentRoomId;
        
        // 无选中房间时，显示自定义默认界面
        if (!roomId) {
            refs.messageList.innerHTML = `
                <div class="empty-chat-container">
                    <div class="empty-chat-img">
                        <img src="https://2bpic.oss-cn-beijing.aliyuncs.com/2025/12/04/6930f7a2c12fc.png" alt="选择聊天对象" />
                    </div>
                    <div class="empty-chat-title">还没有选中聊天对象</div>
                    <div class="empty-chat-desc">选择一位好友或加入一个房间，开始你的聊天吧～</div>
                    <div class="empty-chat-actions">
                        <button class="primary-btn" id="guide-to-friends">查看好友</button>
                        <button class="ghost-btn" id="guide-to-rooms">浏览房间</button>
                    </div>
                </div>
            `;
            // 绑定引导按钮事件
            document.getElementById('guide-to-friends').addEventListener('click', () => {
                document.querySelector('.sidebar-icon[data-view="friends"]').click();
            });
            document.getElementById('guide-to-rooms').addEventListener('click', () => {
                document.querySelector('.sidebar-icon[data-view="rooms"]').click();
            });
            return;
        }

        const messages = state.messagesByRoom[roomId] || [];
        // 有房间但无消息时，显示简化提示
        if (!messages.length) {
            refs.messageList.innerHTML = `
                <div class="empty-room-container">
                    <div class="empty-room-desc">暂无聊天记录</div>
                    <div class="empty-room-tip">发送第一条消息，开启对话吧～</div>
                </div>
            `;
            return;
        }

        // 正常渲染消息列表
        messages.forEach(message => {
            const node = buildMessageNode(message);
            refs.messageList.appendChild(node);
            setupBurnTimer(message, node);
        });
        updateMultiSelectBar();
        refs.messageList.scrollTop = refs.messageList.scrollHeight;
        // 进入房间时立即尝试播放一次番茄动画（然后再兜底处理未播放的）
        requestAnimationFrame(() => {
            playLatestTomatoForEntry(roomId);
            playLatestPendingTomato(roomId);
        });
    }

    function findMessageById(roomId, messageId) {
        if (!roomId || !messageId) return null;
        const list = state.messagesByRoom[roomId] || [];
        return list.find(m => m.id === messageId) || null;
    }

    function getMessagePlainText(message) {
        if (!message) return '';
        const type = getMessageType(message);
        if (type === 'FILE_LINK') {
            const file = parseFileContent(message.content);
            return file.name ? `[文件] ${file.name}` : '[文件消息]';
        }
        if (type === 'DICE_RESULT') return '掷骰子结果';
        if (type === 'RPS_RESULT') return '猜拳结果';
        if (type === 'FORWARD_CARD') {
            const card = parseForwardCardContent(message.content);
            return `[聊天记录] ${card.title || ''}`;
        }
        if (type === 'ACTION' && (message.content || '').toUpperCase() === TOMATO_ACTION) {
            return '扔给你一个番茄！';
        }
        return message.content || '';
    }

    function canRecallMessage(message) {
        if (!message || !state.currentUser) return false;
        if (message.fromUserId !== state.currentUser.id) return false;
        if (!message.timestamp || message.recalled) return false;
        const timestamp =
            typeof message.timestamp === 'number'
                ? message.timestamp
                : Date.parse(message.timestamp) || Number(message.timestamp) || 0;
        if (!timestamp) return false;
        return Date.now() - timestamp <= 2 * 60 * 1000;
    }

    function markMessageRecalled(roomId, messageId, byUserId) {
        const list = state.messagesByRoom[roomId] || [];
        const target = list.find(m => m.id === messageId);
        if (!target) return;
        target.recalled = true;
        target.recalledBy = byUserId || null;
        stopBurnCountdown(target);
        if (target._burnTimeout) {
            clearTimeout(target._burnTimeout);
            target._burnTimeout = null;
        }
        renderMessages();
    }

    function buildRecalledPlaceholder(message) {
        const placeholder = document.createElement('div');
        placeholder.className = 'message-recalled';
        const isMe = message && message.fromUserId === (state.currentUser && state.currentUser.id);
        placeholder.textContent = isMe ? '你撤回了一条消息' : '对方撤回了一条消息';
        return placeholder;
    }

    function buildQuoteBlock(quote) {
        if (!quote) return null;
        const block = document.createElement('div');
        block.className = 'quote-block';
        const sender = document.createElement('div');
        sender.className = 'quote-block-sender';
        sender.textContent = quote.sender || quote.senderName || '引用';
        const text = document.createElement('div');
        text.className = 'quote-block-text';
        text.textContent = quote.content || quote.preview || '';
        block.append(sender, text);
        return block;
    }

    function setQuoteFromMessage(message) {
        if (!message) return;
        const content = getMessagePlainText(message);
        state.currentQuote = {
            messageId: message.id,
            roomId: message.roomId,
            sender: message.fromUserDisplayName || '用户',
            content: content.length > 120 ? `${content.slice(0, 120)}...` : content
        };
        renderQuotePreview();
    }

    function renderQuotePreview() {
        if (!refs.quotePreview) return;
        const quote = state.currentQuote;
        if (!quote) {
            refs.quotePreview.classList.add('hidden');
            return;
        }
        refs.quoteSender.textContent = quote.sender || '引用';
        refs.quoteText.textContent = quote.content || '';
        refs.quotePreview.classList.remove('hidden');
    }

    function clearQuote() {
        state.currentQuote = null;
        renderQuotePreview();
    }

    function hideContextMenu() {
        contextMenu.classList.add('hidden');
        contextMenuMessage = null;
    }

    function showContextMenu(evt, message) {
        if (!contextMenu) return;
        if (state.selectMode) {
            evt.preventDefault();
            toggleMessageSelection(message && message.id);
            return;
        }
        evt.preventDefault();
        contextMenuMessage = message;
        const recallBtn = contextMenu.querySelector('[data-action="recall"]');
        if (recallBtn) {
            recallBtn.classList.toggle('hidden', !canRecallMessage(message));
        }
        contextMenu.classList.remove('hidden');
        const { clientX, clientY } = evt;
        const menuWidth = contextMenu.offsetWidth || 160;
        const menuHeight = contextMenu.offsetHeight || 120;
        const x = Math.min(clientX, window.innerWidth - menuWidth - 8);
        const y = Math.min(clientY, window.innerHeight - menuHeight - 8);
        contextMenu.style.left = `${x}px`;
        contextMenu.style.top = `${y}px`;
    }

    function ensureSelectedMessageSet() {
        if (!(state.selectedMessageIds instanceof Set)) {
            state.selectedMessageIds = new Set();
        }
    }

    function updateMultiSelectBar() {
        const countEl = multiSelectBar.querySelector('.count');
        if (countEl) {
            countEl.textContent = state.selectedMessageIds.size;
        }
        multiSelectBar.classList.toggle('hidden', !state.selectMode);
    }

    function enterSelectMode(initialMessageId) {
        ensureSelectedMessageSet();
        state.selectMode = true;
        if (initialMessageId) {
            state.selectedMessageIds.add(initialMessageId);
        }
        updateMultiSelectBar();
        renderMessages();
    }

    function exitSelectMode() {
        ensureSelectedMessageSet();
        state.selectMode = false;
        state.selectedMessageIds.clear();
        state.forwardTargets.clear();
        updateMultiSelectBar();
        renderMessages();
    }

    function toggleMessageSelection(messageId, forceChecked) {
        if (!messageId) return;
        ensureSelectedMessageSet();
        const shouldSelect =
            forceChecked === undefined ? !state.selectedMessageIds.has(messageId) : Boolean(forceChecked);
        if (!state.selectMode) {
            state.selectMode = true;
        }
        if (shouldSelect) {
            state.selectedMessageIds.add(messageId);
        } else {
            state.selectedMessageIds.delete(messageId);
        }
        const wrapper = document.querySelector(`[data-message-id="${messageId}"]`);
        if (wrapper) {
            wrapper.classList.toggle('selected', shouldSelect);
            const checkbox = wrapper.querySelector('.message-select-checkbox');
            if (checkbox) {
                checkbox.checked = shouldSelect;
            }
        }
        updateMultiSelectBar();
    }

    function getAvailableForwardTargets() {
        const result = [];
        Object.values(state.rooms || {}).forEach(room => {
            result.push({
                id: room.id,
                name: room.name || room.id,
                type: room.type || 'ROOM',
                memberCount: room.memberIds ? room.memberIds.length : 0
            });
        });
        return result;
    }

    function openForwardTargetModal() {
        const modal = document.getElementById('forward-target-modal');
        if (!modal) return;
        renderForwardTargetOptions();
        modal.classList.remove('hidden');
    }

    function closeForwardTargetModal() {
        const modal = document.getElementById('forward-target-modal');
        if (!modal) return;
        modal.classList.add('hidden');
    }

    function renderForwardTargetOptions() {
        const modal = document.getElementById('forward-target-modal');
        if (!modal) return;
        const list = modal.querySelector('.forward-target-list');
        if (!list) return;
        const targets = getAvailableForwardTargets();
        list.innerHTML = '';
        if (!targets.length) {
            const empty = document.createElement('div');
            empty.className = 'muted';
            empty.textContent = '暂无可选房间';
            list.appendChild(empty);
            return;
        }
        targets.forEach(target => {
            const item = document.createElement('label');
            item.className = 'forward-target-item';
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = target.id;
            checkbox.checked = state.forwardTargets.has(target.id);
            checkbox.addEventListener('change', evt => {
                if (evt.target.checked) {
                    state.forwardTargets.add(target.id);
                } else {
                    state.forwardTargets.delete(target.id);
                }
            });
            const info = document.createElement('div');
            info.className = 'target-info';
            const name = document.createElement('div');
            name.className = 'target-name';
            name.textContent = target.name;
            const meta = document.createElement('div');
            meta.className = 'target-meta muted';
            meta.textContent = target.type === 'PRIVATE' ? '私聊' : `房间 · ${target.memberCount || 0}人`;
            info.append(name, meta);
            item.append(checkbox, info);
            list.appendChild(item);
        });
    }

    function getChosenForwardTargets() {
        ensureSelectedMessageSet();
        if (state.forwardTargets && state.forwardTargets.size > 0) {
            return Array.from(state.forwardTargets);
        }
        if (state.currentRoomId) {
            return [state.currentRoomId];
        }
        return [];
    }

    function findMessageInStateById(messageId) {
        if (!messageId) return null;
        const normalizedId = normalizeForwardMessageId(messageId);
        for (const roomId of Object.keys(state.messagesByRoom || {})) {
            const msg = (state.messagesByRoom[roomId] || []).find(m => normalizeForwardMessageId(m.id) === normalizedId);
            if (msg) return msg;
        }
        return null;
    }

    function formatForwardTime(ts) {
        if (!ts) return '';
        const d = new Date(ts);
        if (Number.isNaN(d.getTime())) return '';
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const hh = String(d.getHours()).padStart(2, '0');
        const mi = String(d.getMinutes()).padStart(2, '0');
        return `${mm}-${dd} ${hh}:${mi}`;
    }

    function buildForwardRows(data) {
        const items = data.items || [];
        const ids = Array.isArray(data.messageIds) ? data.messageIds : [];
        const rows = [];
        const count = Math.max(items.length, ids.length);
        for (let i = 0; i < count; i++) {
            const text = items[i] || '';
            const id = ids[i];
            const msg = id ? findMessageInStateById(id) : null;
            const timeStr = msg ? formatForwardTime(msg.timestamp) : '';
            rows.push({ text, time: timeStr });
        }
        return rows;
    }

    function renderForwardDetail(data) {
        const modal = document.getElementById('forward-detail-modal');
        if (!modal) return;
        const titleEl = modal.querySelector('.forward-detail-title');
        const listEl = modal.querySelector('.forward-detail-list');
        if (titleEl) {
            titleEl.textContent = data.title || '聊天记录';
        }
        if (listEl) {
            listEl.innerHTML = '';
            const rows = buildForwardRows(data);
            rows.forEach((rowData, idx) => {
                const row = document.createElement('div');
                row.className = 'forward-detail-item';
                const left = document.createElement('div');
                left.className = 'forward-detail-index muted';
                left.textContent = idx + 1;
                const content = document.createElement('div');
                content.className = 'forward-detail-text';
                
                // Try to split sender and content for styling
                const colonIndex = rowData.text.indexOf(': ');
                if (colonIndex > -1) {
                    const senderName = rowData.text.substring(0, colonIndex);
                    const messageBody = rowData.text.substring(colonIndex + 2);
                    
                    const senderSpan = document.createElement('span');
                    senderSpan.className = 'forward-sender-name';
                    senderSpan.textContent = senderName;
                    
                    const bodySpan = document.createElement('span');
                    bodySpan.className = 'forward-message-body';
                    bodySpan.textContent = messageBody;
                    
                    content.appendChild(senderSpan);
                    content.appendChild(document.createTextNode(': ')); // Keep the colon? Or maybe hide it? User asked for "Sender Name: ... Message Text". I'll keep the colon but maybe style it or just let it be part of the flow.
                    // Actually, let's put the colon in a span or just append text.
                    // Better: Sender Name (styled) + " " + Message Body (styled). The colon is usually part of the display.
                    // Let's just append bodySpan.
                    content.appendChild(bodySpan);
                } else {
                    content.textContent = rowData.text;
                }

                const meta = document.createElement('div');
                meta.className = 'forward-detail-meta muted';
                meta.textContent = rowData.time || '';
                const right = document.createElement('div');
                right.className = 'forward-detail-body';
                right.append(content);
                if (rowData.time) {
                    right.append(meta);
                }
                row.append(left, right);
                listEl.appendChild(row);
            });
            if (!rows.length) {
                const empty = document.createElement('div');
                empty.className = 'forward-detail-empty muted';
                empty.textContent = '暂无更多内容';
                listEl.appendChild(empty);
            }
        }
        modal.classList.remove('hidden');
    }

    function closeForwardDetail() {
        const modal = document.getElementById('forward-detail-modal');
        if (modal) {
            modal.classList.add('hidden');
        }
    }

    function forwardSelectedIndividually() {
        ensureSelectedMessageSet();
        const roomMessages = state.messagesByRoom[state.currentRoomId] || [];
        // 直接保存选中的消息对象，而不是只保存ID后再查找
        const selectedMessages = roomMessages.filter(m => state.selectedMessageIds.has(m.id));
        if (!selectedMessages.length) {
            alert('请选择要转发的消息');
            return;
        }
        const targets = getChosenForwardTargets();
        if (!targets.length) {
            alert('请先选择转发目标或进入一个房间');
            return;
        }
        targets.forEach(roomId => {
            selectedMessages.forEach(message => {
                const payload = {
                    roomId,
                    content: message.content,
                    messageType: getMessageType(message)
                };
                if (message.targetUserId || message.toUser) {
                    payload.targetUserId = message.targetUserId || message.toUser;
                }
                if (getMessageType(message) === 'BURN_AFTER_READING' && typeof message.burnDelay === 'number') {
                    payload.burnDelay = message.burnDelay;
                }
                send('ROOM_MESSAGE', payload);
            });
        });
        exitSelectMode();
    }

    function forwardSelectedCombined() {
        ensureSelectedMessageSet();
        const roomMessages = state.messagesByRoom[state.currentRoomId] || [];
        const orderedIds = roomMessages
            .filter(m => state.selectedMessageIds.has(m.id))
            .map(m => normalizeForwardMessageId(m.id));
        if (!orderedIds.length) {
            alert('请选择要转发的消息');
            return;
        }
        const targets = getChosenForwardTargets();
        if (!targets.length) {
            alert('请先选择转发目标或进入一个房间');
            return;
        }
        targets.forEach(roomId => {
            send('CREATE_FORWARD', { roomId, messageIds: orderedIds });
        });
        exitSelectMode();
    }

    function normalizeForwardMessageId(id) {
        if (!id) return id;
        const idx = id.indexOf('_part_');
        if (idx > 0) {
            return id.slice(0, idx);
        }
        return id;
    }

    async function copyMessageContent(message) {
        if (!message) return;
        const text = getMessagePlainText(message);
        try {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                await navigator.clipboard.writeText(text);
            } else {
                const temp = document.createElement('textarea');
                temp.value = text;
                document.body.appendChild(temp);
                temp.select();
                document.execCommand('copy');
                temp.remove();
            }
        } catch (e) {
            alert('复制失败，请手动复制');
        }
    }

    function recallMessage(message) {
        if (!canRecallMessage(message)) {
            alert('只能在两分钟内撤回自己的消息');
            return;
        }
        if (!confirm('确定要撤回这条消息吗？')) {
            return;
        }
        send('MESSAGE_RECALL', { roomId: message.roomId, messageId: message.id });
        markMessageRecalled(message.roomId, message.id, state.currentUser && state.currentUser.id);
    }

    // AI机器人的用户ID
    const AI_BOT_ID = 'ai-bot';

    /**
     * 处理消息中的 <spilit> 分隔符，将消息拆分为多个独立消息
     * 只对AI机器人的消息进行拆分处理
     */
    function splitAiMessage(message) {
        // 只处理AI类消息（系统小爱或 ai_ 前缀分身）
        const senderId = message.fromUserId;
        const isAi = senderId === AI_BOT_ID || (typeof senderId === 'string' && senderId.startsWith('ai_'));
        if (!isAi) {
            return [message];
        }
        
        const content = message.content || '';
        // 检查是否包含分隔符
        if (!content.includes('<spilit>')) {
            return [message];
        }
        
        // 按 <spilit> 分割消息
        const parts = content.split('<spilit>').map(p => p.trim()).filter(p => p.length > 0);
        if (parts.length <= 1) {
            return [message];
        }
        
        // 为每个部分创建独立的消息对象
        return parts.map((part, index) => ({
            ...message,
            id: message.id + '_part_' + index,
            content: part,
            // 稍微调整时间戳，让消息按顺序显示
            timestamp: message.timestamp + index * 100
        }));
    }

    /**
     * 计算消息显示延迟时间（基于消息长度）
     * @param {string} content 消息内容
     * @returns {number} 延迟毫秒数
     */
    function calculateMessageDelay(content) {
        const length = (content || '').length;
        // 基础延迟 300ms，每个字符增加 50ms，最大 2000ms
        const delay = Math.min(300 + length * 50, 2000);
        return delay;
    }

    function appendMessage(message) {
        normalizeTomatoTarget(message);
        if (!state.messagesByRoom[message.roomId]) {
            state.messagesByRoom[message.roomId] = [];
            if (!state.rooms[message.roomId]) {
                state.rooms[message.roomId] = { id: message.roomId, name: '新消息', type: 'PRIVATE', memberIds: [] };
                if (!state.roomOrder.includes(message.roomId)) {
                    state.roomOrder.push(message.roomId);
                }
                renderRooms();
            }
        }

        // 拆分AI消息
        const messages = splitAiMessage(message);

        // 存储所有消息部分到状态中
        messages.forEach(msg => {
            state.messagesByRoom[msg.roomId].push(msg);
        });

        const needBurnLog =
            getMessageType(message) === 'BURN_AFTER_READING' ||
            (typeof message.burnDelay === 'number' && message.burnDelay > 0);
        if (needBurnLog) {
            logBurnDebug(message);
        }

        if (state.currentRoomId === message.roomId) {
            // 如果是该房间的第一条消息，先清空“暂无聊天记录”提示
            if (state.messagesByRoom[message.roomId].length === messages.length) {
                refs.messageList.innerHTML = '';
            }
            
            // 如果只有一条消息（未拆分），直接显示
            if (messages.length === 1) {
                const node = buildMessageNode(messages[0]);
                refs.messageList.appendChild(node);
                setupBurnTimer(messages[0], node);
                refs.messageList.scrollTop = refs.messageList.scrollHeight;
            } else {
                // 多条消息，延迟依次显示
                let totalDelay = 0;
                messages.forEach((msg, index) => {
                    setTimeout(() => {
                        // 确保还在同一个房间
                        if (state.currentRoomId === msg.roomId) {
                            const node = buildMessageNode(msg);
                            refs.messageList.appendChild(node);
                            setupBurnTimer(msg, node);
                            refs.messageList.scrollTop = refs.messageList.scrollHeight;
                        }
                    }, totalDelay);
                    // 计算下一条消息的延迟（基于当前消息长度）
                    totalDelay += calculateMessageDelay(msg.content);
                });
            }
        }
    }

    function renderTreeHoleMessages() {
        if (!refs.treeHoleList) return;
        const messages = state.treeHoleMessages || [];
        refs.treeHoleList.innerHTML = '';
        if (!messages.length) {
            const empty = document.createElement('div');
            empty.className = 'tree-hole-empty';
            empty.textContent = '暂时还没有树洞消息';
            refs.treeHoleList.appendChild(empty);
            return;
        }
        messages.forEach(msg => refs.treeHoleList.appendChild(buildTreeHoleCard(msg)));
        refs.treeHoleList.scrollTop = refs.treeHoleList.scrollHeight;
    }

    function buildTreeHoleCard(message) {
        const card = document.createElement('div');
        card.className = 'tree-hole-card';
        const alias = document.createElement('p');
        alias.className = 'alias';
        alias.textContent = message.senderAlias || '某人';
        const time = document.createElement('p');
        time.className = 'time';
        time.textContent = message.timestamp ? new Date(message.timestamp).toLocaleString() : '';
        const content = document.createElement('p');
        content.className = 'content';
        content.textContent = message.content || '';
        card.append(alias, time, content);
        return card;
    }

    function renderMoments() {
        if (!refs.momentsList) return;
        let moments = state.moments || [];
        if (state.momentFilterUserId) {
            moments = moments.filter(m => m.userId === state.momentFilterUserId);
        }
        refs.momentsList.innerHTML = '';
        if (!moments.length) {
            const empty = document.createElement('div');
            empty.className = 'moment-empty';
            empty.textContent = state.momentFilterUserId ? 'TA 还没有发布动态' : '还没有动态，发一条分享给好友吧';
            refs.momentsList.appendChild(empty);
            return;
        }
        moments.forEach(moment => refs.momentsList.appendChild(buildMomentCard(moment)));
    }

    function upsertMoment(moment) {
        if (!moment) return;
        const list = Array.isArray(state.moments) ? state.moments : [];
        const idx = list.findIndex(m => m.id === moment.id);
        if (idx >= 0) {
            list[idx] = moment;
        } else {
            list.unshift(moment);
        }
        list.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
        state.moments = list;
        renderMoments();
    }
    
    

    function buildMomentCard(moment) {
        const card = document.createElement('div');
        card.className = 'moment-card';
        card.dataset.id = moment.id;

        const header = document.createElement('div');
        header.className = 'moment-header';
        const avatar = document.createElement('img');
        avatar.className = 'moment-avatar';
        avatar.src = moment.userAvatarUrl || DEFAULT_AVATAR;
        avatar.alt = moment.userDisplayName || moment.userId || 'user';
        const meta = document.createElement('div');
        meta.className = 'moment-meta';
        const name = document.createElement('p');
        name.className = 'moment-author';
        name.textContent = moment.userDisplayName || moment.userId || '用户';
        const time = document.createElement('p');
        time.className = 'moment-time';
        time.textContent = moment.timestamp ? formatMomentTime(moment.timestamp) : '';
        meta.append(name, time);
        header.append(avatar, meta);

        const content = document.createElement('p');
        content.className = 'moment-content';
        content.textContent = moment.content || '';

        const images = Array.isArray(moment.imageUrls) ? moment.imageUrls : [];
        const hasImages = images.length > 0;
        const imageGrid = document.createElement('div');
        imageGrid.className = 'moment-images';



        // 根据图片数量添加布局类（核心逻辑）
    const imgCount = images.length;
    if (imgCount === 1) {
        imageGrid.classList.add('one-img');
    } else if (imgCount >= 2 && imgCount <= 3) {
        imageGrid.classList.add('two-to-three-imgs');
    } else if (imgCount === 4) {
        imageGrid.classList.add('four-imgs');
    } else if (imgCount >= 5 && imgCount <= 6) {
        imageGrid.classList.add('five-to-six-imgs');
    } else if (imgCount >= 7 && imgCount <= 9) {
        imageGrid.classList.add('seven-to-nine-imgs');
    }




        images.forEach(url => {
            const img = document.createElement('img');
            img.src = url;
            img.alt = 'moment-image';
            imageGrid.appendChild(img);
        });

        const actions = document.createElement('div');
        actions.className = 'moment-actions';
        const likes = Array.isArray(moment.likes) ? moment.likes : [];
        const liked = !!(state.currentUser && likes.includes(state.currentUser.id));
        const likeBtn = document.createElement('button');
        likeBtn.type = 'button';
        likeBtn.className = 'moment-action-btn' + (liked ? ' active' : '');
        likeBtn.dataset.action = 'moment-like';
        likeBtn.dataset.id = moment.id;
        likeBtn.textContent = `${liked ? '已赞' : '点赞'} · ${likes.length}`;

        const commentBtn = document.createElement('button');
        commentBtn.type = 'button';
        commentBtn.className = 'moment-action-btn';
        commentBtn.dataset.action = 'moment-focus-comment';
        commentBtn.dataset.id = moment.id;
        const commentCount = Array.isArray(moment.comments) ? moment.comments.length : 0;
        commentBtn.textContent = `评论 · ${commentCount}`;

        actions.append(likeBtn, commentBtn);

        const commentsBox = document.createElement('div');
        commentsBox.className = 'moment-comments';
        (moment.comments || []).forEach(comment => commentsBox.appendChild(buildMomentComment(comment)));

        const form = document.createElement('form');
        form.className = 'moment-comment-form';
        form.dataset.id = moment.id;
        const input = document.createElement('input');
        input.type = 'text';
        input.placeholder = '写评论...';
        input.name = 'comment';
        const submit = document.createElement('button');
        submit.type = 'submit';
        submit.className = 'ghost-btn';
        submit.textContent = '发送';
        form.append(input, submit);

        card.append(header, content);
        if (hasImages) {
            card.append(imageGrid);
        }
        card.append(actions, commentsBox, form);
        return card;
    }

    function buildMomentComment(comment) {
        const row = document.createElement('div');
        row.className = 'moment-comment';
        const avatar = document.createElement('img');
        avatar.className = 'moment-comment-avatar';
        avatar.src = comment.userAvatarUrl || DEFAULT_AVATAR;
        avatar.alt = comment.userDisplayName || comment.userId || 'user';
        const body = document.createElement('div');
        body.className = 'moment-comment-body';
        const name = document.createElement('p');
        name.className = 'name';
        name.textContent = comment.userDisplayName || comment.userId || '好友';
        const text = document.createElement('p');
        text.className = 'text';
        text.textContent = comment.content || '';
        const time = document.createElement('p');
        time.className = 'time';
        time.textContent = comment.timestamp ? formatMomentTime(comment.timestamp) : '';
        body.append(name, text, time);
        row.append(avatar, body);
        return row;
    }

    function formatMomentTime(ts) {
        if (!ts) return '';
        const d = new Date(ts);
        return d.toLocaleString();
    }

    function renderMomentDraftImages() {
        if (!refs.momentUploadPreview) return;
        refs.momentUploadPreview.innerHTML = '';
        (state.momentDraftImages || []).forEach(url => {
            const wrap = document.createElement('div');
            wrap.className = 'preview-item';
            const img = document.createElement('img');
            img.src = url;
            img.alt = 'preview';
            const close = document.createElement('span');
            close.className = 'preview-remove';
            close.textContent = '×';
            close.title = '移除';
            close.addEventListener('click', () => {
                state.momentDraftImages = state.momentDraftImages.filter(u => u !== url);
                renderMomentDraftImages();
            });
            wrap.append(img, close);
            refs.momentUploadPreview.appendChild(wrap);
        });
        // if (refs.momentUploadHint) {
        //     refs.momentUploadHint.textContent = `已选 ${state.momentDraftImages.length} / 9`;
        // }
        if (refs.momentUploadHint) {
            const count = (state.momentDraftImages || []).length;
            // 如果没图了，显示回“最多 9 张”，否则显示“已选 X / 9”
            refs.momentUploadHint.textContent = count > 0 ? `已选 ${count} / 9` : '最多 9 张';
        }
    }

    function openMomentsForUser(userId) {
        state.momentFilterUserId = userId || null;
        send('MOMENT_LIST');
        switchView('moments');
    }

    // 默认头像URL
    const DEFAULT_AVATAR = 'https://2bpic.oss-cn-beijing.aliyuncs.com/2025/12/04/6930f7a2c12fc.png';

    function getMessageType(message) {
        if (!message || message.messageType === undefined || message.messageType === null) return 'TEXT';
        const raw = message.messageType;
        if (typeof raw === 'string') return raw.toUpperCase();
        if (typeof raw === 'object' && raw.name) return raw.name.toString().toUpperCase();
        return raw.toString().toUpperCase();
    }

    function isBurnMessage(message) {
        if (!message) return false;
        const type = getMessageType(message);
        if (type === 'BURN_AFTER_READING') return true;
        return typeof message.burnDelay === 'number' && message.burnDelay > 0;
    }

    function getBurnExpireAt(message) {
        if (!message || !isBurnMessage(message)) return null;
        const baseTs = typeof message.timestamp === 'number' ? message.timestamp : Number(message.timestamp) || Date.now();
        return baseTs + getBurnDelaySeconds(message) * 1000;
    }

    function isBurned(message) {
        if (!message || !isBurnMessage(message)) return false;
        const expireAt = getBurnExpireAt(message);
        if (!expireAt) return false;
        if (Date.now() >= expireAt) {
            message._burned = true;
        }
        return !!message._burned;
    }

    function logBurnDebug(message) {
        try {
            const info = {
                id: message?.id,
                rawType: message?.messageType,
                normalizedType: getMessageType(message),
                burnDelay: message?.burnDelay,
                timestamp: message?.timestamp,
                expireAt: getBurnExpireAt(message),
                isBurnMessage: isBurnMessage(message),
                isBurned: isBurned(message)
            };
            state.lastBurnDebug = info;
            console.log('[burn-debug]', info);
        } catch (e) {
            console.warn('burn-debug failed', e);
        }
    }

    function buildMessageNode(message) {
        if (isBurned(message)) {
            return buildBurnPlaceholder(message);
        }
        normalizeTomatoTarget(message);
        const wrapper = document.createElement('div');
        wrapper.className = 'message-wrapper';
        if (message && message.id) {
            wrapper.dataset.messageId = message.id;
        }
        if (message && message.fromUserId != null) {
            wrapper.dataset.userId = String(message.fromUserId);
        }
        if (state.selectMode) {
            wrapper.classList.add('select-mode');
            if (state.selectedMessageIds.has(message.id)) {
                wrapper.classList.add('selected');
            }
        }
        const isMe = message.fromUserId === (state.currentUser && state.currentUser.id);
        if (isMe) {
            wrapper.classList.add('me');
        }

        // 创建头像
        const avatar = document.createElement('img');
        avatar.className = 'message-avatar';
        avatar.src = message.fromUserAvatarUrl || DEFAULT_AVATAR;
        avatar.alt = message.fromUserDisplayName || '用户';
        if (message && message.fromUserId != null) {
            avatar.dataset.userId = String(message.fromUserId);
        }

        let selector = null;
        if (state.selectMode && message && message.id) {
            selector = document.createElement('label');
            selector.className = 'message-select';
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'message-select-checkbox';
            checkbox.checked = state.selectedMessageIds.has(message.id);
            checkbox.addEventListener('click', evt => {
                evt.stopPropagation();
                toggleMessageSelection(message.id, checkbox.checked);
            });
            const mark = document.createElement('span');
            mark.className = 'message-select-mark';
            selector.append(checkbox, mark);
            wrapper.appendChild(selector);
        }

        if (message && message.recalled) {
            const placeholder = buildRecalledPlaceholder(message);
            if (isMe) {
                wrapper.appendChild(placeholder);
                wrapper.appendChild(avatar);
            } else {
                wrapper.appendChild(avatar);
                wrapper.appendChild(placeholder);
            }
            return wrapper;
        }

        const node = document.createElement('div');
        node.className = 'message';
        const type = getMessageType(message);
        if (isMe) {
            node.classList.add('me');
        }
        if (type === 'DICE_RESULT' || type === 'RPS_RESULT') {
            node.classList.add('game');
        }
        if (isBurnMessage(message)) {
            node.classList.add('burn-after');
        }

        const meta = document.createElement('div');
        meta.className = 'meta';
        const sender = message.fromUserDisplayName || '系统';
        const timeText = message.timestamp ? new Date(message.timestamp).toLocaleTimeString() : '';
        meta.textContent = timeText ? `${sender} · ${timeText}` : sender;
        node.appendChild(meta);

        const body = document.createElement('div');
        body.className = 'content';
        const quote = message.quote || message.quotedMessage || message.quoted;
        if (quote) {
            const quoteNode = buildQuoteBlock(quote);
            if (quoteNode) {
                body.appendChild(quoteNode);
            }
        }
        if (type === 'FILE_LINK') {
            body.appendChild(buildFileMessage(parseFileContent(message.content), message));
        } else if (type === 'DICE_RESULT') {
            body.appendChild(buildDiceResult(message.content));
        } else if (type === 'RPS_RESULT') {
            body.appendChild(buildRpsResult(message.content));
        } else if (type === 'FORWARD_CARD') {
            body.appendChild(buildForwardCard(message));
        } else if (type === 'ACTION' && (message.content || '').toUpperCase() === TOMATO_ACTION) {
            body.insertAdjacentHTML('beforeend', '<strong>🍅 丢给你一个番茄！</strong>');
        } else {
            body.insertAdjacentHTML('beforeend', renderMessageContent(message.content || ''));
        }
        node.appendChild(body);

        if (isBurnMessage(message)) {
            const badge = document.createElement('div');
            badge.className = 'burn-badge';
            const expireAt = getBurnExpireAt(message);
            const remainingSeconds = expireAt
                ? Math.max(0, Math.ceil((expireAt - Date.now()) / 1000))
                : getBurnDelaySeconds(message);
            badge.textContent = formatBurnBadgeText(remainingSeconds);
            node.appendChild(badge);
        }

        // 根据是否是自己的消息，调整头像和消息的顺序
        if (isMe) {
            wrapper.appendChild(node);
            wrapper.appendChild(avatar);
        } else {
            wrapper.appendChild(avatar);
            wrapper.appendChild(node);
        }

        if (state.selectMode && message && message.id) {
            wrapper.addEventListener('click', evt => {
                if (evt.target.closest('.message-select')) return;
                toggleMessageSelection(message.id);
            });
        }
        wrapper.addEventListener('contextmenu', evt => showContextMenu(evt, message));

        return wrapper;
    }

    function formatBurnBadgeText(seconds) {
        const value = Number.isFinite(seconds) ? Math.max(0, Math.ceil(seconds)) : DEFAULT_BURN_DELAY;
        return `🔥 阅后即焚 · ${value}s`;
    }

    function getBurnDelaySeconds(message) {
        const raw = message && typeof message.burnDelay === 'number' ? message.burnDelay : null;
        if (raw && raw > 0) return raw;
        return DEFAULT_BURN_DELAY;
    }

    function stopBurnCountdown(message) {
        if (message && message._burnCountdownInterval) {
            clearInterval(message._burnCountdownInterval);
            message._burnCountdownInterval = null;
        }
    }

    function startBurnCountdown(message, expireAt, badge) {
        if (!message || !badge || !expireAt) return;
        stopBurnCountdown(message);
        const update = () => {
            const remainingMs = expireAt - Date.now();
            const remainingSec = Math.max(0, Math.ceil(remainingMs / 1000));
            badge.textContent = formatBurnBadgeText(remainingSec);
            if (remainingMs <= 0) {
                stopBurnCountdown(message);
            }
        };
        update();
        message._burnCountdownInterval = setInterval(() => {
            if (!badge.isConnected) {
                stopBurnCountdown(message);
                return;
            }
            update();
        }, 1000);
    }

    function setupBurnTimer(message, wrapper) {
        if (!message || !isBurnMessage(message)) return;
        const expireAt = getBurnExpireAt(message);
        if (!expireAt) return;
        const targetWrapper = wrapper || document.querySelector(`[data-message-id="${message.id}"]`);
        const badge = targetWrapper ? targetWrapper.querySelector('.burn-badge') : null;
        if (isBurned(message)) {
            stopBurnCountdown(message);
            const target = targetWrapper || wrapper;
            if (target && target.parentElement) {
                target.replaceWith(buildBurnPlaceholder(message));
            }
            return;
        }

        startBurnCountdown(message, expireAt, badge);

        if (message._burnTimerStarted) return;
        message._burnTimerStarted = true;
        const remaining = expireAt - Date.now();
        message._burnTimeout = setTimeout(() => {
            message._burned = true;
            stopBurnCountdown(message);
            const target = document.querySelector(`[data-message-id="${message.id}"]`) || wrapper;
            if (target && target.parentElement) {
                target.replaceWith(buildBurnPlaceholder(message));
            }
        }, Math.max(0, remaining));
    }

    function buildBurnPlaceholder(message) {
        const placeholder = document.createElement('div');
        placeholder.className = 'burn-placeholder';
        if (message && message.id) {
            placeholder.dataset.messageId = message.id;
        }
        placeholder.textContent = '消息已焚毁';
        return placeholder;
    }

    function isTomatoActionMessage(message) {
        return (
            message &&
            getMessageType(message) === 'ACTION' &&
            (message.content || '').toUpperCase() === TOMATO_ACTION
        );
    }

    function isTomatoForCurrentUser(message) {
        if (!isTomatoActionMessage(message) || !state.currentUser) return false;
        const targetId = message.targetUserId || message.toUser;
        if (targetId != null) {
            return String(targetId) === String(state.currentUser.id);
        }
        if (message.fromUserId != null && String(message.fromUserId) === String(state.currentUser.id)) {
            return false;
        }
        // 缺少显式目标时，默认当前用户（只要不是发送者）也需要看到动画
        return true;
    }

    // 确定番茄动画的目标用户，优先使用消息提供的目标，其次回落到当前用户自己
    // 这样在受击方打开聊天界面时，番茄会飞向自己的最新头像
    function resolveTomatoTargetUserId(message) {
        if (!message) return null;
        if (message.targetUserId != null) return message.targetUserId;
        if (message.toUser != null) return message.toUser;
        if (isTomatoForCurrentUser(message) && state.currentUser) {
            return state.currentUser.id;
        }
        return null;
    }

    // 兼容后端字段命名差异，确保有 toUser/targetUserId
    function normalizeTomatoTarget(message) {
        if (!message) return;
        if (message.targetUserId == null && message.toUser != null) {
            message.targetUserId = message.toUser;
        } else if (message.toUser == null && message.targetUserId != null) {
            message.toUser = message.targetUserId;
        }
    }

    function markTomatoPlayedForCurrent(message, serial) {
        if (!isTomatoForCurrentUser(message)) return;
        if (message) {
            message._tomatoPlayedForCurrent = true;
            if (serial != null) message._tomatoPlayedSerial = serial;
        }
        if (!message || !message.roomId || !message.id) return;
        const stored = findMessageById(message.roomId, message.id);
        if (stored) {
            stored._tomatoPlayedForCurrent = true;
            if (serial != null) stored._tomatoPlayedSerial = serial;
        }
    }

    function playLatestPendingTomato(roomId) {
        if (!roomId || state.currentMode !== 'chat') return;
        const list = state.messagesByRoom[roomId] || [];
        for (let i = list.length - 1; i >= 0; i--) {
            const msg = list[i];
            if (isTomatoForCurrentUser(msg) && !msg._tomatoPlayedForCurrent) {
                playTomatoAnimation(msg);
                return;
            }
        }
    }

    function playLatestTomatoForEntry(roomId) {
        if (!roomId || state.currentMode !== 'chat') return;
        const list = state.messagesByRoom[roomId] || [];
        for (let i = list.length - 1; i >= 0; i--) {
            const msg = list[i];
            if (!isTomatoForCurrentUser(msg)) continue;
            // 已经播放过的番茄不重复飞
            if (msg._tomatoPlayedForCurrent) continue;
            if (msg._tomatoPlayedSerial === state.roomEnterSerial) continue;
            playTomatoAnimation(msg, state.roomEnterSerial);
            return;
        }
    }

    function scheduleTomatoPlayback(roomId) {
        if (!roomId) return;
        const attempts = [0, 250, 700, 1300];
        attempts.forEach(delay => {
            setTimeout(() => {
                if (state.currentRoomId === roomId && state.currentMode === 'chat') {
                    playLatestTomatoForEntry(roomId);
                    playLatestPendingTomato(roomId);
                }
            }, delay);
        });
    }

    function getTomatoTargetAvatar(senderUserId, targetUserId) {
        if (!refs.messageList) return null;
        const senderId = senderUserId != null ? String(senderUserId) : null;
        const expectedTargetId = targetUserId != null ? String(targetUserId) : null;
        const room = state.rooms[state.currentRoomId];
        const preferredTargetId = room ? getTargetUserIdForRoom(room) : null;
        const wrappers = Array.from(refs.messageList.querySelectorAll('.message-wrapper')).reverse();

        const pickAvatar = predicate => {
            for (const wrap of wrappers) {
                const userId = wrap.dataset.userId;
                if (!userId || (senderId && userId === senderId)) continue;
                if (!predicate(userId)) continue;
                const avatar = wrap.querySelector('.message-avatar');
                if (avatar) return avatar;
            }
            return null;
        };

        if (expectedTargetId != null) {
            const direct = pickAvatar(uid => uid === expectedTargetId);
            if (direct) return direct;
        }
        if (preferredTargetId != null) {
            const preferred = pickAvatar(uid => uid === String(preferredTargetId));
            if (preferred) return preferred;
        }
        return pickAvatar(() => true);
    }

    function ensureTomatoLayer() {
        let layer = document.getElementById('tomato-layer');
        if (!layer) {
            layer = document.createElement('div');
            layer.id = 'tomato-layer';
            layer.style.position = 'fixed';
            layer.style.left = '0';
            layer.style.top = '0';
            layer.style.width = '100vw';
            layer.style.height = '100vh';
            layer.style.pointerEvents = 'none';
            layer.style.zIndex = '9999';
            document.body.appendChild(layer);
        }
        return layer;
    }

    function playTomatoAnimation(message) {
        if (!refs.messageList) return;
        const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
        const vw = window.innerWidth || document.documentElement.clientWidth || 1920;
        const vh = window.innerHeight || document.documentElement.clientHeight || 1080;
        const PADDING = 24; // 让飞行轨迹保持在视口内一点点
        console.log('[tomato] play animation', {
            messageId: message && message.id,
            from: message && message.fromUserId,
            target: message && (message.targetUserId || message.toUser),
            roomId: message && message.roomId,
            currentRoomId: state.currentRoomId
        });
        const container = document.querySelector('.chat-area');
        const containerRect = container ? container.getBoundingClientRect() : document.body.getBoundingClientRect();
        const listRect = refs.messageList.getBoundingClientRect();
        const senderUserId = message && message.fromUserId;
        const targetUserId = resolveTomatoTargetUserId(message);
        const fromMe = senderUserId && state.currentUser && senderUserId === state.currentUser.id;
        let startX = fromMe ? containerRect.right - 80 : containerRect.left + 20;
        let startY = containerRect.bottom - 120;

        let targetX = containerRect.left + containerRect.width / 2 + (fromMe ? -40 : 40);
        let targetY = containerRect.top + containerRect.height * 0.35 + Math.random() * 80;
        const targetAvatar = getTomatoTargetAvatar(senderUserId, targetUserId);
        if (targetAvatar) {
            const targetRect = targetAvatar.getBoundingClientRect();
            targetX = targetRect.left + targetRect.width / 2;
            targetY = targetRect.top + targetRect.height / 2;
        }

        // 兜底：确保坐标在视口范围内，避免番茄飞在屏幕之外
        startX = clamp(startX, PADDING, vw - PADDING);
        startY = clamp(startY, PADDING, vh - PADDING);
        targetX = clamp(targetX, PADDING, vw - PADDING);
        targetY = clamp(targetY, PADDING, vh - PADDING);

        const dx = targetX - startX;
        const dy = targetY - startY;
        console.log('[tomato] coords', { startX, startY, targetX, targetY, dx, dy });

        const layer = ensureTomatoLayer();
        const tomato = document.createElement('img');
        tomato.src = TOMATO_IMG;
        tomato.className = 'tomato-projectile';
        tomato.style.left = `${startX}px`;
        tomato.style.top = `${startY}px`;
        tomato.style.setProperty('--dx', `${dx}px`);
        tomato.style.setProperty('--dy', `${dy}px`);
        layer.appendChild(tomato);

        const handleFlightEnd = () => {
            tomato.removeEventListener('animationend', handleFlightEnd);
            tomato.src = TOMATO_SPLAT_IMG;
            tomato.classList.add('tomato-splat');
            tomato.style.animation = 'tomato-splat 420ms ease-out forwards';
            tomato.addEventListener('animationend', () => tomato.remove(), { once: true });
        };

        // 继续放慢飞行速度以便看清轨迹
        tomato.style.animation = 'tomato-flight 1600ms cubic-bezier(0.2, 0.8, 0.3, 1) forwards';
        tomato.addEventListener('animationend', handleFlightEnd);
        markTomatoPlayedForCurrent(message, arguments.length > 1 ? arguments[1] : undefined);
    }

    // 调试用：在控制台运行 window.debugTomato() 可触发一次飞向自己头像的番茄
    window.debugTomato = () => {
        const msg = {
            id: 'debug-tomato-' + Date.now(),
            roomId: state.currentRoomId,
            fromUserId: '__debug__',
            targetUserId: state.currentUser && state.currentUser.id,
            content: TOMATO_ACTION,
            messageType: 'ACTION'
        };
        playTomatoAnimation(msg);
    };

    /**
     * 解析消息内容，支持Markdown基本格式和换行
     */
    function renderMessageContent(text) {
        if (!text) return '';
        
        // 转义HTML特殊字符，防止XSS
        let html = text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
        
        // 处理代码块 ```code```
        html = html.replace(/```([\s\S]*?)```/g, '<pre class="code-block">$1</pre>');
        
        // 处理行内代码 `code`
        html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');
        
        // 处理粗体 **text** 或 __text__
        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');
        
        // 处理斜体 *text* 或 _text_
        html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        html = html.replace(/_([^_]+)_/g, '<em>$1</em>');
        
        // 处理删除线 ~~text~~
        html = html.replace(/~~([^~]+)~~/g, '<del>$1</del>');
        
        // 处理链接 [text](url)
        html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
        
        // 处理换行符 \n 转为 <br>
        html = html.replace(/\\n/g, '<br>');
        html = html.replace(/\n/g, '<br>');
        
        return html;
    }

    function buildFileMessage(file, message) {
        const wrap = document.createElement('div');
        wrap.className = 'file-message';
        const name = document.createElement('div');
        name.className = 'file-name';
        name.textContent = file.name || '文件';
        wrap.appendChild(name);

        const resolvedUrl = getSafeFileUrl(file, message);

        if (file.url && isImage(file.url, file.mimeType)) {
            const img = document.createElement('img');
            img.className = 'preview';
            img.src = resolvedUrl || file.url;
            img.alt = file.name || 'image';
            wrap.appendChild(img);
        }

        const link = document.createElement('a');
        const href = resolvedUrl || file.url || '#';
        link.href = href;
        link.target = '_blank';
        link.rel = 'noreferrer noopener';
        if ((href && href.startsWith('blob:')) || (file.url && file.url.startsWith('data:'))) {
            link.download = file.name || 'file';
        }
        link.textContent = resolvedUrl || file.url ? '打开/下载' : '无可用链接';
        wrap.appendChild(link);

        const meta = document.createElement('div');
        meta.className = 'file-meta';
        const parts = [];
        if (file.mimeType) parts.push(file.mimeType);
        if (file.size) parts.push(formatFileSize(file.size));
        meta.textContent = parts.join(' · ');
        wrap.appendChild(meta);
        return wrap;
    }

    function buildDiceResult(content) {
        const wrap = document.createElement('div');
        wrap.className = 'game-result';
        const face = document.createElement('div');
        face.className = 'dice-face';
        face.textContent = '🎲';
        const text = document.createElement('div');
        text.className = 'result-text';
        text.textContent = `掷出了 ${content || '?'}`;
        const sub = document.createElement('div');
        sub.className = 'sub-text';
        sub.textContent = '随机骰子结果';
        wrap.append(face, text, sub);
        return wrap;
    }

    function buildRpsResult(content) {
        const wrap = document.createElement('div');
        wrap.className = 'game-result';
        const key = (content || '').toString().toUpperCase();
        const handMap = { ROCK: '✊', PAPER: '✋', SCISSORS: '✌️' };
        const labelMap = { ROCK: '石头', PAPER: '布', SCISSORS: '剪刀' };
        const hand = document.createElement('div');
        hand.className = 'rps-hand';
        hand.textContent = handMap[key] || '✊';
        const text = document.createElement('div');
        text.className = 'result-text';
        text.textContent = `出了 ${labelMap[key] || '石头'}`;
        const sub = document.createElement('div');
        sub.className = 'sub-text';
        sub.textContent = '猜拳随机结果';
        wrap.append(hand, text, sub);
        return wrap;
    }

    function parseForwardCardContent(raw) {
        if (!raw) return { title: '聊天记录', items: [], total: 0, messageIds: [], raw: null };
        try {
            const obj = JSON.parse(raw);
            const items = Array.isArray(obj.items) ? obj.items : [];
            const total = typeof obj.total === 'number' ? obj.total : items.length;
            return {
                title: obj.title || '聊天记录',
                items,
                total,
                messageIds: Array.isArray(obj.messageIds) ? obj.messageIds : [],
                raw: obj
            };
        } catch (e) {
            return { title: '聊天记录', items: [], total: 0, messageIds: [], raw: null };
        }
    }

    function buildForwardCard(message) {
        const data = parseForwardCardContent(message.content);
        const wrap = document.createElement('div');
        wrap.className = 'forward-card';
        const header = document.createElement('div');
        header.className = 'forward-card-header';
        const badge = document.createElement('span');
        badge.className = 'forward-card-badge';
        badge.textContent = '聊天记录';
        const title = document.createElement('div');
        title.className = 'forward-card-title';
        title.textContent = data.title || '聊天记录';
        header.append(badge, title);
        wrap.appendChild(header);

        const list = document.createElement('div');
        list.className = 'forward-card-list';
        const rows = buildForwardRows(data).slice(0, 4);
        rows.forEach(rowData => {
            const row = document.createElement('div');
            row.className = 'forward-card-item';
            const text = document.createElement('div');
            text.className = 'forward-card-text';
            text.textContent = rowData.text;
            row.appendChild(text);
            if (rowData.time) {
                const time = document.createElement('div');
                time.className = 'forward-card-time muted';
                time.textContent = rowData.time;
                row.appendChild(time);
            }
            list.appendChild(row);
        });
        if ((data.items || []).length > rows.length) {
            const more = document.createElement('div');
            more.className = 'forward-card-more';
            more.textContent = '...';
            list.appendChild(more);
        }
        if (!(data.items || []).length) {
            const empty = document.createElement('div');
            empty.className = 'forward-card-item muted';
            empty.textContent = '暂无摘要';
            list.appendChild(empty);
        }
        wrap.appendChild(list);

        const footer = document.createElement('div');
        footer.className = 'forward-card-footer muted';
        const total = data.total || (data.items ? data.items.length : 0);
        footer.textContent = `共 ${total} 条聊天记录`;
        wrap.appendChild(footer);

        wrap.addEventListener('click', () => {
            renderForwardDetail(data);
        });
        return wrap;
    }

    function isImage(url, mimeType) {
        if (!url) return false;
        if (mimeType && mimeType.startsWith('image/')) return true;
        if (url.startsWith('data:')) {
            return url.startsWith('data:image/');
        }
        return /\.(png|jpg|jpeg|gif|webp)$/i.test(url.split('?')[0]);
    }

    function parseFileContent(raw) {
        if (!raw) return {};
        try {
            const obj = JSON.parse(raw);
            return obj && typeof obj === 'object' ? obj : {};
        } catch (e) {
            return {};
        }
    }

    function formatFileSize(size) {
        if (!size && size !== 0) return '';
        const kb = size / 1024;
        if (kb < 1024) return `${Math.round(kb)} KB`;
        return `${(kb / 1024).toFixed(2)} MB`;
    }

    function getSafeFileUrl(file, message) {
        if (!file || !file.url) return null;
        if (!file.url.startsWith('data:')) return file.url;
        if (message && message._fileObjectUrl) return message._fileObjectUrl;
        const blob = dataUrlToBlob(file.url);
        if (!blob) return file.url;
        const objectUrl = URL.createObjectURL(blob);
        if (message) {
            message._fileObjectUrl = objectUrl;
        }
        return objectUrl;
    }

    function dataUrlToBlob(dataUrl) {
        try {
            const [header, dataPart] = dataUrl.split(',', 2);
            if (!dataPart) return null;
            const match = header.match(/^data:(.*?)(;base64)?$/);
            const mimeType = match && match[1] ? match[1] : 'application/octet-stream';
            const isBase64 = match && !!match[2];
            if (isBase64) {
                const binary = atob(dataPart);
                const bytes = new Uint8Array(binary.length);
                for (let i = 0; i < binary.length; i++) {
                    bytes[i] = binary.charCodeAt(i);
                }
                return new Blob([bytes], { type: mimeType });
            }
            return new Blob([decodeURIComponent(dataPart)], { type: mimeType });
        } catch (e) {
            console.warn('Failed to convert data URL to blob', e);
            return null;
        }
    }

    function toggleRoomModal(visible) {
        refs.roomModal.classList.toggle('hidden', !visible);
        if (!visible) {
            refs.roomNameInput.value = '';
            refs.roomMembersBox.querySelectorAll('input[type="checkbox"]').forEach(cb => (cb.checked = false));
        }
    }

    function toggleProfileModal(visible) {
        refs.profileModal.classList.toggle('hidden', !visible);
        if (visible && state.currentUser) {
            refs.profileDisplayName.value = state.currentUser.displayName || '';
            refs.profileAvatar.value = state.currentUser.avatarUrl || '';
            refs.profileSignature.value = state.currentUser.signature || '';
        }
    }

    function toggleViewProfileModal(visible) {
        refs.viewProfileModal.classList.toggle('hidden', !visible);
    }

    function updateProfileUI(user) {
        if (!user) return;
        state.currentUser = user;
        refs.currentName.textContent = user.displayName || user.username;
        refs.currentSignature.textContent = user.signature || '这个人很低调，还没有签名';
        if (user.avatarUrl) {
            refs.currentAvatar.src = user.avatarUrl;
        } else {
            refs.currentAvatar.src = DEFAULT_AVATAR;
        }
    }

    function showViewProfile(user) {
        if (!user) return;
        state.viewProfileUser = user;
        refs.viewProfileName.textContent = user.displayName || user.username;
        refs.viewProfileUsername.textContent = `用户：${user.username}`;
        refs.viewProfileSignature.textContent = user.signature || '这个人很低调，还没有签名';
        refs.viewProfileAvatar.src = user.avatarUrl || DEFAULT_AVATAR;
        toggleViewProfileModal(true);
    }

    function toggleSettingsMenu(forceShow) {
        if (!refs.settingsMenu) return;
        const shouldShow = typeof forceShow === 'boolean' ? forceShow : refs.settingsMenu.classList.contains('hidden');
        refs.settingsMenu.classList.toggle('hidden', !shouldShow);
    }
    function initSidebarIcons() {
        const icons = document.querySelectorAll('.sidebar-icon');
        icons.forEach(icon => {
            icon.addEventListener('click', () => {
                // 移除所有激活状态
                icons.forEach(i => i.classList.remove('active'));
                // 添加当前激活状态
                icon.classList.add('active');
        //add 显示当前用户信息
                document.querySelector('.profile-card').classList.remove('hidden');
                // 处理视图切换
                const view = icon.dataset.view;
                if (view === 'moments') {
                    state.momentFilterUserId = null;
                }
                switchView(view);
            });
        });
    }

    function switchView(view) {
        const welcomePanel = document.getElementById('post-login-welcome');
        const chatContent = document.getElementById('chat-content');
        welcomePanel.classList.add('hidden');
        chatContent.classList.remove('hidden');

        const isFullScreen = window.innerHeight >= 0.95 * screen.height && window.innerWidth >= 1081;
        const hiddenClass = isFullScreen ? 'hidden' : 'hidden-preserve';

        // document.querySelectorAll('.sidebar-section, #chat-section, #tree-hole-section, #moments-panel').forEach(el => {
        //     el.classList.add('hidden');
        // });
        document.querySelectorAll('.sidebar-section, #chat-section, #tree-hole-section, #moments-panel, #ai-welcome-placeholder').forEach(el => {
            el?.classList.add('hidden');
        });
        
        switch(view) {
            case 'chat':
                document.querySelector('#chat-section').classList.remove('hidden');
                document.getElementById('game-btn')?.classList.remove('hidden');
                if (state.currentRoomId) {
                    const room = state.rooms[state.currentRoomId];
                    const memberCount = room.memberIds ? room.memberIds.length : 0;
                    refs.chatTitle.textContent = room.name;
                    refs.chatSubtitle.textContent = room.type === 'PRIVATE' ? '私聊' : `成员 ${memberCount}`;
                } else {
                    refs.chatTitle.textContent = '请选择聊天对象';
                    refs.chatSubtitle.textContent = '还没有打开任何房间';
                }
                break;

            case 'user':
                document.querySelector('.profile-card').classList.remove('hidden');
                document.querySelector('#chat-section').classList.remove('hidden');
                // 新增：重置标题为默认值（无房间时）
                if (!state.currentRoomId) {
                    refs.chatTitle.textContent = '请选择聊天对象';
                    refs.chatSubtitle.textContent = '还没有打开任何房间';
                }
                break;
                
            // 好友视图：显示好友列表 + 重置默认标题
            case 'friends':
                document.getElementById('game-btn')?.classList.remove('hidden');
                document.querySelector('[data-section="friends"]').classList.remove('hidden');
                document.querySelector('#chat-section').classList.remove('hidden');
                if (state.currentRoomId) {
                    setCurrentRoom(state.currentRoomId);
                } else {
                    // 关键修复：无房间时强制重置标题
                    refs.chatTitle.textContent = '请选择聊天对象';
                    refs.chatSubtitle.textContent = '还没有打开任何房间';
                }
                break;
                
            // 房间视图：显示房间列表 + 重置默认标题
            case 'rooms':
                document.getElementById('game-btn')?.classList.remove('hidden');
                document.querySelector('[data-section="rooms"]').classList.remove('hidden');
                document.querySelector('#chat-section').classList.remove('hidden');
                if (state.currentRoomId) {
                    setCurrentRoom(state.currentRoomId);
                } else {
                    // 关键修复：无房间时强制重置标题
                    refs.chatTitle.textContent = '请选择聊天对象';
                    refs.chatSubtitle.textContent = '还没有打开任何房间';
                }
                break;

            case 'ai-clones':
                document.getElementById('close-chat-btn').classList.remove('visible');
                document.querySelector('[data-section="ai-clones"]').classList.remove('hidden');
                document.querySelector('#chat-section').classList.remove('hidden');
            document.getElementById('manage-room-btn').classList.add('hidden');    
                //  显示机器人欢迎占位页
                refs.aiWelcomePlaceholder?.classList.remove('hidden');

                refs.chatTitle.textContent = 'AI 分身';
                refs.chatSubtitle.textContent = '发布或添加 AI 分身助手';
                break;

            case 'moments':
                document.getElementById('manage-room-btn').classList.add('hidden');
                document.getElementById('close-chat-btn').classList.remove('visible');
                document.querySelector('#moments-panel')?.classList.remove('hidden');
                refs.chatTitle.textContent = '朋友圈';
                refs.chatSubtitle.textContent = '看看好友的最新动态';
                document.getElementById('game-btn')?.classList.add('hidden');
                if (!state.moments.length) {
                    send('MOMENT_LIST');
                } else {
                    renderMoments();
                }
                break;

            case 'treehole':
                switchMode('treehole');
                document.querySelector('#tree-hole-section').classList.remove('hidden');
                break;
        }
    }


    function closeChat() {
        document.getElementById('manage-room-btn').classList.add('hidden');
        // 重置当前房间ID
        state.currentRoomId = null;
        // 隐藏关闭按钮
        document.getElementById('close-chat-btn').classList.remove('visible');
        // 隐藏上下文菜单
        hideContextMenu();
        // 清除引用
        clearQuote();
        // 重新渲染房间列表（移除选中状态）
        renderRooms();

        // 判断当前是否在AI分身视图
        const isAiCloneView = document.querySelector('[data-section="ai-clones"]')?.classList.contains('hidden') === false;
        
        if (isAiCloneView) {
            // AI广场视图：显示机器人机器人占位页
            refs.aiWelcomePlaceholder?.classList.remove('hidden');
            refs.chatTitle.textContent = 'AI 分身';
            refs.chatSubtitle.textContent = '发布或添加 AI 分身助手';
            refs.messageList.innerHTML = ''; // 清空消息列表
            renderMessages();
        } else {
            // 其他视图：显示默认空状态
            refs.aiWelcomePlaceholder?.classList.add('hidden');
            refs.chatTitle.textContent = '请选择聊天对象';
            refs.chatSubtitle.textContent = '还没有打开任何房间';
            renderMessages();
        }
    }





    // ADD: 群管理相关函数
    function toggleManageRoomModal(show) {
        if (!refs.manageRoomModal) return;
        refs.manageRoomModal.classList.toggle('hidden', !show);
        refs.manageRoomHint && (refs.manageRoomHint.textContent = '');
        if (show) {
            const room = state.currentRoomId ? state.rooms[state.currentRoomId] : null;
            renderManageRoomModal(room);
        }
    }

    function renderManageRoomModal(room) {
        if (!room || room.type !== 'GROUP') {
            toggleManageRoomModal(false);
            return;
        }
        refs.manageRoomName && (refs.manageRoomName.value = room.name || '');
        refs.manageRoomDesc && (refs.manageRoomDesc.value = room.description || '');
        refs.manageRoomAvatar && (refs.manageRoomAvatar.value = room.avatarUrl || '');
        if (refs.manageRoomPublic) {
            refs.manageRoomPublic.checked = room.allowPublicJoin !== false;
        }
        renderManageMembers(room);
        renderManageMemberOptions(room);
    }

    function renderManageMembers(room) {
        if (!refs.manageRoomMembers || !room) return;
        refs.manageRoomMembers.innerHTML = '';
        (room.memberIds || []).forEach(memberId => {
            const li = document.createElement('li');
            const label = document.createElement('div');
            const name = resolveDisplayNameById(memberId);
            label.textContent = memberId === room.ownerId ? `${name}（群主）` : name;
            li.appendChild(label);
            if (memberId !== room.ownerId) {
                const btn = document.createElement('button');
                btn.className = 'ghost-btn danger';
                btn.textContent = '移除';
                btn.addEventListener('click', () => {
                    send('ROOM_REMOVE_MEMBER', { roomId: room.id, targetUserId: memberId });
                });
                li.appendChild(btn);
            }
            refs.manageRoomMembers.appendChild(li);
        });
    }

    function renderManageMemberOptions(room) {
        if (!refs.manageRoomAddSelect || !room) return;
        const existing = new Set(room.memberIds || []);
        refs.manageRoomAddSelect.innerHTML = '';
        const candidates = (state.friends || []).filter(f => !existing.has(f.id));
        if (!candidates.length) {
            const opt = document.createElement('option');
            opt.disabled = true;
            opt.selected = true;
            opt.textContent = '暂无可添加好友';
            refs.manageRoomAddSelect.appendChild(opt);
            return;
        }
        candidates.forEach(friend => {
            const opt = document.createElement('option');
            opt.value = friend.id;
            opt.textContent = friend.displayName || friend.username || friend.id;
            refs.manageRoomAddSelect.appendChild(opt);
        });
    }

    function resolveDisplayNameById(userId) {
        if (!userId) return '';
        if (state.currentUser && state.currentUser.id === userId) {
            return state.currentUser.displayName || state.currentUser.username || userId;
        }
        const friend = (state.friends || []).find(f => f.id === userId);
        if (friend) return friend.displayName || friend.username || userId;
        const clones = [...(state.aiClonesMine || []), ...(state.aiClonesAll || [])];
        const clone = clones.find(c => c.id === userId);
        if (clone) return clone.displayName || clone.id;
        return userId;
    }

    function removeRoomLocal(roomId) {
        delete state.rooms[roomId];
        state.roomOrder = state.roomOrder.filter(id => id !== roomId);
        delete state.messagesByRoom?.[roomId];
        if (state.currentRoomId === roomId) {
            state.currentRoomId = null;
            refs.chatTitle.textContent = '请选择聊天对象';
            refs.chatSubtitle.textContent = '还没有打开任何房间';
            renderMessages();
        }
        renderRooms();
    }



    // 事件绑定
    refs.modeChatBtn?.addEventListener('click', () => switchMode('chat'));
    refs.modeTreeHoleBtn?.addEventListener('click', () => switchMode('treehole'));

    refs.loginForm.addEventListener('submit', evt => {
        evt.preventDefault();
        const payload = {
            username: refs.usernameInput.value.trim(),
            password: refs.passwordInput.value
        };
        state.lastLoginPayload = payload;
        send('AUTH_LOGIN', payload);
    });

    refs.registerBtn.addEventListener('click', () => {
        const payload = {
            username: refs.usernameInput.value.trim(),
            password: refs.passwordInput.value
        };
        state.pendingRegister = payload;
        send('AUTH_REGISTER', payload);
    });

    refs.friendSearchBtn.addEventListener('click', () => {
        const keyword = refs.friendKeyword.value.trim();
        send('FRIEND_SEARCH', { keyword });
    });

    refs.refreshFriends.addEventListener('click', () => send('FRIEND_LIST'));
    refs.refreshFriendRequests.addEventListener('click', () => send('FRIEND_REQUEST_LIST'));

    refs.roomSearchBtn.addEventListener('click', () => {
        const keyword = refs.roomSearchInput.value.trim();
        send('ROOM_SEARCH', { keyword });
    });

    refs.aiCloneCreateBtn?.addEventListener('click', () => {
        const id = refs.aiCloneIdInput?.value.trim();
        const name = refs.aiCloneNameInput?.value.trim();
        const prompt = refs.aiClonePromptInput?.value.trim();
        const avatarUrl = refs.aiCloneAvatarInput?.value.trim();
        if (!id || !prompt) {
            if (refs.aiCloneHint) refs.aiCloneHint.textContent = '分身 ID 和提示词都不能为空';
            return;
        }
        send('AI_CLONE_CREATE', { id, displayName: name, prompt, avatarUrl });
    });

    refs.aiCloneSelfPromptBtn?.addEventListener('click', () => {
        if (refs.aiCloneHint) refs.aiCloneHint.textContent = '正在生成提示词，请稍候...';
        if (refs.aiCloneSelfPromptBtn) refs.aiCloneSelfPromptBtn.disabled = true;
        send('AI_CLONE_SELF_PROMPT');
    });

    refs.refreshAiClones?.addEventListener('click', () => send('AI_CLONE_LIST'));
    refs.openAiCloneModalBtn?.addEventListener('click', () => toggleAiCloneModal(true));
    refs.aiCloneModalCancel?.addEventListener('click', () => toggleAiCloneModal(false));

    refs.profileAvatarUploadBtn?.addEventListener('click', () => {
        refs.profileAvatarFile?.click();
    });
    refs.profileAvatarFile?.addEventListener('change', async evt => {
        const file = evt.target.files?.[0];
        if (!file) return;
        try {
            const url = await uploadImageToCdn(file);
            if (url) {
                refs.profileAvatar.value = url;
                alert('上传成功，已填入头像 URL');
            } else {
                alert('上传成功但未返回 URL');
            }
        } catch (e) {
            alert(e.message || '上传失败');
        } finally {
            evt.target.value = '';
        }
    });

    refs.aiCloneAvatarUploadBtn?.addEventListener('click', () => {
        refs.aiCloneAvatarFile?.click();
    });
    refs.aiCloneAvatarFile?.addEventListener('change', async evt => {
        const file = evt.target.files?.[0];
        if (!file) return;
        try {
            const url = await uploadImageToCdn(file);
            if (url) {
                refs.aiCloneAvatarInput.value = url;
                if (refs.aiCloneHint) refs.aiCloneHint.textContent = '上传成功，已填入头像 URL';
            } else if (refs.aiCloneHint) {
                refs.aiCloneHint.textContent = '上传成功但未返回 URL';
            }
        } catch (e) {
            if (refs.aiCloneHint) {
                refs.aiCloneHint.textContent = e.message || '上传失败';
            } else {
                alert(e.message || '上传失败');
            }
        } finally {
            evt.target.value = '';
        }
    });

    refs.messageForm.addEventListener('submit', evt => {
        evt.preventDefault();
        const content = refs.messageInput.value.trim();
        if (!content) return;
        const room = state.rooms[state.currentRoomId];
        if (!room) {
            alert('请先选择房间');
            return;
        }
        const isBurnMode = refs.burnToggle && refs.burnToggle.checked;
        const payload = {
            roomId: room.id,
            content,
            messageType: isBurnMode ? 'BURN_AFTER_READING' : 'TEXT'
        };
        if (state.currentQuote) {
            payload.quote = state.currentQuote;
        }
        if (isBurnMode) {
            payload.burnDelay = DEFAULT_BURN_DELAY;
        }
        send(room.type === 'PRIVATE' ? 'PRIVATE_MESSAGE' : 'ROOM_MESSAGE', payload);
        refs.messageInput.value = '';
        clearQuote();
    });

    refs.quoteClearBtn?.addEventListener('click', clearQuote);

    // 回车发送消息，Shift+Enter 换行
    if (refs.messageInput) {
        refs.messageInput.addEventListener('keydown', evt => {
            if (evt.key === 'Enter' && !evt.shiftKey) {
                evt.preventDefault();
                if (typeof refs.messageForm.requestSubmit === 'function') {
                    refs.messageForm.requestSubmit();
                } else {
                    refs.messageForm.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
                }
            }
        });
    }

    if (refs.treeHoleForm) {
        refs.treeHoleForm.addEventListener('submit', evt => {
            evt.preventDefault();
            const content = (refs.treeHoleInput?.value || '').trim();
            if (!content) return;
            send('POST_TREE_HOLE', { content });
        });
    }

    refs.treeHoleRefresh?.addEventListener('click', () => send('FETCH_TREE_HOLE'));

    refs.momentUploadBtn?.addEventListener('click', () => refs.momentUploadInput?.click());
    refs.momentUploadInput?.addEventListener('change', async evt => {
        const files = Array.from(evt.target.files || []);
        if (!files.length) return;
        const remain = 9 - state.momentDraftImages.length;
        const batch = files.slice(0, remain);
        if (batch.length < files.length && refs.momentUploadHint) {
            refs.momentUploadHint.textContent = '已达 9 张上限，已截取前 9 张';
        }
        try {
            for (const file of batch) {
                const url = await uploadImageToCdn(file);
                if (url) {
                    state.momentDraftImages.push(url);
                }
            }
            renderMomentDraftImages();
        } catch (e) {
            alert(e.message || '图片上传失败');
        } finally {
            evt.target.value = '';
        }
    });

    if (refs.momentForm) {
        refs.momentForm.addEventListener('submit', evt => {
            evt.preventDefault();
            const content = (refs.momentInput?.value || '').trim();
            const images = state.momentDraftImages || [];
            if (!content && images.length === 0) {
                alert('请输入文字或添加图片');
                return;
            }
            send('MOMENT_POST', { content, imageUrls: images });
        });
    }

    refs.momentsRefresh?.addEventListener('click', () => send('MOMENT_LIST'));

    if (refs.momentsList) {
        refs.momentsList.addEventListener('click', evt => {
            const likeBtn = evt.target.closest('[data-action="moment-like"]');
            if (likeBtn) {
                const momentId = likeBtn.dataset.id;
                if (momentId) {
                    send('MOMENT_LIKE', { momentId });
                }
            }
            const focusBtn = evt.target.closest('[data-action="moment-focus-comment"]');
            if (focusBtn) {
                const id = focusBtn.dataset.id;
                const input = refs.momentsList.querySelector(`.moment-comment-form[data-id="${id}"] input`);
                if (input) {
                    input.focus();
                }
            }
        });
        refs.momentsList.addEventListener('submit', evt => {
            const form = evt.target.closest('.moment-comment-form');
            if (!form) return;
            evt.preventDefault();
            const momentId = form.dataset.id;
            const input = form.querySelector('input[name="comment"]');
            const content = (input?.value || '').trim();
            if (!momentId || !content) return;
            send('MOMENT_COMMENT', { momentId, content });
            if (input) input.value = '';
        });
    }

    refs.sendFileBtn.addEventListener('click', () => {
        if (!state.currentRoomId) {
            alert('请先选择房间');
            return;
        }
        if (!refs.fileInput) {
            alert('文件选择控件未加载，请刷新页面后再试');
            return;
        }
        refs.fileInput.value = '';
        refs.fileInput.click();
    });

    if (refs.fileInput) {
        refs.fileInput.addEventListener('change', evt => {
            const file = evt.target.files?.[0];
            if (!file) return;
            if (!state.currentRoomId) {
                alert('请先选择房间');
                return;
            }
            if (file.size > MAX_FILE_SIZE) {
                alert('文件过大，请选择 2MB 以内的文件');
                return;
            }
            const reader = new FileReader();
            reader.onload = () => {
                send('FILE_MESSAGE', {
                    roomId: state.currentRoomId,
                    fileUrl: reader.result,
                    fileName: file.name,
                    mimeType: file.type,
                    fileSize: file.size
                });
            };
            reader.onerror = () => alert('文件读取失败，请重试');
            reader.readAsDataURL(file);
        });
    } else {
        console.warn('file input not found, file upload disabled');
    }

    refs.emojiBtn.addEventListener('click', evt => {
        evt.stopPropagation();
        const rect = refs.emojiBtn.getBoundingClientRect();
        emojiMenu.style.top = `${rect.bottom + 8}px`;
        emojiMenu.style.left = `${rect.left}px`;
        emojiMenu.classList.toggle('hidden');
    });

    emojiMenu.addEventListener('click', evt => {
        const emoji = evt.target.dataset.emoji;
        if (!emoji) return;
        refs.messageInput.value += emoji;
        emojiMenu.classList.add('hidden');
        refs.messageInput.focus();
    });

    contextMenu.addEventListener('click', evt => {
        const btn = evt.target.closest('button');
        if (!btn) return;
        const action = btn.dataset.action;
        const target = contextMenuMessage;
        hideContextMenu();
        if (!target) return;
        if (action === 'copy') {
            copyMessageContent(target);
        } else if (action === 'quote') {
            setQuoteFromMessage(target);
        } else if (action === 'select') {
            enterSelectMode(target.id);
        } else if (action === 'recall') {
            recallMessage(target);
        }
    });

    multiSelectBar.addEventListener('click', evt => {
        const btn = evt.target.closest('button');
        if (!btn) return;
        const action = btn.dataset.action;
        if (action === 'forward-each') {
            forwardSelectedIndividually();
        } else if (action === 'forward-combine') {
            forwardSelectedCombined();
        } else if (action === 'choose-targets') {
            openForwardTargetModal();
        } else if (action === 'cancel-select') {
            exitSelectMode();
        }
    });

    const forwardTargetModalEl = document.getElementById('forward-target-modal');
    forwardTargetModalEl?.addEventListener('click', evt => {
        const modal = forwardTargetModalEl;
        const actionBtn = evt.target.closest('button');
        if (actionBtn) {
            const action = actionBtn.dataset.action;
            if (action === 'cancel') {
                closeForwardTargetModal();
            } else if (action === 'confirm') {
                closeForwardTargetModal();
            }
            return;
        }
        if (evt.target === modal) {
            closeForwardTargetModal();
        }
    });

    const forwardDetailModalEl = document.getElementById('forward-detail-modal');
    forwardDetailModalEl?.addEventListener('click', evt => {
        const actionBtn = evt.target.closest('button');
        if (actionBtn && actionBtn.dataset.action === 'close-forward-detail') {
            closeForwardDetail();
            return;
        }
        if (evt.target === forwardDetailModalEl) {
            closeForwardDetail();
        }
    });

    document.addEventListener('click', evt => {
        if (!emojiMenu.contains(evt.target) && !refs.emojiBtn.contains(evt.target)) {
            emojiMenu.classList.add('hidden');
        }
        if (!refs.gameMenu.contains(evt.target) && !refs.gameBtn.contains(evt.target)) {
            refs.gameMenu.classList.add('hidden');
        }
        if (refs.settingsMenu && !refs.settingsMenu.contains(evt.target) && !refs.settingsBtn.contains(evt.target)) {
            toggleSettingsMenu(false);
        }
        if (!contextMenu.contains(evt.target)) {
            hideContextMenu();
        }
    });

    refs.messageList?.addEventListener('scroll', hideContextMenu);
    window.addEventListener('resize', hideContextMenu);

    refs.openRoomModalBtn.addEventListener('click', () => toggleRoomModal(true));
    refs.closeRoomModalBtn.addEventListener('click', () => toggleRoomModal(false));
    refs.createRoomConfirm.addEventListener('click', () => {
        const name = refs.roomNameInput.value.trim();
        if (!name) {
            alert('请输入房间名称');
            return;
        }
        const memberUserIds = Array.from(refs.roomMembersBox.querySelectorAll('input[type="checkbox"]:checked')).map(
            cb => cb.value
        );
        send('ROOM_CREATE', { name, memberUserIds });
    });

    refs.editProfileBtn.addEventListener('click', () => toggleProfileModal(true));
    refs.profileCancelBtn.addEventListener('click', () => toggleProfileModal(false));
    refs.profileSaveBtn.addEventListener('click', () => {
        send('PROFILE_UPDATE', {
            displayName: refs.profileDisplayName.value.trim(),
            avatarUrl: refs.profileAvatar.value.trim(),
            signature: refs.profileSignature.value.trim()
        });
    });

    refs.viewProfileClose.addEventListener('click', () => toggleViewProfileModal(false));

    [refs.currentAvatar, refs.currentName, refs.currentSignature].forEach(el => {
        if (!el) return;
        el.style.cursor = 'pointer';
        el.addEventListener('click', () => {
            if (!state.currentUser || !state.currentUser.id) return;
            send('PROFILE_VIEW', { userId: state.currentUser.id });
        });
    });

    refs.gameBtn.addEventListener('click', evt => {
        evt.stopPropagation();
        if (!state.currentRoomId) {
            alert('请先选择房间');
            return;
        }
        refs.gameMenu.classList.toggle('hidden');
    });

    refs.gameMenu.addEventListener('click', evt => {
        const item = evt.target.closest('.dropdown-item');
        if (!item) return;
        evt.stopPropagation();
        if (!state.currentRoomId) {
            alert('请先选择房间');
            return;
        }
        const gameType = item.dataset.game;
        if (gameType === 'TOMATO') {
            sendTomatoAction();
        } else {
            send('GAME_REQUEST', { roomId: state.currentRoomId, gameType });
        }
        refs.gameMenu.classList.add('hidden');
    });

    if (refs.settingsBtn) {
        refs.settingsBtn.addEventListener('click', evt => {
            evt.stopPropagation();
            toggleSettingsMenu();
        });
    }

    if (refs.logoutBtn) {
        refs.logoutBtn.addEventListener('click', () => {
            try {
                state.socket && state.socket.close();
            } catch (e) {
                console.warn('socket close failed', e);
            }
            window.location.reload();
        });
    }

    refs.viewProfileMoments?.addEventListener('click', () => {
        if (!state.viewProfileUser || !state.viewProfileUser.id) return;
        toggleViewProfileModal(false);
        openMomentsForUser(state.viewProfileUser.id);
    });

    refs.viewProfileMoments?.addEventListener('click', () => {
        if (!state.viewProfileUser || !state.viewProfileUser.id) return;
        toggleViewProfileModal(false);
        openMomentsForUser(state.viewProfileUser.id);
    });


    // ADD: 群管理相关事件绑定
    refs.manageRoomBtn?.addEventListener('click', () => toggleManageRoomModal(true));
    refs.manageRoomCancel?.addEventListener('click', () => toggleManageRoomModal(false));
    
    refs.manageRoomSave?.addEventListener('click', () => {
        const room = state.currentRoomId ? state.rooms[state.currentRoomId] : null;
        if (!room) return;
        const payload = {
            roomId: room.id,
            name: refs.manageRoomName?.value.trim(),
            description: refs.manageRoomDesc?.value.trim(),
            avatarUrl: refs.manageRoomAvatar?.value.trim(),
            allowPublicJoin: refs.manageRoomPublic?.checked
        };
        send('ROOM_UPDATE', payload);
    });
    
    refs.manageRoomAddBtn?.addEventListener('click', () => {
        const room = state.currentRoomId ? state.rooms[state.currentRoomId] : null;
        if (!room) return;
        const ids = Array.from(refs.manageRoomAddSelect?.selectedOptions || [])
            .map(opt => opt.value)
            .filter(Boolean);
        if (!ids.length) return;
        send('ROOM_INVITE', { roomId: room.id, memberUserIds: ids });
        if (refs.manageRoomAddSelect) {
            refs.manageRoomAddSelect.selectedIndex = -1;
        }
    });



    //add:
    // ========== 保留你的参数 + 新增缓缓扭动+烟花+文字效果 ==========
    if (document.getElementById('jelly-canvas')) {
        const canvas = document.getElementById('jelly-canvas');
        const ctx = canvas.getContext('2d');
        let width, height;
        let mouseX = canvas.clientWidth / 2; 
        let mouseY = canvas.clientHeight / 2;

        // 保留你的布局/大小，新增扭动幅度参数（匹配你的轻柔风格）
        const jellyBlobs = [
            // 紫色竖长条形（新增twistAmp：扭动幅度）
            { x: 0, y: 0, width: 120, height: 320, color: '#b806c9ff', shape: 'rect', speed: 0.01, twistAmp: 2 },
            // 蓝色三角形
            { x: 0, y: 0, size: 175, color: '#1e2dffff', shape: 'triangle', speed: 0.01, twistAmp: 1.5 },
            // 绿色圆形
            { x: 0, y: 0, radius: 80, color: '#057e03ff', shape: 'circle', speed: 0.01, twistAmp: 1 },
        ];

        // ========== 新增：烟花粒子类（放到canvas作用域内） ==========
        class FireworkParticle {
            constructor(x, y, color) {
                this.x = x;
                this.y = y;
                this.color = color;
                this.radius = Math.random() * 2 + 1;
                this.speed = Math.random() * 4 + 1;
                this.angle = Math.random() * Math.PI * 2;
                this.vx = Math.cos(this.angle) * this.speed;
                this.vy = Math.sin(this.angle) * this.speed;
                this.gravity = 0.05;
                this.alpha = 1;
                this.fade = Math.random() * 0.03 + 0.01;
            }

            update() {
                this.vy += this.gravity;
                this.x += this.vx;
                this.y += this.vy;
                this.alpha -= this.fade;
            }

            draw() { // 移除ctx参数，直接使用外层作用域的ctx
                ctx.save();
                ctx.globalAlpha = this.alpha;
                ctx.fillStyle = this.color;
                ctx.beginPath();
                ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
                ctx.fill();
                ctx.restore();
            }
        }

        // ========== 新增：文字粒子类（随机蹦出指定文字） ==========
        class TextParticle {
            constructor(x, y, color) {
                // 随机选择文字
                this.texts = ['happy', 'ac', 'rich', 'lucky', 'joy', 'fun', 'smile', 'star', 'shine', 'sweet', 'cool', 'chill', 'glow', 'dream', 'peace', 'love'];
                this.text = this.texts[Math.floor(Math.random() * this.texts.length)];
                // 初始位置（烟花位置±30px随机偏移，模拟蹦出效果）
                this.x = x + (Math.random() - 0.5) * 60;
                this.y = y + (Math.random() - 0.5) * 60;
                // 文字颜色（同果冻色系，轻微明暗变化）
                this.color = this.getSimilarColor(color);
                // 动画参数
                this.alpha = 1;
                this.fade = Math.random() * 0.02 + 0.01; // 比烟花慢一点消失
                this.speedY = (Math.random() - 0.5) * 2 - 1; // 轻微上下蹦动
                this.speedX = (Math.random() - 0.5) * 2; // 轻微左右移动
                this.scale = Math.random() * 0.8 + 0.6; // 文字大小随机
                this.rotate = (Math.random() - 0.5) * 0.5; // 轻微旋转
            }

            // 生成同色系文字颜色
            getSimilarColor(baseColor) {
                const r = parseInt(baseColor.slice(1, 3), 16);
                const g = parseInt(baseColor.slice(3, 5), 16);
                const b = parseInt(baseColor.slice(5, 7), 16);
                const adjust = Math.floor(Math.random() * 40) - 20;
                const newR = Math.max(0, Math.min(255, r + adjust));
                const newG = Math.max(0, Math.min(255, g + adjust));
                const newB = Math.max(0, Math.min(255, b + adjust));
                return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`;
            }

            update() {
                // 文字移动+上浮效果
                this.y += this.speedY;
                this.x += this.speedX;
                this.speedY += 0.02; // 轻微重力
                this.alpha -= this.fade;
                this.scale += 0.005; // 轻微放大
            }

            draw() {
                ctx.save();
                ctx.globalAlpha = this.alpha;
                ctx.fillStyle = this.color;
                ctx.font = `${this.scale * 24}px Arial Bold`; // 粗体文字
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.translate(this.x, this.y);
                ctx.rotate(this.rotate);
                // 绘制文字（带轻微描边更醒目）
                ctx.strokeStyle = 'white';
                ctx.lineWidth = 1;
                ctx.strokeText(this.text, 0, 0);
                ctx.fillText(this.text, 0, 0);
                ctx.restore();
            }
        }

        // 存储所有烟花粒子和文字粒子（放到canvas作用域内）
        const fireworks = [];
        const textParticles = [];

        // 检测点是否在圆形内（放到canvas作用域内）
        function isPointInCircle(x, y, circleX, circleY, radius) {
            const dx = x - circleX;
            const dy = y - circleY;
            return dx * dx + dy * dy <= radius * radius;
        }

        // 检测点是否在矩形内（放到canvas作用域内）
        function isPointInRect(x, y, rectX, rectY, width, height) {
            const halfW = width / 2;
            const halfH = height / 2;
            return x >= rectX - halfW && x <= rectX + halfW &&
                y >= rectY - halfH && y <= rectY + halfH;
        }

        // 检测点是否在三角形内（放到canvas作用域内）
        function isPointInTriangle(x, y, triangle) {
            const { x: x1, y: y1 } = triangle.p1;
            const { x: x2, y: y2 } = triangle.p2;
            const { x: x3, y: y3 } = triangle.p3;

            const d1 = (x - x2) * (y1 - y2) - (x1 - x2) * (y - y2);
            const d2 = (x - x3) * (y2 - y3) - (x2 - x3) * (y - y3);
            const d3 = (x - x1) * (y3 - y1) - (x3 - x1) * (y - y1);

            const hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
            const hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

            return !(hasNeg && hasPos);
        }

        // 初始化点击事件（放到canvas作用域内）
        function initJellyClick() {
            if (!canvas) return;

            canvas.addEventListener('click', (e) => {
                const rect = canvas.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;

                // 检查点击是否在某个果冻上
                jellyBlobs.forEach(blob => {
                    let isClicked = false;

                    if (blob.shape === 'circle') {
                        isClicked = isPointInCircle(x, y, blob.x, blob.y, blob.radius);
                    } else if (blob.shape === 'rect') {
                        isClicked = isPointInRect(x, y, blob.x, blob.y, blob.width, blob.height);
                    } else if (blob.shape === 'triangle') {
                        // 计算三角形三个顶点（匹配你的参数）
                        const triangle = {
                            p1: { x: blob.x, y: blob.y - blob.size * 0.7 },
                            p2: { x: blob.x - blob.size * 0.6, y: blob.y + blob.size * 0.4 },
                            p3: { x: blob.x + blob.size * 0.6, y: blob.y + blob.size * 0.4 }
                        };
                        isClicked = isPointInTriangle(x, y, triangle);
                    }

                    // 如果点击了果冻，创建同色系烟花+随机文字
                    if (isClicked) {
                        const fireworkX = x;
                        const fireworkY = y - 200; // 烟花在点击位置上方200px
                        // 创建烟花粒子
                        createFirework(fireworkX, fireworkY, blob.color);
                        // 创建2-4个随机文字粒子（模拟蹦出效果）
                        const textCount = Math.floor(Math.random() * 1) + 1.2;
                        for (let i = 0; i < textCount; i++) {
                            textParticles.push(new TextParticle(fireworkX, fireworkY, blob.color));
                        }
                    }
                });
            });
        }

        // 创建烟花（放到canvas作用域内）
        function createFirework(x, y, baseColor) {
            // 生成同色系的颜色变体
            const colors = generateSimilarColors(baseColor, 8);

            // 创建烟花粒子
            for (let i = 0; i < 100; i++) {
                const color = colors[Math.floor(Math.random() * colors.length)];
                fireworks.push(new FireworkParticle(x, y, color));
            }
        }

        // 生成同色系颜色（放到canvas作用域内）
        function generateSimilarColors(baseColor, count) {
            // 简单实现：从基础颜色生成明暗变体
            const colors = [];
            for (let i = 0; i < count; i++) {
                // 完全保留你的逻辑，不修改
                colors.push(baseColor);
            }
            return colors;
        }

        // 保留你的位置布局
        function resizeCanvas() {
            width = canvas.clientWidth;
            height = canvas.clientHeight;
            canvas.width = width;
            canvas.height = height;

            jellyBlobs[0].x = width * 0.25;
            jellyBlobs[0].y = height * 0.55;

            jellyBlobs[1].x = width * 0.72;
            jellyBlobs[1].y = height * 0.62;

            jellyBlobs[2].x = width * 0.5;
            jellyBlobs[2].y = height * 0.7;
        }
        resizeCanvas();
        window.addEventListener('resize', resizeCanvas);

        // 保留你的鼠标跟随
        document.addEventListener('mousemove', (e) => {
            const rect = canvas.getBoundingClientRect();
            mouseX = e.clientX - rect.left;
            mouseY = e.clientY - rect.top;
        });

        // 新增：计算扭动偏移（基于时间的周期性变化）
        function updateTwist(blob) {
            blob.twistOffset = Math.sin(Date.now() * blob.speed) * blob.twistAmp;
        }

        // 保留你的轻微抖动 + 新增缓缓扭动
        function updateJelly(blob) {
            updateTwist(blob); // 先算扭动偏移
            // 保留你的轻柔平移抖动参数
            if (blob.shape === 'circle') {
                blob.x += Math.sin(Date.now() * blob.speed) * 0.1;
                blob.y += Math.cos(Date.now() * blob.speed) * 0.2;
            } else if (blob.shape === 'rect') {
                blob.y += Math.sin(Date.now() * blob.speed * 1.2) * 0.1;
            } else if (blob.shape === 'triangle') {
                blob.x += Math.cos(Date.now() * blob.speed * 1.5) * 0.1;
            }
        }

function initSloganContainer() {
  // 确保容器只被创建一次
  if (document.querySelector('.jelly-slogan-container')) return;
  
  const canvasContainer = document.querySelector('.post-login-welcome');
  if (canvasContainer) {
    const sloganContainer = document.createElement('div');
    sloganContainer.className = 'jelly-slogan-container';
    sloganContainer.innerHTML = `
      <div class="jelly-slogan">
        <div>have a nice day</div>
        <div>link now,explore more</div>
        <div>nice to meet you</div>
      </div>
    `;
    canvasContainer.appendChild(sloganContainer);
  }
}

// 在页面加载时初始化
initSloganContainer();

        function draw() {
            requestAnimationFrame(draw);
            ctx.clearRect(0, 0, width, height);

            // ========== 新增：绘制并更新烟花 ==========
            for (let i = fireworks.length - 1; i >= 0; i--) {
                fireworks[i].update();
                fireworks[i].draw(); // 不再传ctx，使用外层作用域的ctx

                // 移除完全透明的粒子
                if (fireworks[i].alpha <= 0) {
                    fireworks.splice(i, 1);
                }
            }

            // ========== 新增：绘制并更新文字粒子 ==========
            for (let i = textParticles.length - 1; i >= 0; i--) {
                textParticles[i].update();
                textParticles[i].draw();

                // 移除完全透明的文字
                if (textParticles[i].alpha <= 0) {
                    textParticles.splice(i, 1);
                }
            }

            // ========== 保留你的果冻绘制逻辑（完全不修改参数） ==========
            jellyBlobs.forEach(blob => {
                updateJelly(blob);
                ctx.save();
                ctx.fillStyle = blob.color;

                // 1. 紫色竖长条形：四个角圆润 + 缓缓扭动（保留你的参数）
                if (blob.shape === 'rect') {
                    const cornerRadius = 25; // 保留你的圆角参数
                    const halfW = blob.width / 2;
                    const halfH = blob.height / 2;

                    // 计算四个角的基础坐标（结合扭动偏移，保持顶部/底部水平）
                    const topLeftX = blob.x - halfW;
                    const topLeftY = blob.y - halfH;
                    const topRightX = blob.x + halfW;
                    const topRightY = blob.y - halfH;
                    const bottomRightX = blob.x + halfW;
                    const bottomRightY = blob.y + halfH;
                    const bottomLeftX = blob.x - halfW;
                    const bottomLeftY = blob.y + halfH;

                    // 扭动偏移（只影响左右边缘弯曲，不破坏圆角）
                    const rightTwist = blob.twistOffset;
                    const leftTwist = -blob.twistOffset;

                    ctx.beginPath();
                    // ========== 绘制带圆角的路径（保留你的逻辑） ==========
                    // 1. 顶部左圆角
                    ctx.moveTo(topLeftX + cornerRadius, topLeftY);
                    ctx.quadraticCurveTo(topLeftX, topLeftY, topLeftX, topLeftY + cornerRadius);

                    // 2. 左侧边缘（带扭动弯曲）
                    ctx.quadraticCurveTo(
                        topLeftX + leftTwist, blob.y, // 左侧扭动控制点
                        bottomLeftX, bottomLeftY - cornerRadius
                    );

                    // 3. 底部左圆角
                    ctx.quadraticCurveTo(bottomLeftX, bottomLeftY, bottomLeftX + cornerRadius, bottomLeftY);

                    // 4. 底部水平直线
                    ctx.lineTo(bottomRightX - cornerRadius, bottomLeftY);

                    // 5. 底部右圆角
                    ctx.quadraticCurveTo(bottomRightX, bottomRightY, bottomRightX, bottomRightY - cornerRadius);

                    // 6. 右侧边缘（带扭动弯曲）
                    ctx.quadraticCurveTo(
                        topRightX + rightTwist, blob.y, // 右侧扭动控制点
                        topRightX, topRightY + cornerRadius
                    );

                    // 7. 顶部右圆角
                    ctx.quadraticCurveTo(topRightX, topRightY, topRightX - cornerRadius, topRightY);

                    // 8. 顶部水平直线（闭合路径）
                    ctx.lineTo(topLeftX + cornerRadius, topLeftY);
                    // ======================================

                    ctx.fillStyle = blob.color;
                    ctx.fill();
                }

                // 2. 绿色圆形：轻微鼓包扭动（保留你的参数）
                else if (blob.shape === 'circle') {
                    ctx.translate(blob.x, blob.y);
                    // 轻微拉伸+旋转，模拟果冻鼓包（保留你的参数）
                    const scaleX = 1 + Math.sin(Date.now() * blob.speed * 1.2) * 0.02;
                    const scaleY = 1 + Math.cos(Date.now() * blob.speed * 1.2) * 0.02;
                    ctx.scale(scaleX, scaleY);
                    ctx.rotate(blob.twistOffset * 0.05);
                    ctx.beginPath();
                    ctx.arc(0, 0, blob.radius, 0, Math.PI * 2);
                    ctx.fill();
                    ctx.restore(); // 提前恢复，避免影响眼睛
                    ctx.save(); // 重新保存状态
                }

                // 3. 蓝色三角形：顶点扭动（保留你的参数）
                else if (blob.shape === 'triangle') {
                    ctx.beginPath();
                    // 三角形三个顶点：加入扭动偏移（保留你的参数）
                    const topX = blob.x + blob.twistOffset;
                    const topY = blob.y - blob.size * 0.7 + blob.twistOffset / 2;
                    const leftX = blob.x - blob.size * 0.6 - blob.twistOffset / 3;
                    const leftY = blob.y + blob.size * 0.4;
                    const rightX = blob.x + blob.size * 0.6 + blob.twistOffset / 3;
                    const rightY = blob.y + blob.size * 0.4;

                    ctx.moveTo(topX, topY);
                    ctx.lineTo(leftX, leftY);
                    ctx.lineTo(rightX, rightY);
                    ctx.closePath();
                    ctx.fill();
                }

                // 保留你的眼睛跟随逻辑（完全不修改参数）
                const eyeOffset = blob.shape === 'circle' ? blob.radius * 0.35 : 15;
                const angle = Math.atan2(mouseY - blob.y, mouseX - blob.x);
                const leftEyeX = blob.x + eyeOffset * Math.cos(angle - 0.3);
                const leftEyeY = blob.y + eyeOffset * Math.sin(angle - 0.3);
                const rightEyeX = blob.x + eyeOffset * Math.cos(angle + 0.3);
                const rightEyeY = blob.y + eyeOffset * Math.sin(angle + 0.3);
                ctx.fillStyle = 'white';
                ctx.beginPath();
                ctx.arc(leftEyeX, leftEyeY, 12, 0, Math.PI * 2);
                ctx.arc(rightEyeX, rightEyeY, 12, 0, Math.PI * 2);
                ctx.fill();
                ctx.fillStyle = 'black';
                ctx.beginPath();
                ctx.arc(leftEyeX, leftEyeY, 5, 0, Math.PI * 2);
                ctx.arc(rightEyeX, rightEyeY, 5, 0, Math.PI * 2);
                ctx.fill();

                ctx.restore();
            });
        }
        initJellyClick();
        draw();
    }

    // 调试助手：在控制台可调用 window.dumpBurn() 查看当前房间的阅后即焚字段
    window.dumpBurn = () => {
        const roomId = state.currentRoomId;
        const msgs = roomId ? state.messagesByRoom[roomId] || [] : [];
        const mapped = msgs.map(m => ({
            id: m.id,
            rawType: m.messageType,
            normalizedType: getMessageType(m),
            burnDelay: m.burnDelay,
            timestamp: m.timestamp,
            expireAt: getBurnExpireAt(m),
            isBurnMessage: isBurnMessage(m)
        }));
        console.log('[burn-dump]', { roomId, messages: mapped, lastBurnDebug: state.lastBurnDebug });
        return { roomId, messages: mapped, lastBurnDebug: state.lastBurnDebug };
    };

    document.querySelectorAll('[data-collapse-target]').forEach(btn => {
        btn.addEventListener('click', () => {
            const target = btn.getAttribute('data-collapse-target');
            const body = document.querySelector('[data-collapse-body="' + target + '"]');
            if (!body) return;
            const collapsed = body.classList.toggle('collapsed');
            btn.setAttribute('aria-expanded', (!collapsed).toString());
        });
    });
    document.getElementById('close-chat-btn').addEventListener('click', closeChat);

    function initImagePreview() {
        let currentImageList = []; // 当前预览的图片组
        let currentIndex = 0;      // 当前图片索引

        // 获取核心DOM
        const modal = document.getElementById('image-preview-modal');
        const previewImg = document.getElementById('preview-image');
        const closeBtn = document.getElementById('close-preview');

        // ===== 新增：创建左右箭头DOM =====
        const leftArrow = document.createElement('div');
        leftArrow.className = 'preview-arrow preview-arrow-left';
        leftArrow.textContent = '←';
        const rightArrow = document.createElement('div');
        rightArrow.className = 'preview-arrow preview-arrow-right';
        rightArrow.textContent = '→';
        modal.appendChild(leftArrow);
        modal.appendChild(rightArrow);

        // ===== 新增：索引提示DOM =====
        const indexTip = document.createElement('div');
        indexTip.className = 'preview-index-tip';
        modal.appendChild(indexTip);

        // 切换图片的通用函数（抽离复用）
        function switchImage(newIndex) {
            if (currentImageList.length === 0) return;
            currentIndex = newIndex;
            previewImg.src = currentImageList[currentIndex];
            indexTip.textContent = `${currentIndex + 1}/${currentImageList.length}`;
            // 单张图片时隐藏箭头
            leftArrow.style.display = currentImageList.length > 1 ? 'flex' : 'none';
            rightArrow.style.display = currentImageList.length > 1 ? 'flex' : 'none';
        }

        // 1. 点击图片触发预览
        document.addEventListener('click', (e) => {
            const targetImg = e.target.closest('.moment-images img, .moment-preview-img');
            if (targetImg) {
            // 收集当前图片组
            const imgParent = targetImg.parentElement;
            const allImgs = Array.from(imgParent.querySelectorAll('img'));
            currentImageList = allImgs.map(img => img.dataset.originalUrl || img.src);
            currentIndex = allImgs.findIndex(img => img === targetImg);

            // 显示预览+切换到当前图片+更新箭头/索引
            modal.style.display = 'flex';
            switchImage(currentIndex);
            }
        });

        // 2. 关闭预览
        function closePreview() {
            modal.style.display = 'none';
            previewImg.src = '';
            currentImageList = [];
            currentIndex = 0;
            indexTip.textContent = '';
        }
        closeBtn.addEventListener('click', closePreview);
        modal.addEventListener('click', (e) => {
            // 点击遮罩层空白处才关闭（排除图片、箭头、索引提示）
            if (e.target === modal) closePreview();
        });

        // ===== 新增：鼠标点击箭头切换 =====
        leftArrow.addEventListener('click', () => {
            // 上一张（循环）
            const newIndex = (currentIndex - 1 + currentImageList.length) % currentImageList.length;
            switchImage(newIndex);
        });
        rightArrow.addEventListener('click', () => {
            // 下一张（循环）
            const newIndex = (currentIndex + 1) % currentImageList.length;
            switchImage(newIndex);
        });

        // ===== 新增：点击预览层左右空白区域切换（可选，增强体验） =====
        modal.addEventListener('click', (e) => {
            if (currentImageList.length <= 1) return;
            const modalRect = modal.getBoundingClientRect();
            const clickX = e.clientX - modalRect.left;
            const modalWidth = modalRect.width;

            // 点击左侧1/3区域 → 上一张
            if (clickX < modalWidth / 3 && e.target === modal) {
            const newIndex = (currentIndex - 1 + currentImageList.length) % currentImageList.length;
            switchImage(newIndex);
            }
            // 点击右侧1/3区域 → 下一张
            else if (clickX > modalWidth * 2 / 3 && e.target === modal) {
            const newIndex = (currentIndex + 1) % currentImageList.length;
            switchImage(newIndex);
            }
        });

        // 3. 键盘方向键切换（保留原有）
        document.addEventListener('keydown', (e) => {
            if (modal.style.display !== 'flex' || currentImageList.length <= 1) return;

            switch(e.key) {
            case 'ArrowLeft':
                switchImage((currentIndex - 1 + currentImageList.length) % currentImageList.length);
                e.preventDefault();
                break;
            case 'ArrowRight':
                switchImage((currentIndex + 1) % currentImageList.length);
                e.preventDefault();
                break;
            case 'Escape':
                closePreview();
                break;
            }
        });
        }

        // 页面加载初始化
        window.addEventListener('DOMContentLoaded', initImagePreview);

    // 页面加载后初始化
    window.addEventListener('DOMContentLoaded', initImagePreview);



    connect();
    
})();
