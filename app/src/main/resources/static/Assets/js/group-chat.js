/* ============================================================
   群聊模块（桌面版 + H5 共用）
   - 群列表 / 创建群聊（每人最多5个）/ 模糊搜索申请加入
   - 管理员审批入群、踢出成员
   - 聊天：文字、表情（emoji 面板）、图片
   - 消息记录存浏览器 localStorage（wxyd_group_msg_{群ID}）
   - 服务器只做中转：进入群聊后 2 秒轮询 group/pull，拉走即删
   依赖 jQuery；挂载：GroupChat.mount(containerElement)
   ============================================================ */
(function () {

    var GC_MSG_PREFIX = "wxyd_group_msg_";
    var POLL_INTERVAL = 2000;

    var state = {
        myId: null,
        groups: [],
        chat: null,      // {groupId, name, amIOwner}
        members: [],
        timer: null,
        seq: 0,          // 本地乐观消息序号
        reads: {},       // groupId -> { msgId: {readCount, total} }（自己消息的已读计数）
        readsSig: {}     // groupId -> 上次已读数据签名（无变化不重绘）
    };

    /* ---------------- 工具 ---------------- */

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function gcMsg(msg) {
        if (typeof window.toast === 'function') window.toast(msg);
        else if (typeof window.alterModal === 'function') window.alterModal(msg);
        else alert(msg);
    }

    function gcConfirm(msg, onOk) {
        if (typeof window.confirmModal === 'function') window.confirmModal(msg, onOk);
        else if (typeof window.confirm === 'function' && window.confirm.length === 2) window.confirm(msg, onOk);
        else if (window.confirm(msg)) onOk();
    }

    function ajaxHeaders() {
        return { 'X-Requested-With': 'XMLHttpRequest' };
    }

    function gcGet(url, ok, fail) {
        $.ajax({ url: url, type: 'GET', dataType: 'json', headers: ajaxHeaders() })
            .done(function (res) { handle(res, ok, fail); })
            .fail(function (jqXHR) {
                if (jqXHR.status === 401) stopPoll(); // 会话过期，停止轮询（由页面全局 401 处理跳转/弹登录）
                else if (fail) fail();
            });
    }

    function gcPost(url, body, ok, fail) {
        $.ajax({
            url: url, type: 'POST', dataType: 'json', headers: ajaxHeaders(),
            contentType: 'application/json', data: JSON.stringify(body == null ? {} : { body: body })
        })
            .done(function (res) { handle(res, ok, fail); })
            .fail(function (jqXHR) {
                if (jqXHR.status === 401) stopPoll();
                else if (fail) fail();
            });
    }

    function handle(res, ok, fail) {
        if (res && res.errorCode === '000000') { if (ok) ok(res.body); }
        else if (fail) fail((res && res.errorMsg) || '操作失败');
    }

    function nowStr() {
        var d = new Date();
        function p(i) { return String(i).padStart(2, '0'); }
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
    }

    function resolveImgUrl(rel) {
        // rel 形如 group/img/get?name=xxx；页面位于 /api/ 下，相对解析即可
        return rel;
    }

    /* ---------------- 消息缓存（localStorage，按群分 key） ---------------- */

    function loadCache(groupId) {
        try { return JSON.parse(localStorage.getItem(GC_MSG_PREFIX + groupId)) || []; }
        catch (e) { return []; }
    }

    function saveCache(groupId, list) {
        try {
            localStorage.setItem(GC_MSG_PREFIX + groupId, JSON.stringify(list));
        } catch (e) {
            // 容量超限：丢弃该群最旧的一半后重试一次
            var halved = list.slice(Math.floor(list.length / 2));
            try {
                localStorage.setItem(GC_MSG_PREFIX + groupId, JSON.stringify(halved));
                gcMsg('本地缓存已满，仅保留最近 ' + halved.length + ' 条消息');
            } catch (e2) { /* 忽略 */ }
        }
    }

    function appendMsg(groupId, msg) {
        var list = loadCache(groupId);
        list.push(msg);
        saveCache(groupId, list);
        return list;
    }

    function updateMsg(groupId, lid, patch) {
        var list = loadCache(groupId);
        for (var i = 0; i < list.length; i++) {
            if (list[i].lid === lid) { $.extend(list[i], patch); break; }
        }
        saveCache(groupId, list);
        return list;
    }

    /* ---------------- 表情面板（Unicode emoji） ---------------- */

    var EMOJIS = ("😀 😃 😄 😁 😆 😅 🤣 😂 🙂 😉 😊 😍 🥰 😘 😜 🤪 🤗 🤔 🤨 😐 😑 😶 🙄 😏 😣 😥 😮 😯 😴 😌 😔 😕 🙃 😲 ☹️ 🙁 😖 😞 😟 😤 😢 😭 😦 😧 😨 😩 🤯 😬 😰 😱 🥵 😳 🤢 🤮 🤫 🥺 😎 🤓 🧐 👍 👎 👌 ✌️ 🤞 🤟 🤘 👏 🙌 🤝 💪 🙏 ❤️ 🧡 💛 💚 💙 💜 💔 💯 💥 🔥 ✨ 🎉 🎁 🌹 🌞 🌙 ⚡ ☔ 🍀 🚀 🎵 🍺 ☕ 🐶 🐱 🦊").split(" ");

    function emojiHtml() {
        var h = '';
        for (var i = 0; i < EMOJIS.length; i++) {
            h += '<span class="gc-emoji-item" data-e="' + EMOJIS[i] + '">' + EMOJIS[i] + '</span>';
        }
        return h;
    }

    /* ---------------- 挂载 ---------------- */

    function mount(container) {
        var $c = $(container);
        if (!$c.length) return;
        if (!$c.data('gcMounted')) {
            $c.data('gcMounted', true);
            $c.html(buildDom());
            bindEvents($c);
        }
        // 每次进入页面刷新我的身份与群列表
        gcGet('user/info', function (info) {
            state.myId = info && info.id != null ? Number(info.id) : null;
            loadGroups($c);
        }, function () { loadGroups($c); });
    }

    function buildDom() {
        return '' +
            '<div class="gc-toolbar">' +
            '  <button class="gc-btn gc-btn-primary" data-gc="create">＋ 创建群聊</button>' +
            '  <button class="gc-btn" data-gc="applies">我的审批<span class="gc-badge" data-gc-ref="applyBadge" style="display:none">0</span></button>' +
            '  <input class="gc-input" data-gc-ref="searchInput" placeholder="搜索群聊名称…" maxlength="20">' +
            '  <button class="gc-btn" data-gc="search">搜索</button>' +
            '</div>' +
            '<div data-gc-ref="listView">' +
            '  <div class="gc-section-title">我的群聊</div>' +
            '  <div data-gc-ref="groupList"><div class="gc-empty">加载中…</div></div>' +
            '  <div data-gc-ref="searchWrap" style="display:none">' +
            '    <div class="gc-section-title">搜索结果</div>' +
            '    <div data-gc-ref="searchResult"></div>' +
            '  </div>' +
            '</div>' +
            '<div data-gc-ref="chatView" style="display:none">' +
            '  <div class="gc-chat-head">' +
            '    <button class="gc-btn gc-btn-back" data-gc="back">←</button>' +
            '    <span class="gc-chat-name" data-gc-ref="chatName"></span>' +
            '    <button class="gc-btn gc-btn-sm" data-gc="members">成员</button>' +
            '  </div>' +
            '  <div class="gc-msgs" data-gc-ref="msgList"></div>' +
            '  <div class="gc-emoji-panel" data-gc-ref="emojiPanel" style="display:none">' + emojiHtml() + '</div>' +
            '  <div class="gc-inputbar">' +
            '    <button class="gc-icon-btn" data-gc="emoji" title="表情">😊</button>' +
            '    <button class="gc-icon-btn" data-gc="img" title="图片">🖼</button>' +
            '    <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" data-gc-ref="imgInput" style="display:none">' +
            '    <input class="gc-input gc-text-input" data-gc-ref="textInput" placeholder="输入消息…" maxlength="500">' +
            '    <button class="gc-btn gc-btn-primary" data-gc="send">发送</button>' +
            '  </div>' +
            '</div>' +
            /* 创建群聊弹窗 */
            '<div class="gc-mask" data-gc-ref="createMask" style="display:none">' +
            '  <div class="gc-modal">' +
            '    <div class="gc-modal-title">创建群聊</div>' +
            '    <input class="gc-input" data-gc-ref="groupNameInput" placeholder="群聊名称（20字以内）" maxlength="20">' +
            '    <div class="gc-modal-actions">' +
            '      <button class="gc-btn" data-gc="createCancel">取消</button>' +
            '      <button class="gc-btn gc-btn-primary" data-gc="createOk">创建</button>' +
            '    </div>' +
            '  </div>' +
            '</div>' +
            /* 我的审批弹窗 */
            '<div class="gc-mask" data-gc-ref="appliesMask" style="display:none">' +
            '  <div class="gc-modal gc-modal-lg">' +
            '    <div class="gc-modal-title">入群审批</div>' +
            '    <div data-gc-ref="appliesList" class="gc-modal-body"><div class="gc-empty">加载中…</div></div>' +
            '    <div class="gc-modal-actions"><button class="gc-btn" data-gc="appliesClose">关闭</button></div>' +
            '  </div>' +
            '</div>' +
            /* 成员列表弹窗 */
            '<div class="gc-mask" data-gc-ref="membersMask" style="display:none">' +
            '  <div class="gc-modal gc-modal-lg">' +
            '    <div class="gc-modal-title">群成员</div>' +
            '    <div data-gc-ref="inviteWrap" style="display:none">' +
            '      <div style="display:flex;gap:8px;margin-bottom:8px">' +
            '        <input class="gc-input" data-gc-ref="inviteInput" placeholder="搜索用户名/昵称…" style="flex:1" maxlength="50">' +
            '        <button class="gc-btn gc-btn-primary" data-gc="inviteSearch">搜索</button>' +
            '      </div>' +
            '      <div data-gc-ref="inviteResult" class="gc-modal-body" style="max-height:170px;margin-bottom:10px"></div>' +
            '    </div>' +
            '    <div data-gc-ref="membersList" class="gc-modal-body"><div class="gc-empty">加载中…</div></div>' +
            '    <div class="gc-modal-actions"><button class="gc-btn" data-gc="membersClose">关闭</button></div>' +
            '  </div>' +
            '</div>' +
            /* 已读明细弹窗 */
            '<div class="gc-mask" data-gc-ref="readersMask" style="display:none">' +
            '  <div class="gc-modal gc-modal-lg">' +
            '    <div class="gc-modal-title">已读明细</div>' +
            '    <div data-gc-ref="readersList" class="gc-modal-body"><div class="gc-empty">加载中…</div></div>' +
            '    <div class="gc-modal-actions"><button class="gc-btn" data-gc="readersClose">关闭</button></div>' +
            '  </div>' +
            '</div>' +
            /* 图片全屏预览 */
            '<div class="gc-imgview" data-gc-ref="imgViewer" style="display:none"><img alt=""></div>';
    }

    function bindEvents($c) {
        // 统一事件委托
        $c.on('click', '[data-gc]', function (e) {
            var action = $(this).data('gc');
            var $t = $(this);
            switch (action) {
                case 'create': openCreate($c); break;
                case 'createCancel': $c.find('[data-gc-ref="createMask"]').hide(); break;
                case 'createOk': doCreate($c); break;
                case 'applies': openApplies($c); break;
                case 'appliesClose': $c.find('[data-gc-ref="appliesMask"]').hide(); break;
                case 'search': doSearch($c); break;
                case 'apply': doApply($c, $t.data('id')); break;
                case 'enter': enterChat($c, $t.data('id'), $t.data('name'), Number($t.data('owner'))); break;
                case 'back': leaveChat($c); break;
                case 'members': openMembers($c); break;
                case 'membersClose': $c.find('[data-gc-ref="membersMask"]').hide(); break;
                case 'kick': doKick($c, $t.data('id')); break;
                case 'inviteSearch': doInviteSearch($c); break;
                case 'invite': doInvite($c, $t.data('id')); break;
                case 'reads': openReaders($c, $t.data('msgid')); break;
                case 'readersClose': $c.find('[data-gc-ref="readersMask"]').hide(); break;
                case 'emoji': $c.find('[data-gc-ref="emojiPanel"]').toggle(); break;
                case 'img': $c.find('[data-gc-ref="imgInput"]').click(); break;
                case 'send': sendText($c); break;
                case 'approve': doHandle($c, $t.data('id'), true); break;
                case 'reject': doHandle($c, $t.data('id'), false); break;
                case 'retry': retrySend($c, $t.data('lid')); break;
            }
        });
        // 表情面板项（动态生成，单独委托）
        $c.on('click', '.gc-emoji-item', function () {
            var $input = $c.find('[data-gc-ref="textInput"]');
            $input.val($input.val() + $(this).data('e')).focus();
        });
        // 回车发送
        $c.on('keydown', '[data-gc-ref="textInput"]', function (e) {
            if (e.key === 'Enter') { e.preventDefault(); sendText($c); }
        });
        // 邀请搜索回车
        $c.on('keydown', '[data-gc-ref="inviteInput"]', function (e) {
            if (e.key === 'Enter') { e.preventDefault(); doInviteSearch($c); }
        });
        // 图片选择 → 上传 → 发送
        $c.on('change', '[data-gc-ref="imgInput"]', function () {
            if (this.files && this.files[0]) sendImage($c, this.files[0]);
            $(this).val('');
        });
        // 图片全屏预览：点击图片打开，点击遮罩关闭
        $c.on('click', '.gc-msg img', function () {
            var $v = $c.find('[data-gc-ref="imgViewer"]');
            $v.find('img').attr('src', $(this).attr('src'));
            $v.show();
        });
        $c.on('click', '[data-gc-ref="imgViewer"]', function () { $(this).hide(); });
    }

    /* ---------------- 群列表 / 搜索 ---------------- */

    function loadGroups($c) {
        gcGet('group/my', function (list) {
            state.groups = list || [];
            var html = '';
            if (!state.groups.length) html = '<div class="gc-empty">暂未加入任何群聊，可创建或搜索申请加入</div>';
            state.groups.forEach(function (g) {
                html += '<div class="gc-group-item">' +
                    '<div class="gc-group-info" data-gc="enter" data-id="' + g.id + '" data-name="' + esc(g.name) + '" data-owner="' + g.ownerId + '">' +
                    '<div class="gc-avatar">' + esc((g.name || '?').charAt(0)) + '</div>' +
                    '<div class="gc-group-meta">' +
                    '<div class="gc-group-name">' + esc(g.name) + (Number(g.ownerId) === state.myId ? '<span class="gc-tag">群主</span>' : '') + '</div>' +
                    '<div class="gc-group-sub">' + esc(g.ownerName || '') + ' · ' + g.memberCount + '人</div>' +
                    '</div></div>' +
                    '<button class="gc-btn gc-btn-sm" data-gc="enter" data-id="' + g.id + '" data-name="' + esc(g.name) + '" data-owner="' + g.ownerId + '">进入</button>' +
                    '</div>';
            });
            $c.find('[data-gc-ref="groupList"]').html(html);
        }, function () {
            $c.find('[data-gc-ref="groupList"]').html('<div class="gc-empty">加载失败</div>');
        });
    }

    function doSearch($c) {
        var kw = $c.find('[data-gc-ref="searchInput"]').val().trim();
        if (!kw) { $c.find('[data-gc-ref="searchWrap"]').hide(); return; }
        gcGet('group/search?keyword=' + encodeURIComponent(kw), function (list) {
            var html = '';
            if (!list || !list.length) html = '<div class="gc-empty">未找到相关群聊</div>';
            (list || []).forEach(function (g) {
                var btn;
                if (Number(g.joined) > 0) {
                    btn = '<button class="gc-btn gc-btn-sm" data-gc="enter" data-id="' + g.id + '" data-name="' + esc(g.name) + '" data-owner="' + esc(g.ownerId) + '">进入</button>';
                } else if (Number(g.applying) > 0) {
                    btn = '<span class="gc-tag gc-tag-wait">已申请</span>';
                } else {
                    btn = '<button class="gc-btn gc-btn-sm gc-btn-primary" data-gc="apply" data-id="' + g.id + '">申请加入</button>';
                }
                html += '<div class="gc-group-item">' +
                    '<div class="gc-group-info">' +
                    '<div class="gc-avatar">' + esc((g.name || '?').charAt(0)) + '</div>' +
                    '<div class="gc-group-meta">' +
                    '<div class="gc-group-name">' + esc(g.name) + '</div>' +
                    '<div class="gc-group-sub">群主 ' + esc(g.ownerName || '') + ' · ' + g.memberCount + '人</div>' +
                    '</div></div>' + btn + '</div>';
            });
            $c.find('[data-gc-ref="searchResult"]').html(html);
            $c.find('[data-gc-ref="searchWrap"]').show();
        }, function () {
            gcMsg('搜索失败');
        });
    }

    function doApply($c, groupId) {
        gcPost('group/apply', { groupId: Number(groupId) }, function () {
            gcMsg('申请已提交，等待管理员审批');
            doSearch($c);
        }, function (msg) { gcMsg(msg); });
    }

    /* ---------------- 创建群聊 / 审批 / 踢人 ---------------- */

    function openCreate($c) {
        $c.find('[data-gc-ref="groupNameInput"]').val('');
        $c.find('[data-gc-ref="createMask"]').show();
        $c.find('[data-gc-ref="groupNameInput"]').focus();
    }

    function doCreate($c) {
        var name = $c.find('[data-gc-ref="groupNameInput"]').val().trim();
        if (!name) { gcMsg('请输入群聊名称'); return; }
        gcPost('group/create', { name: name }, function () {
            $c.find('[data-gc-ref="createMask"]').hide();
            gcMsg('创建成功');
            loadGroups($c);
        }, function (msg) { gcMsg(msg); });
    }

    function openApplies($c) {
        $c.find('[data-gc-ref="appliesMask"]').show();
        gcGet('group/applies', function (list) {
            var html = '';
            if (!list || !list.length) html = '<div class="gc-empty">暂无待审批申请</div>';
            (list || []).forEach(function (a) {
                html += '<div class="gc-apply-item">' +
                    '<div class="gc-group-meta">' +
                    '<div class="gc-group-name">' + esc(a.name) + ' 申请加入「' + esc(a.groupName) + '」</div>' +
                    '<div class="gc-group-sub">' + (a.reason ? esc(a.reason) + ' · ' : '') + esc(a.applyTime || '') + '</div>' +
                    '</div>' +
                    '<div>' +
                    '<button class="gc-btn gc-btn-sm gc-btn-primary" data-gc="approve" data-id="' + a.id + '">通过</button>' +
                    ' <button class="gc-btn gc-btn-sm gc-btn-danger" data-gc="reject" data-id="' + a.id + '">拒绝</button>' +
                    '</div></div>';
            });
            $c.find('[data-gc-ref="appliesList"]').html(html);
            $c.find('[data-gc-ref="applyBadge"]').hide();
        }, function () {
            $c.find('[data-gc-ref="appliesList"]').html('<div class="gc-empty">加载失败</div>');
        });
    }

    function doHandle($c, applyId, approve) {
        gcPost('group/handle', { applyId: Number(applyId), approve: approve }, function () {
            gcMsg(approve ? '已通过' : '已拒绝');
            openApplies($c);
            loadGroups($c);
        }, function (msg) { gcMsg(msg); });
    }

    function openMembers($c) {
        if (!state.chat) return;
        $c.find('[data-gc-ref="membersMask"]').show();
        // 邀请区仅群主可见
        $c.find('[data-gc-ref="inviteWrap"]').toggle(state.chat.amIOwner);
        $c.find('[data-gc-ref="inviteInput"]').val('');
        $c.find('[data-gc-ref="inviteResult"]').empty();
        gcGet('group/members?groupId=' + state.chat.groupId, function (list) {
            state.members = list || [];
            var html = '';
            state.members.forEach(function (m) {
                var kick = '';
                if (state.chat.amIOwner && m.role !== 'owner') {
                    kick = ' <button class="gc-btn gc-btn-sm gc-btn-danger" data-gc="kick" data-id="' + m.userId + '">踢出</button>';
                }
                html += '<div class="gc-apply-item">' +
                    '<div class="gc-group-meta">' +
                    '<div class="gc-group-name">' + esc(m.name || m.userId) + (m.role === 'owner' ? '<span class="gc-tag">群主</span>' : '') + '</div>' +
                    '</div><div>' + kick + '</div></div>';
            });
            $c.find('[data-gc-ref="membersList"]').html(html || '<div class="gc-empty">无成员</div>');
        }, function () {
            $c.find('[data-gc-ref="membersList"]').html('<div class="gc-empty">加载失败</div>');
        });
    }

    function doKick($c, userId) {
        if (!state.chat) return;
        gcConfirm('确定将该成员踢出群聊？', function () {
            gcPost('group/kick', { groupId: state.chat.groupId, userId: Number(userId) }, function () {
                gcMsg('已踢出');
                $c.find('[data-gc-ref="membersMask"]').hide();
                openMembers($c);
            }, function (msg) { gcMsg(msg); });
        });
    }

    /** 邀请：模糊搜索用户（仅群主） */
    function doInviteSearch($c) {
        if (!state.chat) return;
        var kw = $c.find('[data-gc-ref="inviteInput"]').val().trim();
        if (!kw) { gcMsg('请输入用户名或昵称关键字'); return; }
        gcGet('group/users?groupId=' + state.chat.groupId + '&keyword=' + encodeURIComponent(kw), function (list) {
            var html = '';
            if (!list || !list.length) html = '<div class="gc-empty">未找到相关用户</div>';
            (list || []).forEach(function (u) {
                var btn = Number(u.joined) > 0
                    ? '<span class="gc-tag gc-tag-wait">已加入</span>'
                    : '<button class="gc-btn gc-btn-sm gc-btn-primary" data-gc="invite" data-id="' + u.id + '">邀请</button>';
                html += '<div class="gc-apply-item">' +
                    '<div class="gc-group-meta">' +
                    '<div class="gc-group-name">' + esc(u.name) + '</div>' +
                    '<div class="gc-group-sub">' + esc(u.username || '') + '</div>' +
                    '</div><div>' + btn + '</div></div>';
            });
            $c.find('[data-gc-ref="inviteResult"]').html(html);
        }, function (msg) { gcMsg(msg); });
    }

    function doInvite($c, targetUserId) {
        if (!state.chat) return;
        var kw = $c.find('[data-gc-ref="inviteInput"]').val().trim();
        gcPost('group/invite', { groupId: state.chat.groupId, userId: Number(targetUserId) }, function () {
            gcMsg('已邀请入群');
            // 刷新成员列表与搜索结果（已加入标记）
            openMembers($c);
            if (kw) {
                $c.find('[data-gc-ref="inviteInput"]').val(kw);
                doInviteSearch($c);
            }
        }, function (msg) { gcMsg(msg); });
    }

    /* ---------------- 聊天 ---------------- */

    function enterChat($c, groupId, name, ownerId) {
        stopPoll();
        state.chat = {
            groupId: Number(groupId),
            name: name,
            amIOwner: state.myId != null && Number(ownerId) === state.myId
        };
        $c.find('[data-gc-ref="chatName"]').text(name);
        $c.find('[data-gc-ref="listView"]').hide();
        $c.find('[data-gc-ref="chatView"]').show();
        $c.find('[data-gc-ref="emojiPanel"]').hide();
        renderMsgs($c);
        loadReads($c);
        startPoll($c);
    }

    function leaveChat($c) {
        stopPoll();
        state.chat = null;
        $c.find('[data-gc-ref="chatView"]').hide();
        $c.find('[data-gc-ref="listView"]').show();
        loadGroups($c);
    }

    function startPoll($c) {
        stopPoll();
        pullOnce($c);
        state.timer = setInterval(function () { pullOnce($c); }, POLL_INTERVAL);
    }

    function stopPoll() {
        if (state.timer) { clearInterval(state.timer); state.timer = null; }
    }

    /** 拉取我的中转消息（服务端拉走即删），按群写入本地缓存 */
    function pullOnce($c) {
        gcGet('group/pull', function (list) {
            var touched = {};
            if (list && list.length) {
                list.forEach(function (m) {
                    appendMsg(m.groupId, {
                        type: m.msgType,
                        content: m.content,
                        from: m.fromName || m.fromUserId,
                        time: m.time,
                        mine: false
                    });
                    touched[m.groupId] = true;
                });
                // 只在当前打开的群有新消息时刷新视图
                if (state.chat && touched[state.chat.groupId]) renderMsgs($c);
            }
            // 同步自己消息的已读计数
            loadReads($c);
        }, function () { /* 静默：下一轮重试 */ });
    }

    /** 拉取自己在当前群发出消息的已读计数（仅发送者可见，接口侧也做了归属校验） */
    function loadReads($c) {
        if (!state.chat) return;
        gcGet('group/reads?groupId=' + state.chat.groupId, function (list) {
            if (!state.chat) return;
            var map = {};
            (list || []).forEach(function (r) {
                map[Number(r.msgId)] = { readCount: Number(r.readCount) || 0, total: Number(r.total) || 0 };
            });
            var gid = state.chat.groupId;
            var sig = JSON.stringify(map);
            if (state.reads[gid] !== undefined && state.readsSig[gid] === sig) return; // 无变化不重绘
            state.readsSig[gid] = sig;
            state.reads[gid] = map;
            renderMsgs($c);
        }, function () { /* 静默 */ });
    }

    /** 已读明细弹窗 */
    function openReaders($c, msgId) {
        $c.find('[data-gc-ref="readersMask"]').show();
        $c.find('[data-gc-ref="readersList"]').html('<div class="gc-empty">加载中…</div>');
        gcGet('group/readers?msgId=' + msgId, function (list) {
            var html = '';
            if (!list || !list.length) html = '<div class="gc-empty">还没有人读过</div>';
            (list || []).forEach(function (r) {
                html += '<div class="gc-apply-item">' +
                    '<div class="gc-group-meta">' +
                    '<div class="gc-group-name">' + esc(r.name || r.userId) + '</div>' +
                    '<div class="gc-group-sub">读于 ' + esc(r.readTime || '') + '</div>' +
                    '</div></div>';
            });
            $c.find('[data-gc-ref="readersList"]').html(html);
        }, function () {
            $c.find('[data-gc-ref="readersList"]').html('<div class="gc-empty">加载失败</div>');
        });
    }

    /** 卡死自愈：超过 60 秒仍是“发送中”的本地消息按已发送处理（旧版本遗留或缓存写入失败兜底） */
    function healStuckPending(list) {
        var changed = false;
        var now = Date.now();
        list.forEach(function (m) {
            if (m.mine && m.pending && m.time) {
                var t = new Date(String(m.time).replace(/-/g, '/')).getTime();
                if (!isNaN(t) && now - t > 60000) { m.pending = false; changed = true; }
            }
        });
        if (changed) saveCache(state.chat.groupId, list);
    }

    /** 自己消息的状态：有已读数据 → 已读 N/M（可点击查看明细）；否则 → 已发送 */
    function readBadgeHtml(m) {
        if (!m.msgId) return '<span class="gc-msg-status">已发送</span>';
        var info = (state.reads[state.chat.groupId] || {})[Number(m.msgId)];
        if (!info) return '<span class="gc-msg-status">已发送</span>';
        return '<span class="gc-msg-status gc-read-badge" data-gc="reads" data-msgid="' + m.msgId + '">已读 ' + info.readCount + '/' + info.total + '</span>';
    }

    function renderMsgs($c) {
        if (!state.chat) return;
        var list = loadCache(state.chat.groupId);
        healStuckPending(list);
        var html = '';
        if (!list.length) {
            html = '<div class="gc-empty" style="margin-top:60px">暂无消息，发送第一条消息吧</div>';
        }
        list.forEach(function (m) {
            var cls = m.mine ? 'gc-msg mine' : 'gc-msg';
            var body;
            if (m.type === 'image') {
                body = '<img src="' + esc(resolveImgUrl(m.content)) + '" alt="图片" class="gc-msg-img">';
            } else {
                body = '<div class="gc-bubble-text">' + esc(m.content) + '</div>';
            }
            var status = '';
            if (m.pending) status = '<span class="gc-msg-status">发送中…</span>';
            else if (m.failed) status = '<span class="gc-msg-status gc-msg-fail" data-gc="retry" data-lid="' + m.lid + '">发送失败，点击重试</span>';
            else if (m.mine) status = readBadgeHtml(m);
            html += '<div class="' + cls + '">' +
                '<div class="gc-msg-meta">' + esc(m.mine ? '我' : (m.from || '')) + ' · ' + esc(m.time || '') + '</div>' +
                body + status +
                '</div>';
        });
        var $list = $c.find('[data-gc-ref="msgList"]');
        $list.html(html);
        $list.scrollTop($list[0].scrollHeight);
    }

    function sendText($c) {
        if (!state.chat) return;
        var $input = $c.find('[data-gc-ref="textInput"]');
        var text = $input.val().trim();
        if (!text) return;
        $input.val('');
        $c.find('[data-gc-ref="emojiPanel"]').hide();
        deliver($c, { type: 'text', content: text });
    }

    function sendImage($c, file) {
        if (!state.chat) return;
        var fd = new FormData();
        fd.append('image', file);
        $.ajax({ url: 'group/img', type: 'POST', data: fd, processData: false, contentType: false, dataType: 'json', headers: ajaxHeaders() })
            .done(function (res) {
                if (res.errorCode === '000000' && res.body) {
                    deliver($c, { type: 'image', content: res.body });
                } else {
                    gcMsg(res.errorMsg || '图片上传失败');
                }
            })
            .fail(function (jqXHR) {
                if (jqXHR.status !== 401) gcMsg('图片上传失败');
            });
    }

    /** 乐观发送：先渲染，成功后回写 msgId（→ 已发送/已读），失败可重试 */
    function deliver($c, payload) {
        var groupId = state.chat.groupId;
        var lid = ++state.seq;
        appendMsg(groupId, {
            lid: lid,
            type: payload.type,
            content: payload.content,
            from: '我',
            time: nowStr(),
            mine: true,
            pending: true
        });
        renderMsgs($c);
        gcPost('group/send', {
            groupId: groupId,
            type: payload.type,
            content: payload.content
        }, function (msgId) {
            updateMsg(groupId, lid, { pending: false, msgId: msgId != null ? Number(msgId) : null });
            renderMsgs($c);
        }, function (msg) {
            updateMsg(groupId, lid, { pending: false, failed: true, errMsg: msg });
            renderMsgs($c);
        });
    }

    function retrySend($c, lid) {
        if (!state.chat) return;
        var groupId = state.chat.groupId;
        var list = loadCache(groupId);
        var msg = null;
        for (var i = 0; i < list.length; i++) {
            if (list[i].lid === Number(lid)) { msg = list[i]; break; }
        }
        if (!msg) return;
        updateMsg(groupId, msg.lid, { pending: true, failed: false });
        renderMsgs($c);
        gcPost('group/send', { groupId: groupId, type: msg.type, content: msg.content }, function (msgId) {
            updateMsg(groupId, msg.lid, { pending: false, msgId: msgId != null ? Number(msgId) : null });
            renderMsgs($c);
        }, function (m) {
            updateMsg(groupId, msg.lid, { pending: false, failed: true, errMsg: m });
            renderMsgs($c);
        });
    }

    /* ---------------- 对外接口 ---------------- */

    window.GroupChat = { mount: mount };

    /* H5：注册为模块，goPage('group') 时自动挂载到 #page-group */
    if (window.registerH5Module) {
        window.registerH5Module('group', function ($page) {
            window.GroupChat.mount($page[0]);
        });
    }
})();
