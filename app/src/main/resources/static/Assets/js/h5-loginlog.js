/* ============================================================
   登录日志（H5 移动端）
   数据：/api/loginLog/list?username=&event=&pageNum=&pageSize=
   分页：每次上拉多拉 10 条（pageSize=10），滚到底自动加载更多
   注册：registerH5Module('loginlog', renderLoginLog)
   依赖：h5.js 的 esc / api / toast；H5State.isAdmin
   管理员看全部（可按用户名搜索），普通用户只看自己的记录
   ============================================================ */
(function () {

    var PAGE_SIZE = 10;
    var state = { page: 0, loading: false, done: false, keyword: '' };

    var EVENT_TXT = { login_success: '登录成功', login_fail: '登录失败', logout: '登出' };
    var EVENT_COLOR = { login_success: '#43e97b', login_fail: '#f5576c', logout: '#e0a800' };
    var CHANNEL_TXT = { password: '密码', github: 'GitHub', gitee: 'Gitee', email: '邮箱' };

    function eventBadge(e) {
        var known = EVENT_TXT[e] ? e : 'login_fail';
        return '<span class="h5-badge" style="background:' + EVENT_COLOR[known] + '22;color:' + EVENT_COLOR[known] + '">' +
            esc(EVENT_TXT[known] || e) + '</span>';
    }

    function cardHtml(l) {
        return '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
            '<div class="h5-card-title">' + esc(l.username) +
            ' <span style="font-weight:400;color:var(--text-6)">(' + esc(CHANNEL_TXT[l.channel] || l.channel) + ')</span></div>' +
            '<div class="h5-card-sub">' + esc(l.createTime || '') +
            (l.ip ? ' · IP ' + esc(l.ip) : '') + '</div>' +
            (l.detail ? '<div class="h5-card-sub">' + esc(l.detail) + '</div>' : '') +
            '</div>' + eventBadge(l.event) + '</div></div>';
    }

    function loadMore($page) {
        if (state.loading || state.done) return;
        state.loading = true;
        $('#h5LlLoading').show();
        var params = [];
        if (state.keyword) params.push('username=' + encodeURIComponent(state.keyword));
        var ev = $('#h5LlEvent').val();
        if (ev) params.push('event=' + encodeURIComponent(ev));
        params.push('pageNum=' + (state.page + 1));
        params.push('pageSize=' + PAGE_SIZE);
        api({ url: 'loginLog/list?' + params.join('&'), type: 'GET' }).done(function (res) {
            var list = (res.errorCode === '000000' && res.body && res.body.list) || [];
            state.page += 1;
            if (list.length < PAGE_SIZE) state.done = true;
            if (state.page === 1 && !list.length) {
                $('#h5LlEmpty').show();
                $('#h5LlLoading').hide();
                return;
            }
            $('#h5LlList').append(list.map(cardHtml).join(''));
        }).fail(function () {
            state.done = true;
            toast('加载失败');
        }).always(function () {
            state.loading = false;
            $('#h5LlLoading').toggle(!state.done);
        });
    }

    function reset($page) {
        state.page = 0; state.loading = false; state.done = false;
        $('#h5LlList').empty();
        $('#h5LlEmpty').hide();
        $('#h5LlLoading').show();
        loadMore($page);
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
            '<div id="h5LlList"></div>' +
            '<div class="h5-empty" id="h5LlEmpty" style="display:none">暂无登录记录</div>' +
            '<div class="h5-empty" id="h5LlLoading" style="display:none">加载中…</div>'
        );
        /* 管理员可搜索全部用户；普通用户服务端已限定只返回自己的记录 */
        if (window.H5State && H5State.isAdmin) $('#h5LlSearchWrap').show();
        reset($page);

        /* 事件筛选：直接绑定在 select 上（不经委托），change 即重查 */
        $('#h5LlEvent').off('change').on('change', function () { reset($page); });
        $page.off('click').on('click', '#h5LlRefresh', function () { reset($page); })
            .on('click', '#h5LlSearch', function () {
                state.keyword = $('#h5LlKeyword').val().trim();
                reset($page);
            })
            .on('keydown', '#h5LlKeyword', function (e) {
                if (e.key === 'Enter') { state.keyword = $('#h5LlKeyword').val().trim(); reset($page); }
            });
        /* .h5-main 是滚动容器：滚到底部自动加载（仅当本页可见时） */
        $('.h5-main').off('scroll.loginlog').on('scroll.loginlog', function () {
            if (!$page.hasClass('active') || state.loading || state.done) return;
            if (this.scrollHeight - this.scrollTop - this.clientHeight < 300) loadMore($page);
        });
    }

    window.registerH5Module('loginlog', renderLoginLog);
})();
