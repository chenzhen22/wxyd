/* ============================================================
   福利专区（PC 桌面版）
   数据：/api/welfare/list、/api/welfare/detail（只读 dataScan 抓取的 scan_post）
   纯展示：卡片=首图+标题+摘要+时间；详情=标题+时间+图文正文。
   Markdown 渲染依赖 Assets/js/welfare-md.js（window.WelfareMd）。
   ============================================================ */
(function () {

    var PAGE_SIZE = 20;
    var state = { page: 0, loading: false, done: false, observer: null, keyword: '' };

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function msg(m) {
        if (typeof window.alterModal === 'function') window.alterModal(m);
        else alert(m);
    }

    function showList() {
        $('#welfareDetail').hide();
        $('#welfareList').show();
    }

    function showDetail(id) {
        $('#welfareArticle').html('<div class="welfare-empty">加载中…</div>');
        $('#welfareList').hide();
        $('#welfareDetail').show();
        $.getJSON('welfare/detail', { id: id }).done(function (res) {
            if (res.errorCode !== '000000' || !res.body) {
                $('#welfareArticle').html('<div class="welfare-empty">' + esc(res.errorMsg || '加载失败') + '</div>');
                return;
            }
            var b = res.body;
            var urlMap = {};
            (b.images || []).forEach(function (img) {
                if (img.url && img.webUrl) urlMap[img.url] = img.webUrl;
            });
            var html = '<h1 class="wf-title">' + esc(b.title) + '</h1>' +
                '<div class="wf-meta">' + esc(b.publishedAt || '') + '</div>' +
                '<div class="wf-body">' + window.WelfareMd.render(b.contentMd, urlMap) + '</div>';
            $('#welfareArticle').html(html);
        }).fail(function () {
            $('#welfareArticle').html('<div class="welfare-empty">加载失败，请稍后重试</div>');
        });
    }

    function cardHtml(p) {
        return '<div class="welfare-card" data-id="' + p.id + '">' +
            (p.firstImage
                ? '<div class="welfare-card-img"><img src="' + esc(p.firstImage) + '" loading="lazy" alt=""></div>'
                : '<div class="welfare-card-img welfare-card-noimg">🎁</div>') +
            '<div class="welfare-card-title">' + esc(p.title) + '</div>' +
            '<div class="welfare-card-summary">' + esc(p.summary || '') + '</div>' +
            '<div class="welfare-card-time">' + esc(p.publishedAt || '') + '</div>' +
            '</div>';
    }

    function loadMore() {
        if (state.loading || state.done) return;
        state.loading = true;
        $('#welfareSentinel .welfare-loading').show();
        $.getJSON('welfare/list', { page: state.page + 1, size: PAGE_SIZE, keyword: state.keyword || undefined }).done(function (res) {
            var list = (res.errorCode === '000000' && res.body) || [];
            state.page += 1;
            if (list.length < PAGE_SIZE) state.done = true;
            if (state.page === 1 && !list.length) $('#welfareEmpty').show();
            var html = '';
            list.forEach(function (p) { html += cardHtml(p); });
            $('#welfareGrid').append(html);
        }).fail(function () {
            state.done = true;
        }).always(function () {
            state.loading = false;
            $('#welfareSentinel .welfare-loading').toggle(!state.done);
            if (state.done) {
                if (state.observer) { state.observer.disconnect(); state.observer = null; }
                $('#welfareSentinel').hide();
            }
        });
    }

    function reset() {
        state.page = 0;
        state.loading = false;
        state.done = false;
        $('#welfareGrid').empty();
        $('#welfareEmpty').hide();
        $('#welfareSentinel').show();
        showList();
        loadMore();
        // 滚动到底自动加载：观察哨兵元素
        if (state.observer) state.observer.disconnect();
        if (window.IntersectionObserver) {
            state.observer = new IntersectionObserver(function (entries) {
                if (entries[0].isIntersecting) loadMore();
            }, { rootMargin: '400px' });
            state.observer.observe($('#welfareSentinel')[0]);
        }
    }

    /* ================= 羊毛雷达（订阅关键词 → 钉钉/邮件推送） ================= */

    var radarLoaded = false;

    function postJSON(url, body) {
        return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: body }) });
    }

    function robotOptionsHtml(selected) {
        return $.getJSON('robot/list').then(function (res) {
            var list = (res && res.body) || [];
            return list.map(function (r) {
                return '<option value="' + r.id + '"' + (r.id === selected ? ' selected' : '') + '>' +
                    esc(r.name || ('机器人 #' + r.id)) + '</option>';
            }).join('');
        }).then(function (html) { return html || '<option value="">（尚无机器人，请先到「钉钉机器人」添加）</option>'; });
    }

    function renderRadar() {
        $.getJSON('radar/list').done(function (res) {
            var list = (res && res.body) || [];
            var html = '<div class="welfare-empty" style="text-align:left;padding:10px 4px">' +
                '<b>📡 羊毛雷达</b> —— 关键词命中新帖时自动推送到你的钉钉/邮箱（每 5 分钟检查一次，只推新帖，最多 10 条）</div>';
            if (list.length) {
                html += list.map(function (s) {
                    var ch = s.channel === 'dingtalk' ? '钉钉' : '邮箱';
                    var target = s.channel === 'dingtalk' ? ('机器人#' + s.robotId) : s.email;
                    return '<div class="welfare-card" style="cursor:default;display:flex;align-items:center;gap:8px">' +
                        '<div style="flex:1;min-width:0">' +
                        '<b>🔍 ' + esc(s.keyword) + '</b> <span class="welfare-date">→ ' + ch + ' · ' + esc(target) + '</span>' +
                        '</div>' +
                        '<button class="btn-main" data-radar="test" data-id="' + s.id + '" style="padding:3px 10px;font-size:12px;">测试</button>' +
                        '<button class="btn-main" data-radar="toggle" data-id="' + s.id + '" data-on="' + s.enabled + '" style="padding:3px 10px;font-size:12px;">' + (s.enabled ? '暂停' : '启用') + '</button>' +
                        '<button class="btn-main" data-radar="del" data-id="' + s.id + '" style="padding:3px 10px;font-size:12px;color:#f5576c;">删除</button>' +
                        '</div>';
                }).join('');
            } else {
                html += '<div class="welfare-empty">还没有订阅，添加一条关键词试试</div>';
            }
            html += '<div class="welfare-card" style="cursor:default">' +
                '<div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">' +
                '<input id="radarKw" class="note-editor-input" style="flex:2;min-width:140px;" placeholder="关键词，如：VPS / 吃喝">' +
                '<select id="radarCh" class="note-editor-input" style="flex:1;min-width:110px;">' +
                '<option value="dingtalk">钉钉推送</option><option value="email">邮件推送</option></select>' +
                '<select id="radarRobot" class="note-editor-input" style="flex:1;min-width:120px;display:none"></select>' +
                '<input id="radarEmail" class="note-editor-input" style="flex:1;min-width:140px;display:none" placeholder="接收邮箱">' +
                '<button class="btn-main" id="radarAdd" style="padding:6px 16px;">＋ 添加</button>' +
                '</div></div>';
            $('#radarPanel').html(html);
            bindRadar();
        });
    }

    function bindRadar() {
        $('#radarCh').off('change').on('change', function () {
            syncRadarChannelFields();
        });
        robotOptionsHtml(null).then(function (html) {
            $('#radarRobot').html(html);
            syncRadarChannelFields(); // 初始渲染也要按当前通道显隐（修复：默认钉钉但机器人下拉藏着）
        });
        $('#radarPanel').off('click', '[data-radar]').on('click', '[data-radar]', function () {
            var op = $(this).data('radar');
            var id = $(this).data('id');
            if (op === 'del') {
                postJSON('radar/delete', { id: id }).done(function (r) { r.errorCode === '000000' ? renderRadar() : msg(r.errorMsg || '删除失败'); });
            } else if (op === 'toggle') {
                postJSON('radar/toggle', { id: id, enabled: $(this).data('on') ? false : true }).done(function (r) { r.errorCode === '000000' ? renderRadar() : msg(r.errorMsg || '操作失败'); });
            } else if (op === 'test') {
                msg('测试推送中…');
                postJSON('radar/test', { id: id }).done(function (r) { r.errorCode === '000000' ? msg(r.body) : msg(r.errorMsg || '测试失败'); });
            }
        });
        $('#radarAdd').off('click').on('click', function () {
            var kw = $('#radarKw').val().trim();
            if (!kw) { msg('请输入关键词'); return; }
            var payload = { keyword: kw, channel: $('#radarCh').val() };
            if (payload.channel === 'dingtalk') payload.robotId = $('#radarRobot').val();
            else payload.email = $('#radarEmail').val().trim();
            postJSON('radar/add', payload).done(function (r) {
                if (r.errorCode === '000000') { renderRadar(); }
                else msg(r.errorMsg || '添加失败');
            });
        });
    }

    /** 按当前通道显隐机器人/邮箱字段（初次渲染与切换共用，修复初始隐藏 bug） */
    function syncRadarChannelFields() {
        var isDing = $('#radarCh').val() === 'dingtalk';
        $('#radarRobot').toggle(isDing);
        $('#radarEmail').toggle(!isDing);
    }

    window.Welfare = {
        init: function () {
            if (!$('#welfareGrid').length) return;
            reset();
            // 事件委托（卡片动态生成）
            $('#welfareGrid').off('click').on('click', '.welfare-card', function () {
                showDetail($(this).data('id'));
            });
            $('#welfareBackBtn').off('click').on('click', showList);
            // 关键词模糊搜索（300ms 防抖，命中标题/正文）
            var searchTimer = null;
            $('#welfareSearch').off('input').on('input', function () {
                clearTimeout(searchTimer);
                var kw = $(this).val().trim();
                searchTimer = setTimeout(function () {
                    if (kw === state.keyword) return;
                    state.keyword = kw;
                    reset();
                }, 300);
            });
            // 羊毛雷达入口（登录用户可见；未登录时整个 welfare 模块本就不可达）
            $('#radarEntry').show();
            $('#radarEntry').off('click').on('click', function () {
                var $p = $('#radarPanel');
                if ($p.is(':visible')) { $p.hide(); return; }
                $p.show().html('<div class="welfare-empty">加载中…</div>');
                renderRadar();
            });
        }
    };
})();
