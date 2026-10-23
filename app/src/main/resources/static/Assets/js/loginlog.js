/**
 * 登录日志前端逻辑（PC）。
 * 管理员可查全部并按用户名/邮箱搜索，普通用户服务端限定仅本人记录；
 * 支持按事件类型筛选（登录成功 / 登录失败 / 登出）。
 * 后端：loginLog/list?event=&username=&pageNum=&pageSize=（Result 包裹分页结果，
 * body = { total, list, pageNum, pageSize }）。
 */
var LoginLog = (function () {
    var loaded = false;
    var isAdmin = false;
    var PAGE_SIZE = 20;
    var pageNum = 1;
    var total = 0;

    var EVENT_TXT = { login_success: '登录成功', login_fail: '登录失败', logout: '登出' };
    var EVENT_COLOR = { login_success: '#43e97b', login_fail: '#f5576c', logout: '#e0a800' };
    var CHANNEL_TXT = { password: '密码', github: 'GitHub', gitee: 'Gitee', email: '邮箱' };

    function esc(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function eventBadge(e) {
        var txt = EVENT_TXT[e] || e;
        var color = EVENT_COLOR[e] || '#888';
        return '<span style="background:' + color + '22;color:' + color + ';padding:2px 8px;border-radius:10px;font-size:12px;">' + esc(txt) + '</span>';
    }

    function loadList() {
        var params = [];
        var ev = $('#llEvent').val();
        if (ev) params.push('event=' + encodeURIComponent(ev));
        if (isAdmin) {
            var kw = $('#llKeyword').val().trim();
            if (kw) params.push('username=' + encodeURIComponent(kw));
        }
        params.push('pageNum=' + pageNum);
        params.push('pageSize=' + PAGE_SIZE);
        var qs = params.length ? ('?' + params.join('&')) : '';
        $.getJSON('loginLog/list' + qs, function (res) {
            var body = (res && res.body) || {};
            var list = body.list || [];
            total = body.total || 0;
            pageNum = body.pageNum || pageNum;
            if (!list.length) {
                $('#llTableBody').html('<tr><td colspan="99" style="text-align:center;color:#999;">暂无登录记录</td></tr>');
                renderPageInfo();
                return;
            }
            var startNo = (pageNum - 1) * PAGE_SIZE;
            var html = '';
            for (var i = 0; i < list.length; i++) {
                var l = list[i];
                html += '<tr><td>' + (startNo + i + 1) + '</td><td>' + esc(l.username) + '</td><td>' + eventBadge(l.event) +
                    '</td><td>' + esc(CHANNEL_TXT[l.channel] || l.channel) + '</td><td>' + esc(l.ip || '—') +
                    '</td><td>' + esc(l.createTime || '') + '</td><td>' + esc(l.detail || '') + '</td></tr>';
            }
            html += '<tr><td colspan="99" style="text-align:center;color:#999;font-size:12px;">日志保留 90 天</td></tr>';
            $('#llTableBody').html(html);
            renderPageInfo();
        }).fail(function () {
            $('#llTableBody').html('<tr><td colspan="99" style="text-align:center;color:#999;">加载失败</td></tr>');
        });
    }

    function renderPageInfo() {
        var pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
        $('#llPageInfo').text('第 ' + pageNum + ' / ' + pages + ' 页 · 共 ' + total + ' 条');
        $('#llPrev').prop('disabled', pageNum <= 1);
        $('#llNext').prop('disabled', pageNum >= pages);
    }

    function init() {
        if (loaded || !$('#llTableBody').length) return;
        loaded = true;
        pageNum = 1;
        loadList();
        // 是否管理员决定是否展示用户名搜索框（服务端同样做了权限限定）；
        // 列表加载不依赖此请求，失败也不影响展示
        $.getJSON('user/info', function (res) {
            var info = (res && res.body) || {};
            isAdmin = !!info.isAdmin;
            $('#llKeyword').toggle(isAdmin);
        });

        $('#llEvent').off('change').on('change', function () { pageNum = 1; loadList(); });
        $('#llSearch').off('click').on('click', function () { pageNum = 1; loadList(); });
        $('#llRefresh').off('click').on('click', function () { pageNum = 1; loadList(); });
        $('#llKeyword').off('keydown').on('keydown', function (e) {
            if (e.key === 'Enter') { pageNum = 1; loadList(); }
        });
        $('#llPrev').off('click').on('click', function () {
            if (pageNum > 1) { pageNum--; loadList(); }
        });
        $('#llNext').off('click').on('click', function () {
            if (pageNum * PAGE_SIZE < total) { pageNum++; loadList(); }
        });
    }

    return { init: init };
})();
