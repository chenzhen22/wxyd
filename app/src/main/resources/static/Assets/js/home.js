/**
 * PC 工作台首页：问候、KPI、快捷入口、最新金融日报、账号与状态。
 * 数据源：user/info、menu/list、gateway/keys、dayinfo/latest（管理员额外：用量汇总、批次任务）。
 */
var Home = (function () {

    var inited = false;
    var lastLoad = 0;

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function num(n) {
        n = Number(n) || 0;
        return n >= 10000 ? (n / 1000).toFixed(1) + 'k' : String(n);
    }

    function renderHeader() {
        var d = new Date();
        var week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
        function p(n) { return n < 10 ? '0' + n : n; }
        $('#homeDate').text(d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
            + ' 星期' + week + ' · ' + p(d.getHours()) + ':' + p(d.getMinutes()));
        var h = d.getHours();
        var word = h < 6 ? '凌晨好' : h < 12 ? '早上好' : h < 14 ? '中午好' : h < 19 ? '下午好' : '晚上好';
        $('#homeGreeting').html(word + '，<span id="homeUser">朋友</span> <small>小泽助手工作台</small>');
        $.get('user/info', function (res) {
            if (res && res.errorCode === '000000' && res.body) {
                $('#homeUser').text(res.body.username || '朋友');
                if (!res.body.isAdmin) {
                    $('#homeReqCard, #homeBatchCard').hide();
                }
            }
        }, 'json');
    }

    function loadLinks() {
        $.get('menu/list?platform=desktop', function (res) {
            var list = (res && res.body) || [];
            var $l = $('#homeLinks').html('');
            if (!list.length) {
                $l.html('<span class="home-empty">暂无可用菜单</span>');
                return;
            }
            list.forEach(function (m) {
                $('<div class="home-link"/>')
                    .attr('data-go', m.key)
                    .append('<i>' + esc(m.icon || '•') + '</i><span>' + esc(m.name) + '</span>')
                    .appendTo($l);
            });
        }, 'json').fail(function () {
            $('#homeLinks').html('<span class="home-empty">菜单加载失败</span>');
        });
    }

    function loadKeys() {
        $.get('gateway/keys', function (res) {
            if (!res || res.errorCode !== '000000') {
                $('#homeKeyCount').text('-');
                $('#homeKeyHint').text('暂无数据');
                return;
            }
            var list = (res.body && res.body.list) || [];
            var active = list.filter(function (k) { return k.enabled; }).length;
            $('#homeKeyCount').text(list.length);
            $('#homeKeyHint').text('今日可用 ' + active + ' 把');
        }, 'json');
    }

    function loadDayinfo() {
        $.get('dayinfo/latest?type=finance', function (res) {
            var d = res && res.errorCode === '000000' ? res.body : null;
            if (!d) {
                $('#homeDayDate').text('-');
                $('#homeDayTitle').text('暂无日报');
                $('#homeDayinfo').html('<span class="home-empty">暂无日报，等外部推送入库后即可查看</span>');
                return;
            }
            var date = d.infoDate || d.date || '';
            $('#homeDayDate').text(date || '-');
            $('#homeDayTitle').text(d.title || '—');
            var brief = d.brief || d.content || '';
            brief = String(brief).replace(/[#*>`]/g, '').replace(/\n+/g, ' ').slice(0, 180);
            $('#homeDayinfo').html('<h4>' + esc(d.title || '金融日报') + '</h4>' +
                '<div style="font-size:12px;color:var(--text-5);margin-bottom:6px;">' + esc(date) + '</div>' +
                '<div class="brief">' + esc(brief || '（本期无摘要）') + '</div>');
        }, 'json');
    }

    function loadStatus() {
        var rows = [];
        $.get('user/info', function (res) {
            if (res && res.errorCode === '000000' && res.body) {
                rows.push('<div class="row"><span>登录账号</span><b>' + esc(res.body.username || '-') + '</b></div>');
                rows.push('<div class="row"><span>角色</span><b>' + (res.body.isAdmin ? '管理员' : '普通用户') + '</b></div>');
            }
            $.get('gateway/keys', function (r2) {
                if (r2 && r2.errorCode === '000000') {
                    var list = (r2.body && r2.body.list) || [];
                    var active = list.filter(function (k) { return k.enabled; }).length;
                    rows.push('<div class="row"><span>模型 Key</span><b class="' + (active ? 'ok' : 'warn') + '">' +
                        (list.length ? active + ' / ' + list.length + ' 可用' : '未创建') + '</b></div>');
                }
                rows.push('<div class="row"><span>页面更新</span><b>' + new Date().toLocaleTimeString('zh-CN', { hour12: false }) + '</b></div>');
                $('#homeStatus').html(rows.join(''));
            }, 'json');
        }, 'json');
    }

    function loadAdminWidgets() {
        $.get('gateway/admin/usage/summary', function (res) {
            if (!res || res.errorCode !== '000000') { $('#homeReqCard').hide(); return; }
            var t = (res.body && res.body.today) || {};
            $('#homeReqToday').text(t.count || 0);
            $('#homeReqHint').text('输入 ' + num(t.inputTokens) + ' · 输出 ' + num(t.outputTokens));
        }, 'json').fail(function () { $('#homeReqCard').hide(); });

        $.get('batch/list', function (res) {
            if (!res || res.errorCode !== '000000') { $('#homeBatchCard').hide(); return; }
            var list = (res.body && res.body.list) || [];
            var now = new Date();
            function p2(n) { return n < 10 ? '0' + n : n; }
            var day = now.getFullYear() + '-' + p2(now.getMonth() + 1) + '-' + p2(now.getDate());
            var today = list.filter(function (t) {
                return t.lastRunTime && t.lastRunTime.slice(0, 10) === day;
            }).length;
            $('#homeBatchCount').text(list.length);
            $('#homeBatchHint').text('今日已执行 ' + today + ' 个任务');
        }, 'json').fail(function () { $('#homeBatchCard').hide(); });
    }

    function bind() {
        $('#welcome').on('click', '[data-go]', function () {
            var key = $(this).attr('data-go');
            if (key) location.hash = '#/' + key;
        });
    }

    function init() {
        if (Date.now() - lastLoad < 1000) return; // 首屏 DOM ready 与路由各触发一次，避免重复请求
        lastLoad = Date.now();
        if (!inited) { inited = true; bind(); }
        renderHeader();
        loadLinks();
        loadKeys();
        loadDayinfo();
        loadStatus();
        loadAdminWidgets();
    }

    return { init: init };
})();

$(function () {
    if ($('#welcome').hasClass('active')) Home.init();
});
