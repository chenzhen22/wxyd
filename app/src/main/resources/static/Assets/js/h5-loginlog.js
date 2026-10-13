/* ============================================================
   登录日志（H5 移动端）
   数据：/api/loginLog/list?username=（仅管理员过滤生效）
   注册：registerH5Module('loginlog', renderLoginLog)
   依赖：h5.js 的 esc / api / toast；H5State.isAdmin
   管理员看全部（可按用户名搜索），普通用户只看自己的记录
   ============================================================ */
(function () {

    var EVENT_TXT = { login_success: '登录成功', login_fail: '登录失败', logout: '登出' };
    var EVENT_COLOR = { login_success: '#43e97b', login_fail: '#f5576c', logout: '#e0a800' };
    var CHANNEL_TXT = { password: '密码', github: 'GitHub', gitee: 'Gitee', email: '邮箱' };

    function eventBadge(e) {
        var known = EVENT_TXT[e] ? e : 'login_fail';
        return '<span class="h5-badge" style="background:' + EVENT_COLOR[known] + '22;color:' + EVENT_COLOR[known] + '">' +
            esc(EVENT_TXT[known] || e) + '</span>';
    }

    function renderLoginLog($page) {
        $page.html(
            '<div class="h5-section-title">📜 登录日志' +
            '<div><span class="h5-btn-ghost h5-btn-sm" id="h5LlRefresh">↻ 刷新</span></div></div>' +
            '<div style="display:flex;gap:6px;margin-bottom:8px">' +
            '<select class="h5-select" id="h5LlEvent" style="width:auto;flex:1">' +
            '<option value="">全部状态</option>' +
            '<option value="login_success">登录成功</option>' +
            '<option value="login_fail">登录失败</option>' +
            '<option value="logout">登出</option>' +
            '</select></div>' +
            '<div id="h5LlSearchWrap" style="display:none;margin-bottom:8px">' +
            '<input class="h5-input" id="h5LlKeyword" placeholder="按用户名/邮箱搜索" style="flex:1">' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5LlSearch">搜索</span>' +
            '</div>' +
            '<div id="h5LlList"><div class="h5-empty">加载中…</div></div>'
        );
        /* 管理员可搜索全部用户；普通用户服务端已限定只返回自己的记录 */
        if (window.H5State && H5State.isAdmin) $('#h5LlSearchWrap').show();
        loadList('');

        $page.off('click').on('click', '#h5LlRefresh', function () {
            loadList($('#h5LlKeyword').val() || '');
        }).on('click', '#h5LlSearch', function () {
            loadList($('#h5LlKeyword').val() || '');
        }).on('keydown', '#h5LlKeyword', function (e) {
            if (e.key === 'Enter') loadList($('#h5LlKeyword').val() || '');
        }).off('change', '#h5LlEvent').on('change', '#h5LlEvent', function () {
            loadList($('#h5LlKeyword').val() || '');
        });
    }

    function loadList(keyword) {
        var params = [];
        if (keyword) params.push('username=' + encodeURIComponent(keyword));
        var ev = $('#h5LlEvent').val();
        if (ev) params.push('event=' + encodeURIComponent(ev));
        var qs = params.length ? ('?' + params.join('&')) : '';
        api({ url: 'loginLog/list' + qs, type: 'GET' }).done(function (res) {
            var list = (res.errorCode === '000000' && res.body) || [];
            if (!list.length) {
                $('#h5LlList').html('<div class="h5-empty">暂无登录记录</div>');
                return;
            }
            $('#h5LlList').html(list.map(function (l) {
                return '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
                    '<div class="h5-card-title">' + esc(l.username) +
                    ' <span style="font-weight:400;color:var(--text-6)">(' + esc(CHANNEL_TXT[l.channel] || l.channel) + ')</span></div>' +
                    '<div class="h5-card-sub">' + esc(l.createTime || '') +
                    (l.ip ? ' · IP ' + esc(l.ip) : '') + '</div>' +
                    (l.detail ? '<div class="h5-card-sub">' + esc(l.detail) + '</div>' : '') +
                    '</div>' + eventBadge(l.event) + '</div></div>';
            }).join('') +
            '<div class="h5-empty" style="font-size:12px">仅显示最近 200 条，日志保留 90 天</div>');
        }).fail(function () {
            $('#h5LlList').html('<div class="h5-empty">加载失败</div>');
        });
    }

    window.registerH5Module('loginlog', renderLoginLog);
})();
