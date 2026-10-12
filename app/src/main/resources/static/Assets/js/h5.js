/* ============================================================
   H5 移动端核心 + 轻/中模块（小泽助手）
   复用后端 /api 端点；鉴权：所有请求带 X-Requested-With，401 → 登录浮层。
   重模块（javaapi / sql / dubbo）见 h5-javaapi.js / h5-sqltool.js / h5-dubbo.js。
   ============================================================ */

window.H5Modules = {};
window.registerH5Module = function (name, fn) { window.H5Modules[name] = fn; };

/* ---------------- 禁用左滑/右滑返回（浏览器手势导航） ----------------
   手机浏览器在页面边缘滑动会触发 前进/后退，导致 H5 被意外切走。
   方案：加载时推入一个守卫历史状态；无论用户怎么滑，popstate 一触发
   就立刻再推入守卫状态，浏览器始终停留在本页（返回键/侧滑均不再离开）。 */
(function () {
    if (!window.history || !history.pushState) return;
    var guard = function () {
        try { history.pushState({ h5: 'guard' }, '', location.href); } catch (e) {}
    };
    guard();
    window.addEventListener('popstate', guard);
    /* bfcache 恢复（如从其他页返回）时补一次守卫，防止历史位被消耗 */
    window.addEventListener('pageshow', function (e) { if (e.persisted) guard(); });
})();

var H5State = { userId: null, username: '', isAdmin: false, theme: 'dark', menus: [], visibleKeys: null, currentPage: null };

/* ---------------- 菜单可见性（menu/list 接口） ---------------- */
/* H5State.menus 为 menu/list?platform=h5 的 body（按 sortNo 排序）；
   visibleKeys 为其 key 派生列表；拉取失败置 null → 默认全部可见。home/profile 固定可见 */
function menuVisible(name) {
    if (name === 'home' || name === 'profile') return true;
    if (!H5State.visibleKeys) return true; // 未拉取到配置时默认可见
    return H5State.visibleKeys.indexOf(name) !== -1;
}

/* ---------------- 通用工具 ---------------- */

function esc(s) {
    return s == null ? '' : String(s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/** 统一 AJAX：带 X-Requested-With，使拦截器返回 401 JSON 而非重定向 */
function api(opts) {
    return $.ajax($.extend({
        type: 'POST',
        dataType: 'json',
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    }, opts));
}

/** POST {body:{...}} 包裹的 Result 类接口 */
function postJSON(url, body) {
    return api({
        url: url, type: 'POST', contentType: 'application/json',
        data: JSON.stringify(body == null ? {} : { body: body })
    });
}

var toastTimer = null;
function toast(msg) {
    var t = $('#h5Toast');
    t.text(msg).show();
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.hide(); }, 2200);
}

function loading(on) { $('#h5Loading').toggleClass('show', !!on); }

/* ---- 居中对话框 ---- */
function openDialog(html) { $('#h5Dialog').html(html); $('#h5DialogMask').addClass('show'); }
function closeDialog() { $('#h5DialogMask').removeClass('show'); }
function confirm(msg, onOk) {
    openDialog('<h3>提示</h3><div style="color:var(--text-2)">' + esc(msg) + '</div>' +
        '<div class="h5-dialog-actions">' +
        '<button class="h5-btn" id="h5DlgCancel">取消</button>' +
        '<button class="h5-btn-primary" id="h5DlgOk">确认</button></div>');
    $('#h5DlgCancel').one('click', closeDialog);
    $('#h5DlgOk').one('click', function () { closeDialog(); if (onOk) onOk(); });
}

/* ---- 底部抽屉 ---- */
function openSheet(html) {
    $('#h5Sheet').html('<span class="h5-sheet-close" style="position:absolute;top:10px;right:14px;">×</span>' + html);
    $('#h5SheetMask').addClass('show');
    $('#h5Sheet').addClass('show');
}
function closeSheet() { $('#h5SheetMask').removeClass('show'); $('#h5Sheet').removeClass('show'); }

/* ---- 浮动按钮 ---- */
function setFab(show, handler) {
    var fab = $('#h5Fab');
    if (show) { fab.show(); fab.off('click').on('click', handler); }
    else fab.hide();
}

/* ---- 尺寸/时间格式化 ---- */
function fmtSize(n) {
    n = Number(n) || 0;
    if (n > 1048576) return (n / 1048576).toFixed(2) + ' MB';
    if (n > 1024) return (n / 1024).toFixed(1) + ' KB';
    return n + ' B';
}
function fmtTime(ts) {
    var d = new Date(Number(ts));
    if (isNaN(d.getTime())) return '';
    function p(i) { return String(i).padStart(2, '0'); }
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

/* ---------------- 主题 ---------------- */
var THEMES = ['dark', 'light', 'eyecare', 'techblue', 'orange', 'pink', 'system'];
var THEME_LABELS = { dark: '暗色', light: '亮色', eyecare: '护眼绿', techblue: '科技蓝', orange: '活力橙', pink: '少女粉', system: '跟随系统' };

function applyTheme(t) {
    if (!t) t = 'dark';
    var resolved = t === 'system'
        ? ((window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light')
        : t;
    document.documentElement.setAttribute('data-theme', resolved);
    H5State.theme = t;
    try { localStorage.setItem('wxyd-theme', t); } catch (e) { }
}

function openThemeSheet() {
    var html = '<div class="h5-sheet-head"><h3>主题</h3></div><div class="h5-theme-list">';
    THEMES.forEach(function (t) {
        html += '<div class="h5-theme-opt' + (H5State.theme === t ? ' active' : '') + '" data-t="' + t + '">' + THEME_LABELS[t] + '</div>';
    });
    html += '</div>';
    openSheet(html);
    $('#h5Sheet .h5-theme-opt').one('click', function () {
        var t = $(this).data('t');
        applyTheme(t);
        $('#h5Sheet .h5-theme-opt').removeClass('active');
        $(this).addClass('active');
        if (H5State.username) {
            postJSON('user/updateTheme', { theme: t }).fail(function () { });
        }
        toast('主题已切换');
    });
}

/* ---------------- 登录 / 启动 ---------------- */

function showLogin() {
    $('#h5LoginErr').text('');
    $('#h5LoginUser').val('');
    $('#h5LoginPwd').val('');
    $('#h5LoginMask').addClass('show');
    setFab(false);
    closeSheet();
}

function hideLogin() { $('#h5LoginMask').removeClass('show'); }

/* GitHub 授权回调后（h5.html?oauth=error|disabled）给出提示 */
(function () {
    try {
        var p = new URLSearchParams(location.search).get('oauth');
        if (p === 'error' || p === 'disabled' || p === 'neterror') {
            history.replaceState(null, '', location.pathname); // 去掉参数，避免重复提示
            if (p === 'error') toast('GitHub 授权登录失败，请重试或使用密码登录');
            else if (p === 'neterror') toast('无法连接 GitHub：服务器出网被拦截或未配置代理');
            else toast('GitHub 登录未配置，请使用密码登录');
        }
    } catch (e) {}
})();

function doLogin() {
    var u = $('#h5LoginUser').val().trim(), p = $('#h5LoginPwd').val();
    if (!u || !p) { $('#h5LoginErr').text('请输入用户名/邮箱和密码'); return; }
    loading(true);
    postJSON('login', { username: u, password: p }).done(function (res) {
        loading(false);
        if (res.errorCode === '000000') { hideLogin(); bootstrap(); }
        else $('#h5LoginErr').text(res.errorMsg || '登录失败');
    }).fail(function () {
        loading(false);
        $('#h5LoginErr').text('网络错误，请稍后重试');
    });
}

/* 分享链接参数：/api/h5.html?welfare=<id>（免登录访客也可查看该帖） */
function getShareWelfareId() {
    var m = location.search.match(/[?&]welfare=(\d+)/);
    return m ? m[1] : null;
}

/* 访客模式：未登录但带着分享链接打开 → 仅展示该帖详情，其余功能仍需登录 */
function enterGuestShare() {
    var id = getShareWelfareId();
    if (!id || !window.H5Welfare) { showLogin(); return; }
    hideLogin();
    window.H5Welfare.openShared(id);
}

function bootstrap() {
    api({ url: 'user/info', type: 'GET' }).done(function (res) {
        if (res.errorCode === '000000' && res.body) {
            H5State.userId = res.body.id || null;
            H5State.username = res.body.username || '';
            H5State.isAdmin = !!res.body.isAdmin;
            hideLogin();
            /* 先拉取菜单列表（menu/list）再进首页（菜单管理功能） */
            api({ url: 'menu/list?platform=h5', type: 'GET' }).always(function (mv) {
                if (mv && mv.errorCode === '000000' && mv.body) {
                    H5State.menus = mv.body;
                    H5State.visibleKeys = mv.body.map(function (m) { return m.key; });
                } else {
                    /* 拉取失败：回退默认全可见语义 */
                    H5State.menus = null;
                    H5State.visibleKeys = null;
                }
                /* 底部 tab 按配置显隐 */
                ['message', 'group'].forEach(function (t) {
                    $('#h5Tabbar .h5-tab[data-target="' + t + '"]').toggle(menuVisible(t));
                });
                /* 分享链接：/api/h5.html?welfare=<id> 打开自动落在该帖详情 */
                var share = getShareWelfareId();
                if (share && window.H5Welfare) {
                    goPage('welfare');
                    window.H5Welfare.openShared(share);
                } else {
                    goPage(currentHashKey() || 'home');
                }
            });
            api({ url: 'user/theme', type: 'GET' }).done(function (r) {
                if (r.errorCode === '000000' && r.body) applyTheme(r.body);
            });
        } else if (getShareWelfareId()) {
            enterGuestShare();
        } else showLogin();
    }); /* 401 由全局 ajaxError 统一弹出登录（带分享链接时进访客模式） */
}

function doLogout() {
    api({ url: 'logout', type: 'POST' }).always(function () {
        H5State.username = ''; H5State.isAdmin = false;
        showLogin();
    });
}

/* ---------------- 路由 ---------------- */

function goPage(name) {
    if (!H5State.username && name !== 'home') { showLogin(); return; }
    /* 菜单管理配置：被隐藏的模块回到首页 */
    if (!menuVisible(name)) { name = 'home'; }
    /* 手输未知 hash 兜底（菜单拉取失败时 menuVisible 默认放行，页面不存在则回首页） */
    if ($('#page-' + name).length === 0) { name = 'home'; }
    H5State.currentPage = name;
    $('.h5-page').removeClass('active');
    $('#page-' + name).addClass('active');
    $('#h5Tabbar .h5-tab').removeClass('active');
    $('#h5Tabbar .h5-tab[data-target="' + name + '"]').addClass('active');
    setFab(false);
    var r = PAGE_RENDER[name];
    if (r) r($('#page-' + name));
    else if (window.H5Modules[name]) window.H5Modules[name]($('#page-' + name));
    $('.h5-main').scrollTop(0);
    /* 同步 hash（hashchange 回调按 currentPage 去重，不会回环） */
    var target = '#/' + name;
    if (location.hash !== target) location.hash = target;
}

/** 解析当前 hash 为页面 key：#/welfare → welfare；无 hash → null */
function currentHashKey() {
    var h = (location.hash || '').replace(/^#\/?/, '');
    return h || null;
}

/* hash 路由：深度链接与 hash 导航（popstate 守卫为兼容旧版保留，会清空前进栈）；
   goPage 内部改 hash 触发的 hashchange 因 key === currentPage 被忽略，避免循环 */
window.addEventListener('hashchange', function () {
    var key = currentHashKey();
    if (key && key !== H5State.currentPage) goPage(key);
});

/* ---------------- 首页 ---------------- */

function renderHome() {
    /* 数据驱动：menu/list 结果（已按 sortNo 排序、服务端已过滤权限/管理员项）；
       hashUrl 为空的行（message/group）是底部 tab，不出卡片 */
    var items = (H5State.menus || []).filter(function (m) {
        return m.hashUrl && menuVisible(m.key);
    });
    var html = '<div class="h5-section-title">你好，' + esc(H5State.username) + ' 👋</div>' +
        '<div class="h5-alert h5-alert-info">点击下方模块卡片进入对应功能</div>' +
        '<div class="h5-grid">';
    items.forEach(function (m) {
        html += '<div class="h5-grid-item" data-mod="' + esc(m.key) + '">' +
            '<div class="h5-grid-ico">' + esc(m.icon || '•') + '</div>' +
            '<div class="h5-grid-name">' + esc(m.name) + '</div>' +
            (Number(m.adminOnly) === 1 ? '<div class="h5-grid-badge">管理员</div>' : '') + '</div>';
    });
    html += '</div>';
    $('#page-home').html(html);
    $('#page-home .h5-grid-item').on('click', function () { goPage($(this).data('mod')); });
}

/* ---------------- 我的 ---------------- */

function renderProfile() {
    var html =
        '<div class="h5-profile">' +
        '<div class="h5-avatar">' + esc((H5State.username || '?').charAt(0).toUpperCase()) + '</div>' +
        '<div class="h5-profile-name">' + esc(H5State.username) + '</div>' +
        '<div class="h5-profile-role">' + (H5State.isAdmin ? '超级管理员 <span class="h5-badge h5-badge-role">ADMIN</span>' : '普通用户') + '</div>' +
        '</div>' +
        '<div class="h5-card">' +
        '<div class="h5-list">' +
        '<li id="h5ProfileTheme" style="cursor:pointer"><div class="h5-li-main"><div class="h5-li-title">🎨 主题设置</div><div class="h5-li-sub">当前：' + (THEME_LABELS[H5State.theme] || H5State.theme) + '</div></div><span style="color:var(--text-6)">›</span></li>' +
        '<li id="h5ProfilePwd" style="cursor:pointer"><div class="h5-li-main"><div class="h5-li-title">🔑 修改密码</div><div class="h5-li-sub">定期更换密码更安全</div></div><span style="color:var(--text-6)">›</span></li>' +
        '</div></div>' +
        '<div class="h5-card"><button class="h5-btn-danger h5-btn-block" id="h5DoLogout">退出登录</button></div>';
    $('#page-profile').html(html);
    $('#h5ProfileTheme').on('click', openThemeSheet);
    $('#h5ProfilePwd').on('click', showChangePwd);
    $('#h5DoLogout').on('click', doLogout);
}

function showChangePwd() {
    openSheet('<div class="h5-sheet-head"><h3>修改密码</h3></div>' +
        '<div class="h5-field"><label>原密码</label><input class="h5-input" type="password" id="pwdOld" autocomplete="current-password"></div>' +
        '<div class="h5-field"><label>新密码</label><input class="h5-input" type="password" id="pwdNew" autocomplete="new-password"></div>' +
        '<div class="h5-field"><label>确认新密码</label><input class="h5-input" type="password" id="pwdNew2" autocomplete="new-password"></div>' +
        '<button class="h5-btn-primary h5-btn-block" id="pwdSave">保存</button>');
    $('#pwdSave').on('click', function () {
        var o = $('#pwdOld').val(), n = $('#pwdNew').val(), n2 = $('#pwdNew2').val();
        if (!o || !n) { toast('请输入原密码和新密码'); return; }
        if (n !== n2) { toast('两次输入的新密码不一致'); return; }
        if (n === o) { toast('新密码不能与原密码相同'); return; }
        postJSON('user/updatePassword', { oldPassword: o, newPassword: n }).done(function (res) {
            if (res.errorCode === '000000') { closeSheet(); toast('密码修改成功'); }
            else toast(res.errorMsg || '修改失败');
        });
    });
}

/* ---------------- 用户管理（管理员） ---------------- */

var STATUS_LABEL = { 0: '已通过', 1: '待审批', 2: '已拒绝', 3: '已暂停' };
var USER_ACT = { approve: 'approveUser', reject: 'rejectUser', pause: 'pauseUser', resume: 'resumeUser', del: 'deleteUser' };

function statusBadge(s) {
    var known = (s in STATUS_LABEL);
    return '<span class="h5-badge h5-badge-' + (known ? s : 1) + '">' + (STATUS_LABEL[s] || '未知') + '</span>';
}

function userActBtn(act, id, label, danger) {
    return '<button class="' + (danger ? 'h5-btn-danger' : 'h5-btn-primary') + ' h5-btn-sm" data-act="' + act + '" data-id="' + id + '">' + label + '</button>';
}

function renderUser($page) {
    $page.html('<div class="h5-section-title">用户管理<span class="h5-btn-ghost h5-btn-sm" id="usrRefresh">刷新</span></div><div id="usrList"><div class="h5-empty">加载中…</div></div>');
    loadUsers();
    $page.find('#usrRefresh').on('click', loadUsers);
}

function loadUsers() {
    loading(true);
    api({ url: 'user/list', type: 'GET' }).done(function (res) {
        loading(false);
        var list = (res.errorCode === '000000' && res.body) || [];
        var html = '';
        if (!list.length) html = '<div class="h5-empty">暂无用户</div>';
        list.forEach(function (u) {
            var actions;
            if (u.role == 0) {
                actions = '<span class="h5-badge h5-badge-1">不可操作</span>';
            } else if (u.status == 0) {
                actions = userActBtn('pause', u.id, '暂停') + ' ' + userActBtn('del', u.id, '删除', true);
            } else if (u.status == 1) {
                actions = userActBtn('approve', u.id, '通过') + ' ' + userActBtn('reject', u.id, '拒绝', true);
            } else if (u.status == 2) {
                actions = userActBtn('del', u.id, '删除', true);
            } else if (u.status == 3) {
                actions = userActBtn('resume', u.id, '恢复') + ' ' + userActBtn('del', u.id, '删除', true);
            } else {
                actions = '';
            }
            html += '<div class="h5-card"><div class="h5-card-row">' +
                '<div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + esc(u.username) + '</div>' +
                '<div class="h5-card-sub">' + esc(u.displayName || '') + ' · ' + (u.role == 0 ? '超管' : '普通') + ' · ' + esc(u.createTime || '') + '</div>' +
                '</div>' + statusBadge(u.status) + '</div>' +
                (actions ? '<div class="h5-card-actions">' + actions + '</div>' : '') +
                '</div>';
        });
        $('#usrList').html(html);
    }).fail(function () { loading(false); $('#usrList').html('<div class="h5-empty">加载失败</div>'); });
}

function doUserAction(act, id) {
    var ep = USER_ACT[act];
    if (!ep) return;
    loading(true);
    postJSON(ep, { id: Number(id) }).done(function (res) {
        loading(false);
        if (res.errorCode === '000000') { toast('操作成功'); loadUsers(); }
        else toast(res.errorMsg || '操作失败');
    }).fail(function () { loading(false); toast('请求失败'); });
}

/* ---------------- 留言板 ---------------- */

var msgFlag = '';

function renderMessage() {
    $('#page-message').html(
        '<div class="h5-section-title">留言板' +
        '<div><span class="h5-btn-ghost h5-btn-sm" id="msgTabAll" style="margin-right:6px">全部</span>' +
        '<span class="h5-btn-ghost h5-btn-sm" id="msgTabMine">我的</span></div></div>' +
        '<div id="msgList"><div class="h5-empty">加载中…</div></div>');
    setFab(true, showAddMessage);
    loadMessages('');
    $('#msgTabAll').on('click', function () { loadMessages(''); });
    $('#msgTabMine').on('click', function () { loadMessages('1'); });
}

function loadMessages(flag) {
    msgFlag = flag;
    loading(true);
    postJSON('queryMessage', { flag: flag }).done(function (res) {
        loading(false);
        var list = (res.errorCode === '000000' && res.body && res.body.msgList) || [];
        var html = '';
        if (!list.length) html = '<div class="h5-empty">暂无留言，点击右下角 ＋ 发布</div>';
        list.forEach(function (m) {
            html += '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + esc(m.name || '匿名') + '</div>' +
                '<div style="margin:8px 0;color:var(--text-2);font-size:13.5px;white-space:pre-wrap;word-break:break-word;">' + esc(m.content || '') + '</div>' +
                '<div class="h5-card-sub">' + esc(m.time || '') + '</div></div>' +
                '<button class="h5-btn-danger h5-btn-sm" data-id="' + esc(m.id) + '">删除</button></div></div>';
        });
        $('#msgList').html(html);
    }).fail(function () { loading(false); $('#msgList').html('<div class="h5-empty">加载失败</div>'); });
}

function showAddMessage() {
    openSheet('<div class="h5-sheet-head"><h3>新增留言</h3></div>' +
        '<div class="h5-field"><textarea class="h5-textarea" id="msgInput" style="min-height:110px;font-family:inherit" placeholder="写下你的留言…"></textarea></div>' +
        '<button class="h5-btn-primary h5-btn-block" id="msgSubmit">发布留言</button>');
    $('#msgSubmit').on('click', function () {
        var c = $('#msgInput').val().trim();
        if (!c) { toast('留言内容不能为空'); return; }
        postJSON('addMessage', { message: c }).done(function (res) {
            if (res.errorCode === '000000') { closeSheet(); toast('已发布'); loadMessages(msgFlag); }
            else toast(res.errorMsg || '发布失败');
        });
    });
}

/* ---------------- 钉钉机器人 ---------------- */

var robotCache = [];

function renderRobot() {
    $('#page-robot').html('<div class="h5-section-title">钉钉机器人<span class="h5-btn-ghost h5-btn-sm" id="robotRefresh">刷新</span></div><div id="robotList"><div class="h5-empty">加载中…</div></div>');
    setFab(true, function () { showRobotForm(null); });
    loadRobots();
    $('#robotRefresh').on('click', loadRobots);
}

function loadRobots() {
    loading(true);
    api({ url: 'robot/list', type: 'GET' }).done(function (res) {
        loading(false);
        var list = (res.errorCode === '000000' && res.body) || [];
        robotCache = list;
        var html = '';
        if (!list.length) html = '<div class="h5-empty">暂无机器人，点击 ＋ 新增</div>';
        list.forEach(function (r) {
            html += '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + esc(r.name || '') + '</div>' +
                '<div class="h5-card-sub">Token 尾号：' + esc((r.accessToken || '').slice(-6)) + '</div></div></div>' +
                '<div class="h5-card-actions">' +
                '<button class="h5-btn-primary h5-btn-sm" data-act="send" data-id="' + r.id + '">发送</button>' +
                '<button class="h5-btn h5-btn-sm" data-act="edit" data-id="' + r.id + '">编辑</button>' +
                '<button class="h5-btn-danger h5-btn-sm" data-act="del" data-id="' + r.id + '">删除</button>' +
                '</div></div>';
        });
        $('#robotList').html(html);
    }).fail(function () { loading(false); $('#robotList').html('<div class="h5-empty">加载失败</div>'); });
}

function showRobotForm(robot) {
    var isEdit = !!robot;
    openSheet('<div class="h5-sheet-head"><h3>' + (isEdit ? '编辑机器人' : '新增机器人') + '</h3></div>' +
        '<div class="h5-field"><label>名称</label><input class="h5-input" id="rbName" value="' + esc(robot && robot.name || '') + '"></div>' +
        '<div class="h5-field"><label>ACCESS_TOKEN</label><input class="h5-input h5-code" id="rbToken" value="' + esc(robot && robot.accessToken || '') + '"></div>' +
        '<div class="h5-field"><label>SECRET（编辑时留空则不修改）</label><input class="h5-input h5-code" id="rbSecret" value=""></div>' +
        '<button class="h5-btn-primary h5-btn-block" id="rbSave">保存</button>');
    $('#rbSave').on('click', function () {
        var body = { name: $('#rbName').val().trim(), accessToken: $('#rbToken').val().trim(), secret: $('#rbSecret').val().trim() };
        if (!body.name) { toast('请输入名称'); return; }
        if (isEdit) body.id = robot.id;
        postJSON(isEdit ? 'robot/update' : 'robot/add', body).done(function (res) {
            if (res.errorCode === '000000') { closeSheet(); toast('已保存'); loadRobots(); }
            else toast(res.errorMsg || '保存失败');
        });
    });
}

function showSendSheet(robot) {
    openSheet('<div class="h5-sheet-head"><h3>发送消息 · ' + esc(robot.name) + '</h3></div>' +
        '<div class="h5-field"><label>类型</label><select class="h5-select" id="sendType"><option value="text">纯文本</option><option value="file">文件</option></select></div>' +
        '<div class="h5-field" id="sendTextWrap"><label>内容</label><textarea class="h5-textarea" id="sendText" style="font-family:inherit"></textarea></div>' +
        '<div class="h5-field" id="sendFileWrap" style="display:none"><label>选择文件</label><input class="h5-input" type="file" id="sendFile"></div>' +
        '<button class="h5-btn-primary h5-btn-block" id="sendGo">发送</button>');
    $('#sendType').on('change', function () {
        var isFile = $(this).val() === 'file';
        $('#sendTextWrap').toggle(!isFile);
        $('#sendFileWrap').toggle(isFile);
    });
    $('#sendGo').on('click', function () {
        var type = $('#sendType').val();
        var fd = new FormData();
        fd.append('robotId', robot.id);
        fd.append('type', type);
        if (type === 'text') {
            fd.append('text', $('#sendText').val());
        } else {
            var f = $('#sendFile')[0].files[0];
            if (!f) { toast('请选择文件'); return; }
            fd.append('file', f);
        }
        loading(true);
        api({ url: 'robot/send', data: fd, processData: false, contentType: false }).done(function (res) {
            loading(false);
            if (res.errorCode === '000000') {
                if (res.body && res.body.error) { toast(res.body.error); return; } // 后端"成功"但实际未发出
                closeSheet(); toast(res.body && res.body.type === 'file' ? '文件已发送' : '文本已发送');
            }
            else toast(res.errorMsg || '发送失败');
        }).fail(function () { loading(false); toast('发送失败'); });
    });
}

/* ---------------- Shell 脚本学习 ---------------- */

function renderShell($page) {
    $page.html('<div class="h5-section-title">Shell 脚本学习</div>' +
        '<div id="ssListWrap"><div id="ssList"><div class="h5-empty">加载中…</div></div></div>' +
        '<div id="ssDetailWrap" style="display:none"></div>');
    api({ url: 'shellscript/topics', type: 'GET' }).done(function (list) {
        var html = '';
        (list || []).forEach(function (t) {
            html += '<div class="h5-card" data-name="' + esc(t.name) + '" style="cursor:pointer">' +
                '<div class="h5-card-title">' + esc(t.name) + '</div>' +
                (t.category ? '<div class="h5-card-sub">' + esc(t.category) + '</div>' : '') + '</div>';
        });
        $('#ssList').html(html || '<div class="h5-empty">暂无主题</div>');
    }).fail(function () { $('#ssList').html('<div class="h5-empty">加载失败</div>'); });

    $page.off('click', '.h5-card[data-name]').on('click', '.h5-card[data-name]', function () {
        openShellTopic($(this).data('name'));
    });
}

function openShellTopic(name) {
    loading(true);
    api({ url: 'shellscript/topics/' + encodeURIComponent(name), type: 'GET' }).done(function (t) {
        loading(false);
        var html = '<div class="h5-section-title"><span class="h5-btn-ghost h5-btn-sm" id="ssBack">← 返回列表</span>' +
            '<a class="h5-btn-ghost h5-btn-sm" id="ssSaveNote" style="color:#a78bfa;text-decoration:none">📝 存入笔记</a></div>' +
            '<div class="h5-card"><div class="h5-card-title" style="font-size:17px">' + esc(t.name) + '</div>' +
            (t.category ? '<div class="h5-card-tags"><span class="h5-badge h5-badge-role">' + esc(t.category) + '</span></div>' : '') +
            '<div style="margin-top:10px;color:var(--text-2);font-size:13.5px">' + esc(t.intro || '') + '</div></div>';
        if ((t.syntaxes || []).length) {
            html += '<div class="h5-section-title">语法速查</div><div class="h5-card">';
            t.syntaxes.forEach(function (s) {
                html += '<div style="padding:8px 0;border-bottom:1px solid var(--border-1)">' +
                    '<div class="h5-li-title h5-code">' + esc(s.signature || s.name) + '</div>' +
                    (s.description ? '<div class="h5-li-sub">' + esc(s.description) + '</div>' : '') + '</div>';
            });
            html += '</div>';
        }
        if ((t.examples || []).length) {
            html += '<div class="h5-section-title">示例</div>';
            t.examples.forEach(function (ex) {
                html += '<div class="h5-card"><div class="h5-card-title">' + esc(ex.name || '') + '</div>' +
                    (ex.description ? '<div class="h5-card-sub">' + esc(ex.description) + '</div>' : '') +
                    '<div class="h5-pre" style="margin-top:8px">' + esc(ex.code || '') + '</div></div>';
            });
        }
        $('#ssListWrap').hide();
        $('#ssDetailWrap').show().html(html);
        $('#ssBack').one('click', function () { $('#ssDetailWrap').hide(); $('#ssListWrap').show(); });
        /* R2 一键沉淀：把条目完整存为私有笔记（Q2 默认 private） */
        $('#ssSaveNote').one('click', function () {
            var md = ['## 介绍', t.intro || '', '## 语法'];
            (t.syntaxes || []).forEach(function (s) {
                md.push('### ' + s.name);
                if (s.signature) md.push('```bash', s.signature, '```');
                if (s.description) md.push(s.description);
            });
            md.push('## 示例');
            (t.examples || []).forEach(function (ex) {
                md.push('### ' + ex.name);
                if (ex.description) md.push(ex.description);
                if (ex.code) md.push('```bash', ex.code, '```');
            });
            saveFromSourceH5('shell/' + t.name, t.name, t.category, md.join('\n'), function (ok, msg) {
                toast(ok ? '已存为私有笔记：' + msg : (msg || '保存失败'));
            });
        });
    }).fail(function () { loading(false); toast('加载失败'); });
}

/* ---------------- 记忆笔记（用户级 · 可编辑） ---------------- */

/** 极简 Markdown 渲染（标题/列表/代码块/行内样式） */
function mdToHtml(md) {
    if (!md) return '';
    var lines = String(md).split(/\r?\n/);
    var out = [], inCode = false, codeBuf = [], listOpen = false;
    function closeList() { if (listOpen) { out.push('</ul>'); listOpen = false; } }
    function inline(s) {
        return esc(s)
            .replace(/`([^`]+)`/g, '<code style="background:var(--glass-3);padding:1px 5px;border-radius:5px;font-family:ui-monospace,monospace">$1</code>')
            .replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>')
            .replace(/\[\[([^\[\]]+)\]\]/g, '<a href="javascript:;" class="h5-wikilink" data-wiki="$1" style="color:#a78bfa;text-decoration:none;border-bottom:1px dashed rgba(167,139,250,.5)">$1</a>')
            .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');
    }
    lines.forEach(function (line) {
        if (/^```/.test(line.trim())) {
            if (inCode) {
                out.push('<div class="h5-pre" style="margin:8px 0">' + esc(codeBuf.join('\n')) + '</div>');
                codeBuf = []; inCode = false;
            } else { closeList(); inCode = true; }
            return;
        }
        if (inCode) { codeBuf.push(line); return; }
        var h = line.match(/^(#{1,4})\s+(.*)$/);
        if (h) {
            closeList();
            var lv = h[1].length;
            out.push('<h' + (lv + 2) + ' style="margin:16px 0 6px;font-size:' + (17 - lv) + 'px;color:var(--text-1)">' + inline(h[2]) + '</h' + (lv + 2) + '>');
            return;
        }
        var li = line.match(/^\s*[-*]\s+(.*)$/);
        if (li) {
            if (!listOpen) { out.push('<ul style="padding-left:20px;margin:6px 0">'); listOpen = true; }
            out.push('<li style="color:var(--text-2);margin:3px 0">' + inline(li[1]) + '</li>');
            return;
        }
        closeList();
        if (line.trim() === '') return;
        out.push('<p style="margin:8px 0;color:var(--text-2)">' + inline(line) + '</p>');
    });
    closeList();
    if (inCode && codeBuf.length) out.push('<div class="h5-pre">' + esc(codeBuf.join('\n')) + '</div>');
    return out.join('');
}

/* ---------------- 记忆笔记（用户级 · 可编辑 · v2 个人知识库） ---------------- */

var noteScope = 'mine';
var noteSearchTimer = null;

function renderNotes($page) {
    $page.html(
        '<div class="h5-section-title">记忆笔记' +
        '<div><span class="h5-btn-ghost h5-btn-sm" id="noteTabMine">我的笔记</span>' +
        '<span class="h5-btn-ghost h5-btn-sm" id="noteTabPublic">他人公开</span></div></div>' +
        '<div class="h5-card"><div class="h5-field" style="margin:0">' +
        '<input class="h5-input" id="noteSearchInput" placeholder="🔍 搜索笔记（标题+正文）" maxlength="50"></div>' +
        '<div id="noteScopeHint" class="h5-card-sub" style="margin-top:6px;color:#e0a800;display:none">他人笔记仅支持按标题搜索（正文不入库）</div></div>' +
        '<div id="noteListWrap">' +
        '<div id="noteRecentBox" style="display:none;margin-bottom:10px"></div>' +
        '<div id="noteList"><div class="h5-empty">加载中…</div></div></div>' +
        '<div id="noteDetailWrap" style="display:none"></div>');
    $('#noteTabMine').on('click', function () { switchNoteTab('mine'); });
    $('#noteTabPublic').on('click', function () { switchNoteTab('public'); });
    $('#noteSearchInput').on('input', function () {
        clearTimeout(noteSearchTimer);
        noteSearchTimer = setTimeout(loadNotesH5, 300);
    });
    /* v2：命中标记说明 / 详情导出 / 最近浏览 / wiki 链接（事件委托，动态内容） */
    $page.off('click', '.note-item-h5').on('click', '.note-item-h5', function () {
        var ownerId = $(this).data('owner');
        openNoteH5(ownerId != null ? ownerId : H5State.userId, $(this).data('name'),
            ownerId == null || ownerId === H5State.userId);
    });
    $page.on('click', '.h5-wikilink', function () {
        var title = $(this).data('wiki');
        api({ url: 'note/resolve?name=' + encodeURIComponent(title), type: 'GET' }).done(function (res) {
            var n = res && res.body;
            if (n && n.ownerId != null) {
                openNoteH5(n.ownerId, n.name, n.ownerId === H5State.userId);
            } else {
                toast('未找到笔记「' + title + '」');
            }
        }).fail(function () { toast('解析失败'); });
    });
    $page.on('click', '.note-backlink-h5', function () {
        var ownerId = $(this).data('owner');
        openNoteH5(ownerId, $(this).data('name'), ownerId === H5State.userId);
    });
    $page.on('click', '.note-recent-item-h5', function () {
        var ownerId = $(this).data('owner');
        openNoteH5(ownerId, $(this).data('name'), ownerId === H5State.userId);
    });
    switchNoteTab('mine');
}

function switchNoteTab(mode) {
    noteScope = mode;
    $('#noteTabMine').css({ background: mode === 'mine' ? 'var(--accent)' : 'transparent', color: mode === 'mine' ? '#fff' : 'var(--text-2)' });
    $('#noteTabPublic').css({ background: mode === 'public' ? 'var(--accent)' : 'transparent', color: mode === 'public' ? '#fff' : 'var(--text-2)' });
    $('#noteScopeHint').toggle(mode === 'public');
    $('#noteDetailWrap').hide();
    $('#noteListWrap').show();
    if (mode === 'mine') {
        setFab(true, function () { openNoteEditorH5(null); });
    } else {
        setFab(false);
    }
    renderRecentH5();
    loadNotesH5();
}

function noteVisBadge(v) {
    return v === 'private'
        ? '<span class="h5-badge" style="background:rgba(255,193,7,.15);color:#e0a800">私有</span>'
        : '<span class="h5-badge" style="background:rgba(67,233,123,.15);color:#43e97b">公开</span>';
}

/** 命中标记：标题命中/正文命中/仅标题（Q1：他人正文不入库，仅标题可搜） */
function noteHitBadge(n) {
    var b = '';
    if (n.titleMatched) b = '<span class="h5-badge" style="background:rgba(67,233,123,.15);color:#43e97b">标题命中</span>';
    else if (n.contentMatched) b = '<span class="h5-badge" style="background:rgba(255,193,7,.15);color:#e0a800">正文命中</span>';
    if (n.contentSearchable === false) b += '<span class="h5-badge" style="background:rgba(255,255,255,.08);color:var(--text-9)">仅标题</span>';
    return b;
}

function noteTagsRow(tags) {
    if (!tags || !tags.length) return '';
    return '<div style="margin-top:4px">' + tags.slice(0, 4).map(function (t) {
        return '<span class="h5-badge" style="background:rgba(167,139,250,.12);color:#a78bfa;margin-right:4px">' + esc(t) + '</span>';
    }).join('') + '</div>';
}

/** 统一检索：scope=mine|public，标题+正文（服务端 Q1 权限） */
function loadNotesH5() {
    var kw = ($('#noteSearchInput').val() || '').trim();
    loading(true);
    api({ url: 'note/search?scope=' + noteScope + '&field=all' + (kw ? '&keyword=' + encodeURIComponent(kw) : ''), type: 'GET' })
    .done(function (res) {
        loading(false);
        var list = (res.errorCode === '000000' && res.body) || [];
        if (!list.length) {
            $('#noteList').html('<div class="h5-empty">' + (kw ? '未找到相关笔记' : (noteScope === 'mine' ? '暂无笔记，点击右下角 ＋ 新增' : '暂无他人公开笔记')) + '</div>');
            return;
        }
        var html = '';
        list.forEach(function (n) {
            var isMine = noteScope === 'mine' || n.ownerId === H5State.userId;
            html += '<div class="h5-card note-item-h5" data-owner="' + n.ownerId + '" data-name="' + esc(n.name) + '" style="cursor:pointer">' +
                '<div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + esc(n.name) + '</div>' +
                '<div class="h5-card-sub">' + (isMine ? esc(n.category || '未分类') : esc(n.ownerName || '') + (n.category ? ' · ' + esc(n.category) : '')) + ' · ' + esc(n.updateTime || '') + '</div>' +
                (n.preview ? '<div class="h5-card-sub" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + esc(n.preview) + '</div>' : '') +
                noteTagsRow(n.tags) + '</div>' +
                '<div>' + (isMine ? noteVisBadge(n.visibility) : '') + noteHitBadge(n) + '</div></div></div>';
        });
        $('#noteList').html(html);
    }).fail(function () { loading(false); $('#noteList').html('<div class="h5-empty">加载失败</div>'); });
}

/* ---------------- 最近浏览（R8，localStorage） ---------------- */

var RECENT_KEY_H5 = 'wxydNoteRecent'; /* 与桌面版共用 key，跨端同步体验 */

function pushRecentH5(ownerId, name) {
    try {
        var arr = [];
        try { arr = JSON.parse(localStorage.getItem(RECENT_KEY_H5)) || []; } catch (e) {}
        if (!Array.isArray(arr)) arr = [];
        arr = arr.filter(function (r) { return !(String(r.ownerId) === String(ownerId) && r.name === name); });
        arr.unshift({ ownerId: ownerId, name: name, time: Date.now() });
        localStorage.setItem(RECENT_KEY_H5, JSON.stringify(arr.slice(0, 10)));
    } catch (e) { /* 忽略存储异常 */ }
    renderRecentH5();
}

function renderRecentH5() {
    var $box = $('#noteRecentBox');
    if (!$box.length) return;
    var arr = [];
    try { arr = JSON.parse(localStorage.getItem(RECENT_KEY_H5)) || []; } catch (e) {}
    if (noteScope !== 'mine' || !Array.isArray(arr) || !arr.length) { $box.hide().empty(); return; }
    var html = '<div class="h5-card-sub" style="margin-bottom:4px">🕘 最近浏览</div>';
    arr.slice(0, 5).forEach(function (r) {
        html += '<div class="h5-card note-recent-item-h5" data-owner="' + esc(r.ownerId) + '" data-name="' + esc(r.name) + '" style="cursor:pointer;padding:8px 12px">' +
            '<div class="h5-card-title" style="font-size:13.5px">' + esc(r.name) + '</div></div>';
    });
    $box.html(html).show();
}

function openNoteH5(ownerId, name, mine) {
    loading(true);
    api({ url: 'note/get?ownerId=' + ownerId + '&name=' + encodeURIComponent(name), type: 'GET' }).done(function (res) {
        loading(false);
        if (res.errorCode !== '000000' || !res.body) { toast(res.errorMsg || '笔记不存在'); return; }
        var n = res.body;
        pushRecentH5(ownerId, name);
        /* R7 来源跳转：javaapi/xxx、shell/xxx 可在 H5 站内跳，dubbo 无资料页不渲染 */
        var sourceHtml = '';
        if (n.source) {
            var sp = String(n.source).split('/');
            var spUrl = null;
            if (sp[0] === 'shell' && sp[1]) spUrl = "javascript:goH5Module('shell')";
            else if (sp[0] === 'javaapi' && sp[1]) spUrl = "javascript:goH5Module('javaapi')";
            if (spUrl) {
                sourceHtml = '<div class="h5-card-sub" style="margin-top:6px;color:#38bdf8">参考：' +
                    esc(sp[0] === 'shell' ? 'Shell 脚本' : 'Java API') + ' / ' + esc(sp[1]) +
                    ' <a href="' + spUrl + '" style="color:#38bdf8">前往 →</a></div>';
            }
        }
        var html = '<div class="h5-section-title"><span class="h5-btn-ghost h5-btn-sm" id="noteBack">← 返回列表</span>' +
            (mine ? '<span class="h5-btn-ghost h5-btn-sm" id="noteEditBtn">编辑</span>' : '') +
            '<a class="h5-btn-ghost h5-btn-sm" style="text-decoration:none;color:#38bdf8" href="note/export?ownerId=' + ownerId + '&name=' + encodeURIComponent(name) + '" target="_blank" rel="noopener">导出</a>' +
            (mine ? '<span class="h5-btn-ghost h5-btn-sm" id="noteDelBtn" style="color:#f5576c">删除</span>' : '') + '</div>' +
            '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
            '<div class="h5-card-title" style="font-size:17px">' + esc(n.name) + '</div>' +
            '<div class="h5-card-sub">' + esc(n.ownerName || '') + (n.category ? ' · ' + esc(n.category) : '') + ' · ' + esc(n.updateTime || '') + '</div>' +
            sourceHtml + '</div>' +
            (mine ? noteVisBadge(n.visibility) : '') + '</div>' +
            (n.tags && n.tags.length ? '<div style="margin-top:8px">' + noteTagsRow(n.tags) + '</div>' : '') +
            '<div id="noteBacklinksH5"></div>' +
            '<div class="h5-md" style="margin-top:6px">' + mdToHtml(n.content) + '</div></div>';
        $('#noteListWrap').hide();
        $('#noteDetailWrap').show().html(html);
        /* R7 反向链接 */
        api({ url: 'note/backlinks?name=' + encodeURIComponent(name), type: 'GET' }).done(function (br) {
            var bl = (br && br.body) || [];
            if (bl.length) {
                var bh = '<div class="h5-card-sub" style="margin-top:8px">🔗 被 ' + bl.length + ' 篇笔记引用：</div>';
                bl.forEach(function (b) {
                    bh += '<a href="javascript:;" class="note-backlink-h5" data-owner="' + b.ownerId + '" data-name="' + esc(b.name) + '" ' +
                        'style="display:inline-block;margin:3px 8px 0 0;color:#a78bfa;font-size:13px">' + esc(b.name) + '</a>';
                });
                $('#noteBacklinksH5').html(bh);
            }
        });
        $('#noteBack').one('click', function () { $('#noteDetailWrap').hide(); $('#noteListWrap').show(); renderRecentH5(); });
        if (mine) {
            $('#noteEditBtn').one('click', function () { openNoteEditorH5(n); });
            $('#noteDelBtn').one('click', function () {
                confirm('确定删除笔记「' + n.name + '」？不可恢复。', function () {
                    postJSON('note/delete', { name: n.name }).done(function (r) {
                        if (r.errorCode === '000000') { toast('已删除'); closeDetailH5(); loadNotesH5(); }
                        else toast(r.errorMsg || '删除失败');
                    });
                });
            });
        }
    }).fail(function () { loading(false); toast('加载失败'); });
}

/** H5 站内模块跳转（来源「前往 →」用）；notes 页面栈较浅，直接回首页再进目标模块 */
function goH5Module(name) {
    try { goPage(name); } catch (e) { toast('跳转失败'); }
}

/** 一键沉淀（R2，供 shell/javaapi/dubbo H5 详情调用） */
function saveFromSourceH5(sourceKey, sourceTitle, sourceCategory, content, cb) {
    if (!content || !content.trim()) { cb && cb(false, '摘录内容为空'); return; }
    postJSON('note/saveFromSource', {
        sourceKey: sourceKey, sourceTitle: sourceTitle,
        sourceCategory: sourceCategory, content: content
    }).done(function (res) {
        if (res.errorCode === '000000') cb && cb(true, (res.body && res.body.name) || '');
        else cb && cb(false, res.errorMsg || '保存失败');
    }).fail(function () { cb && cb(false, '保存失败'); });
}

function closeDetailH5() { $('#noteDetailWrap').hide(); $('#noteListWrap').show(); }

/** 新增/编辑笔记（底部抽屉表单，v2：含标签；编辑时保留 source 断链保护） */
function openNoteEditorH5(note) {
    var isEdit = !!note;
    openSheet('<div class="h5-sheet-head"><h3>' + (isEdit ? '编辑笔记' : '新增笔记') + '</h3></div>' +
        '<div class="h5-field"><label>标题</label><input class="h5-input" id="ntTitle" maxlength="50" value="' + esc(note && note.name || '') + '"></div>' +
        '<div class="h5-field"><label>分类</label><input class="h5-input" id="ntCategory" maxlength="20" value="' + esc(note && note.category || '') + '"></div>' +
        '<div class="h5-field"><label>可见性</label><select class="h5-select" id="ntVis">' +
        '<option value="public"' + (isEdit && note.visibility === 'private' ? '' : ' selected') + '>公共</option>' +
        '<option value="private"' + (isEdit && note.visibility === 'private' ? ' selected' : '') + '>私有</option></select></div>' +
        '<div class="h5-field"><label>标签（逗号分隔）</label><input class="h5-input" id="ntTags" maxlength="200" value="' + esc(note && note.tags ? note.tags.join(', ') : '') + '"></div>' +
        '<div class="h5-field"><label>正文（markdown）</label><textarea class="h5-textarea" id="ntBody" style="min-height:200px;font-family:ui-monospace,monospace">' + esc(note && note.content || '') + '</textarea></div>' +
        '<button class="h5-btn h5-btn-block" id="ntPreviewBtn">预览</button>' +
        '<div class="h5-md" id="ntPreview" style="display:none;margin:10px 0"></div>' +
        '<button class="h5-btn-primary h5-btn-block" id="ntSave">保存</button>');
    $('#ntPreviewBtn').on('click', function () {
        var $p = $('#ntPreview');
        if ($p.is(':visible')) { $p.hide(); $('#ntBody').show(); $(this).text('预览'); }
        else { $p.html(mdToHtml($('#ntBody').val())).show(); $('#ntBody').hide(); $(this).text('编辑'); }
    });
    $('#ntSave').on('click', function () {
        var body = {
            name: $('#ntTitle').val().trim(),
            category: $('#ntCategory').val().trim(),
            visibility: $('#ntVis').val(),
            content: $('#ntBody').val(),
            tags: $('#ntTags').val().split(/[,，]/).map(function (s) { return s.trim(); })
                     .filter(function (s) { return !!s; }),
            oldName: isEdit ? note.name : ''
        };
        /* 编辑时保留来源，避免一次保存把 R3 断链 */
        if (isEdit && note.source) body.source = note.source;
        if (!body.name) { toast('请输入标题'); return; }
        loading(true);
        postJSON('note/save', body).done(function (res) {
            loading(false);
            if (res.errorCode === '000000') {
                closeSheet();
                loadNotesH5();
                openNoteH5(H5State.userId, res.body, true);
                toast('保存成功');
            } else toast(res.errorMsg || '保存失败');
        }).fail(function () { loading(false); toast('保存失败'); });
    });
}

/* ---------------- 文件共享 ---------------- */

function renderShare($page) {
    var html = '<div class="h5-section-title">文件共享<span class="h5-btn-ghost h5-btn-sm" id="shareRefresh">刷新</span></div>' +
        (H5State.isAdmin ? '<div class="h5-alert h5-alert-info">管理员可上传/删除文件，点击文件名下载</div>' : '<div class="h5-alert h5-alert-info">点击文件名下载</div>') +
        '<div id="shareList"><div class="h5-empty">加载中…</div></div>';
    $page.html(html);
    if (H5State.isAdmin) setFab(true, showShareUpload);
    loadShare();
    $page.find('#shareRefresh').on('click', loadShare);
}

function loadShare() {
    loading(true);
    api({ url: 'share/list', type: 'GET' }).done(function (res) {
        loading(false);
        var list = (res.errorCode === '000000' && res.body) || [];
        var html = '';
        if (!list.length) html = '<div class="h5-empty">暂无共享文件' + (H5State.isAdmin ? '，点击 ＋ 上传' : '') + '</div>';
        list.forEach(function (f) {
            /* 下载用真实 <a> 链接（target=_blank）：程序化 a.click() 在部分移动浏览器/WebView 会被拦截导致“没反应” */
            var dlUrl = 'share/download?name=' + encodeURIComponent(f.name);
            html += '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title" style="color:var(--accent-2)">' + esc(f.name) + '</div>' +
                '<div class="h5-card-sub">' + fmtSize(f.size) + ' · ' + fmtTime(f.lastModified) + '</div></div>' +
                '<a class="h5-btn-primary h5-btn-sm" style="text-decoration:none;display:inline-block" href="' + dlUrl + '" target="_blank" rel="noopener">下载</a>' +
                (H5State.isAdmin ? ' <button class="h5-btn-danger h5-btn-sm" data-del="' + esc(f.name) + '">删除</button>' : '') +
                '</div></div>';
        });
        $('#shareList').html(html);
    }).fail(function () { loading(false); $('#shareList').html('<div class="h5-empty">加载失败</div>'); });
}

function showShareUpload() {
    openSheet('<div class="h5-sheet-head"><h3>上传文件</h3></div>' +
        '<div class="h5-field"><label>选择文件（可多选）</label><input class="h5-input" type="file" id="shareFiles" multiple></div>' +
        '<button class="h5-btn-primary h5-btn-block" id="shareUpGo">开始上传</button>');
    $('#shareUpGo').on('click', function () {
        var files = $('#shareFiles')[0].files;
        if (!files || !files.length) { toast('请选择文件'); return; }
        var fd = new FormData();
        for (var i = 0; i < files.length; i++) fd.append('files', files[i]);
        loading(true);
        api({ url: 'share/upload', data: fd, processData: false, contentType: false }).done(function (res) {
            loading(false);
            if (res.errorCode === '000000') { closeSheet(); toast('上传成功'); loadShare(); }
            else toast(res.errorMsg || '上传失败');
        }).fail(function () { loading(false); toast('上传失败'); });
    });
}

/* ---------------- 归档下载 ---------------- */

function renderArchive($page) {
    $page.html(
        '<div class="h5-section-title">报文归档下载</div>' +
        '<div class="h5-card">' +
        '<div class="h5-field"><label>目标网址</label><input class="h5-input h5-code" id="arcUrl" value="https://kstest1.kshbank.cn:9091/extService/index.html"></div>' +
        '<div class="h5-field"><label>元素 ID（承载 Base64 的元素）</label><input class="h5-input h5-code" id="arcElementId" value="xzz"></div>' +
        '<div class="h5-field"><label>文件名（留空自动生成）</label><input class="h5-input" id="arcFileName" placeholder="主机名_时间戳.7z"></div>' +
        '<div class="h5-field"><label>超时(ms)</label><input class="h5-input" id="arcTimeout" type="number" value="60000"></div>' +
        '<div class="h5-field"><label style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--text-2)"><input type="checkbox" id="arcInsecure"> 忽略证书校验</label></div>' +
        '<button class="h5-btn-primary h5-btn-block" id="arcGo">获取并下载</button>' +
        '</div>' +
        '<div class="h5-card"><div class="h5-section-title" style="margin-top:0">执行结果</div>' +
        '<ul class="h5-steps" id="arcSteps"><li>尚未执行。点击「获取并下载」开始。</li></ul>' +
        '<div class="h5-pre" id="arcError" style="display:none"></div></div>');
    $page.find('#arcGo').on('click', doArchiveFetch);
}

function doArchiveFetch() {
    var body = {
        url: $('#arcUrl').val().trim(),
        elementId: $('#arcElementId').val().trim(),
        fileName: $('#arcFileName').val().trim(),
        timeoutMs: parseInt($('#arcTimeout').val(), 10) || 60000,
        insecureTls: $('#arcInsecure').is(':checked')
    };
    if (!body.url) { toast('请输入目标网址'); return; }
    loading(true);
    var steps = $('#arcSteps').html('<li class="active">抓取页面并解析…</li>');
    $('#arcError').hide();
    fetch('archive/fetch.do', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
        credentials: 'same-origin',
        body: JSON.stringify(body)
    }).then(function (resp) {
        loading(false);
        var ct = resp.headers.get('Content-Type') || '';
        if (ct.indexOf('application/json') >= 0) {
            return resp.json().then(function (err) {
                steps.html('<li class="err">失败 [' + esc(err.stage || '未知阶段') + ']：' + esc(err.message || err.error || '未知错误') + '</li>');
            });
        }
        var disp = resp.headers.get('Content-Disposition') || '';
        var name = 'archive.7z';
        if (disp.indexOf("filename*=UTF-8''") >= 0) {
            try { name = decodeURIComponent(disp.split("filename*=UTF-8''")[1]); } catch (e) { }
        }
        var elapsed = resp.headers.get('X-Archive-Elapsed-Ms') || '';
        steps.html('<li class="done">抓取完成，开始下载…</li>');
        return resp.blob().then(function (blob) {
            var a = document.createElement('a');
            a.href = URL.createObjectURL(blob);
            a.download = name;
            document.body.appendChild(a);
            a.click();
            a.remove();
            steps.html('<li class="done">已下载：' + esc(name) + '（' + fmtSize(blob.size) + (elapsed ? ' · ' + esc(elapsed) + 'ms' : '') + '）</li>');
        });
    }).catch(function (e) {
        loading(false);
        steps.html('<li class="err">网络错误：' + esc(String(e)) + '</li>');
    });
}

/* ---------------- 页面渲染注册表 ---------------- */

var PAGE_RENDER = {
    home: renderHome,
    message: renderMessage,
    robot: renderRobot,
    profile: renderProfile,
    user: renderUser,
    shell: renderShell,
    notes: renderNotes,
    share: renderShare,
    archive: renderArchive
    /* javaapi / sql / dubbo 由对应 h5-*.js 通过 registerH5Module 注册 */
};

/* ---------------- 事件委托（动态内容） ---------------- */

function bindDelegates() {
    /* 用户管理操作（按钮含 h5-btn-primary/h5-btn-danger，不能只用 .h5-btn 匹配） */
    $('#page-user').on('click', '#usrList [data-act]', function () {
        var act = $(this).data('act'), id = $(this).data('id');
        if (act === 'pause') {
            confirm('确定暂停该用户？暂停后该用户将无法登录。', function () { doUserAction(act, id); });
        } else if (act === 'del') {
            confirm('确定删除该用户？其关联的钉钉机器人也会一并删除，且不可恢复。', function () { doUserAction(act, id); });
        } else {
            doUserAction(act, id);
        }
    });
    /* 留言删除（后端校验：管理员任意 / 本人自己的） */
    $('#page-message').on('click', '#msgList .h5-btn-danger', function () {
        var id = $(this).data('id');
        confirm('删除该留言？', function () {
            postJSON('delMessage', { msgId: String(id) }).done(function (res) {
                if (res.errorCode === '000000') { toast('已删除'); loadMessages(msgFlag); }
                else toast(res.errorMsg || '删除失败');
            });
        });
    });
    /* 机器人操作（发送/编辑/删除按钮同上，用 [data-act] 委托） */
    $('#page-robot').on('click', '#robotList [data-act]', function () {
        var act = $(this).data('act'), id = $(this).data('id');
        var robot = (robotCache || []).find(function (r) { return r.id == id; });
        if (act === 'send') { if (robot) showSendSheet(robot); }
        else if (act === 'edit') { showRobotForm(robot); }
        else if (act === 'del') {
            confirm('确定删除该机器人？', function () {
                postJSON('robot/delete', { id: Number(id) }).done(function (res) {
                    if (res.errorCode === '000000') { toast('已删除'); loadRobots(); }
                    else toast(res.errorMsg || '删除失败');
                });
            });
        }
    });
    /* 文件共享：下载改为真实 <a> 链接直接导航，无需委托；删除仍走确认 */
    $('#page-share').on('click', '[data-del]', function () {
        var name = $(this).data('del');
        confirm('确定删除文件「' + name + '」？', function () {
            postJSON('share/delete', { name: name }).done(function (res) {
                if (res.errorCode === '000000') { toast('已删除'); loadShare(); }
                else toast(res.errorMsg || '删除失败');
            });
        });
    });
}

/* ---------------- 启动 ---------------- */

$(function () {
    /* 全局 401：会话过期 → 弹出登录 */
    $(document).ajaxError(function (ev, jqXHR) {
        if (jqXHR && jqXHR.status === 401) {
            H5State.username = ''; H5State.isAdmin = false;
            /* 带分享链接打开时进访客模式（免登录看该帖），否则弹登录 */
            if (getShareWelfareId() && window.H5Welfare) enterGuestShare();
            else showLogin();
        }
    });

    applyTheme(localStorage.getItem('wxyd-theme') || 'dark');

    $('#h5Tabbar').on('click', '.h5-tab', function () { goPage($(this).data('target')); });
    $('#h5ThemeBtn').on('click', openThemeSheet);
    $('#h5LogoutBtn').on('click', function () {
        if (H5State.username) confirm('确定退出登录？', doLogout);
        else showLogin();
    });
    $('#h5LoginBtn').on('click', doLogin);
    $('#h5LoginPwd').on('keydown', function (e) { if (e.key === 'Enter') doLogin(); });

    $('#h5DialogMask').on('click', function (e) { if (e.target === this) closeDialog(); });
    $('#h5SheetMask').on('click', closeSheet);
    $('#h5Sheet').on('click', '.h5-sheet-close', closeSheet);

    /* 输入框聚焦时滚动到可视区中央，避免被手机键盘遮挡 */
    $(document).on('focusin', '.h5-main textarea, .h5-main input', function () {
        var el = this;
        setTimeout(function () {
            if (el && el.scrollIntoView) el.scrollIntoView({ block: 'center', behavior: 'smooth' });
        }, 300);
    });

    bindDelegates();
    bootstrap();
});
