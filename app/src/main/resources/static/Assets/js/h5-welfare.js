/* ============================================================
   福利专区（H5 移动端）
   数据：/api/welfare/list、/api/welfare/detail（只读 dataScan 抓取的 scan_post）
   注册：registerH5Module('welfare', renderWelfare)
   依赖：h5.js 的 esc / api / toast / loading
         Assets/js/welfare-md.js 的 window.WelfareMd
   详情：全屏覆盖层展示（非底部抽屉）；地址栏同步 ?welfare=<id>，
         链接 /api/h5.html?welfare=<id> 可直接分享，打开自动落在该帖
   ============================================================ */
(function () {

    var PAGE_SIZE = 20;
    var state = { page: 0, loading: false, done: false, shareId: null, keyword: '' };

    /* ---- 分享链接：?welfare=<id> 与地址栏互同步 ---- */
    function urlWithWelfare(id) {
        try {
            var params = new URLSearchParams(location.search);
            if (id) params.set('welfare', id); else params.delete('welfare');
            var qs = params.toString();
            return location.pathname + (qs ? '?' + qs : '');
        } catch (e) {
            return location.pathname;
        }
    }

    /* ---- 全屏详情覆盖层 ---- */
    function ensureFullscreen() {
        var $fs = $('#wfFullscreen');
        if (!$fs.length) {
            $fs = $(
                '<div class="wf-fullscreen" id="wfFullscreen">' +
                '<div class="wf-fs-head">' +
                '<span class="wf-fs-back" id="wfFsBack">‹ 返回</span>' +
                '<div class="wf-fs-head-title">福利详情</div>' +
                '<span class="wf-fs-copy" id="wfFsCopy" title="复制分享链接">🔗</span>' +
                '</div>' +
                '<div class="wf-fs-body" id="wfFsBody"></div>' +
                '</div>'
            );
            $('body').append($fs);
            $fs.on('click', '#wfFsBack', function () {
                $fs.removeClass('show');
                var wasShare = !!state.shareId;
                state.shareId = null;
                try { history.pushState(null, '', urlWithWelfare(null)); } catch (e) {}
                /* 访客（免登录分享链接）关闭详情后回到登录浮层，浏览其余功能需登录 */
                if (wasShare && (!window.H5State || !H5State.username) && typeof showLogin === 'function') {
                    showLogin();
                }
            });
            $fs.on('click', '#wfFsCopy', function () {
                var url = location.origin + urlWithWelfare(state.shareId);
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(url).then(function () {
                        toast('链接已复制，去分享吧');
                    }, function () { toast('复制失败，请复制地址栏链接'); });
                } else {
                    toast('请复制地址栏链接分享');
                }
            });
        }
        return $fs;
    }

    function showDetail(id) {
        state.shareId = id;
        loading(true);
        api({ url: 'welfare/detail?id=' + id, type: 'GET' }).done(function (res) {
            loading(false);
            var $fs = ensureFullscreen();
            if (res.errorCode !== '000000' || !res.body) {
                /* 分享链接打开但帖子已过期（7 天滚动清理）等场景，在详情页内提示 */
                $fs.find('#wfFsBody').html(
                    '<div class="h5-empty">' + esc(res.errorMsg || '加载失败') + '</div>'
                );
            } else {
                var b = res.body;
                var urlMap = {};
                (b.images || []).forEach(function (img) {
                    if (img.url && img.webUrl) urlMap[img.url] = img.webUrl;
                });
                $fs.find('#wfFsBody').html(
                    '<h3 class="wf-title">' + esc(b.title) + '</h3>' +
                    '<div class="wf-meta">' + esc(b.publishedAt || '') + '</div>' +
                    '<div class="wf-body">' + window.WelfareMd.render(b.contentMd, urlMap) + '</div>'
                );
            }
            $fs.addClass('show');
            $fs.find('#wfFsBody').scrollTop(0);
            /* 地址栏同步为可分享链接 */
            try { history.pushState(null, '', urlWithWelfare(id)); } catch (e) {}
        }).fail(function () {
            loading(false);
            toast('网络错误，请稍后重试');
        });
    }

    /* 分享链接入口：h5.js bootstrap 检测 ?welfare=<id> 后调用 */
    window.H5Welfare = { openShared: function (id) { showDetail(String(id)); } };

    function cardHtml(p) {
        return '<div class="h5-card welfare-card" data-id="' + p.id + '">' +
            (p.firstImage
                ? '<div class="wf-card-img"><img src="' + esc(p.firstImage) + '" loading="lazy" alt=""></div>'
                : '<div class="wf-card-img wf-card-noimg">🎁</div>') +
            '<div class="h5-card-title">' + esc(p.title) + '</div>' +
            '<div class="h5-card-sub">' + esc(p.summary || '') + '</div>' +
            '<div class="wf-card-time">' + esc(p.publishedAt || '') + '</div>' +
            '</div>';
    }

    function loadMore($page) {
        if (state.loading || state.done) return;
        state.loading = true;
        $('#wfLoading').show();
        api({ url: 'welfare/list?page=' + (state.page + 1) + '&size=' + PAGE_SIZE + (state.keyword ? '&keyword=' + encodeURIComponent(state.keyword) : ''), type: 'GET' })
            .done(function (res) {
                var list = (res.errorCode === '000000' && res.body) || [];
                state.page += 1;
                if (list.length < PAGE_SIZE) state.done = true;
                if (state.page === 1 && !list.length) {
                    $('#wfEmpty').show();
                    $('#wfLoading').hide();
                    return;
                }
                var html = '';
                list.forEach(function (p) { html += cardHtml(p); });
                $('#wfList').append(html);
            })
            .fail(function () { state.done = true; toast('加载失败'); })
            .always(function () {
                state.loading = false;
                $('#wfLoading').toggle(!state.done);
            });
    }

    function renderWelfare($page) {
        state.page = 0;
        state.loading = false;
        state.done = false;
        state.keyword = '';
        $page.html(
            '<div class="h5-section-title">🎁 福利专区' +
            '<span class="h5-btn-ghost h5-btn-sm" id="wfRadarBtn" style="float:right">📡 雷达</span></div>' +
            '<div id="wfRadarBox" style="display:none;margin-bottom:10px"></div>' +
            '<input id="wfSearch" placeholder="🔍 搜索标题 / 正文关键词" autocomplete="off" ' +
            'style="width:100%;padding:10px 12px;margin-bottom:10px;border-radius:10px;font-size:13px;' +
            'background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);outline:none;">' +
            '<div id="wfList"></div>' +
            '<div class="h5-empty" id="wfEmpty" style="display:none">暂无福利</div>' +
            '<div class="wf-loading-more" id="wfLoading" style="display:none">加载中…</div>'
        );
        // 关键词模糊搜索（300ms 防抖，命中标题/正文）
        var searchTimer = null;
        $('#wfSearch').off('input').on('input', function () {
            clearTimeout(searchTimer);
            var kw = $(this).val().trim();
            searchTimer = setTimeout(function () {
                if (kw === state.keyword) return;
                state.keyword = kw;
                state.page = 0; state.loading = false; state.done = false;
                $('#wfList').empty(); $('#wfEmpty').hide(); $('#wfLoading').show();
                loadMore($page);
            }, 300);
        });

        loadMore($page);
        $page.off('click').on('click', '.welfare-card', function () {
            showDetail($(this).data('id'));
        });
        $page.on('click', '#wfRadarBtn', openRadarSheet);
        /* .h5-main 是滚动容器：滚到底部自动加载（仅当本页可见时） */
        $('.h5-main').off('scroll.welfare').on('scroll.welfare', function () {
            if (!$page.hasClass('active') || state.loading || state.done) return;
            if (this.scrollHeight - this.scrollTop - this.clientHeight < 300) loadMore($page);
        });
    }

    /* ================= 羊毛雷达（H5 订阅管理，复用 desktop 同一套 /radar 接口） ================= */

    function openRadarSheet() {
        openSheet('<div class="h5-sheet-head"><h3>📡 羊毛雷达</h3></div>' +
            '<div id="h5RadarList"><div class="h5-empty">加载中…</div></div>');
        loadRadarH5();
    }

    function loadRadarH5() {
        api({ url: 'radar/list', type: 'GET' }).done(function (res) {
            var list = (res.errorCode === '000000' && res.body) || [];
            var html = '<div class="h5-card-sub" style="margin-bottom:8px">关键词命中新帖时自动推送（每 5 分钟检查，只推新帖，最多 10 条）</div>';
            list.forEach(function (s) {
                var ch = s.channel === 'dingtalk' ? '钉钉' : '邮箱';
                var target = s.channel === 'dingtalk' ? ('机器人#' + s.robotId) : s.email;
                html += '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
                    '<div class="h5-card-title">🔍 ' + esc(s.keyword) + '</div>' +
                    '<div class="h5-card-sub">→ ' + ch + ' · ' + esc(target) + (s.enabled ? '' : ' · 已暂停') + '</div></div>' +
                    '<div>' +
                    '<button class="h5-btn h5-btn-sm" data-op="toggle" data-id="' + s.id + '" data-on="' + s.enabled + '">' + (s.enabled ? '暂停' : '启用') + '</button>' +
                    ' <button class="h5-btn h5-btn-sm" data-op="test" data-id="' + s.id + '">测试</button>' +
                    ' <button class="h5-btn-danger h5-btn-sm" data-op="del" data-id="' + s.id + '">删</button>' +
                    '</div></div></div>';
            });
            html += '<div class="h5-field"><label>关键词</label><input class="h5-input" id="h5RadarKw" maxlength="64" placeholder="如：VPS / 吃喝"></div>' +
                '<div class="h5-field"><label>推送通道</label><select class="h5-select" id="h5RadarCh">' +
                '<option value="dingtalk">钉钉推送</option><option value="email">邮件推送</option></select></div>' +
                '<div class="h5-field" id="h5RadarRobotField"><label>钉钉机器人</label><select class="h5-select" id="h5RadarRobot"></select></div>' +
                '<div class="h5-field" id="h5RadarEmailField" style="display:none"><label>接收邮箱</label><input class="h5-input" id="h5RadarEmail" placeholder="you@example.com"></div>' +
                '<button class="h5-btn-primary h5-btn-block" id="h5RadarAdd">＋ 添加订阅</button>';
            $('#h5RadarList').html(html);

            api({ url: 'robot/list', type: 'GET' }).done(function (r) {
                var robots = (r && r.body) || [];
                $('#h5RadarRobot').html(robots.length
                    ? robots.map(function (x) { return '<option value="' + x.id + '">' + esc(x.name || ('机器人#' + x.id)) + '</option>'; }).join('')
                    : '<option value="">（请先到 PC 端「钉钉机器人」添加）</option>');
            });
            $('#h5RadarCh').off('change').on('change', function () {
                var isDing = $(this).val() === 'dingtalk';
                $('#h5RadarRobotField').toggle(isDing);
                $('#h5RadarEmailField').toggle(!isDing);
            });
            $('#h5RadarList').off('click', '[data-op]').on('click', '[data-op]', function () {
                var $b = $(this), op = $b.data('op'), id = $b.data('id');
                if (op === 'del') {
                    postJSON('radar/delete', { id: id }).done(function (r) { r.errorCode === '000000' ? loadRadarH5() : toast(r.errorMsg || '失败'); });
                } else if (op === 'toggle') {
                    postJSON('radar/toggle', { id: id, enabled: $b.data('on') ? false : true }).done(function (r) { r.errorCode === '000000' ? loadRadarH5() : toast(r.errorMsg || '失败'); });
                } else if (op === 'test') {
                    toast('测试推送中…');
                    postJSON('radar/test', { id: id }).done(function (r) { r.errorCode === '000000' ? toast(r.body) : toast(r.errorMsg || '失败'); });
                }
            });
            $('#h5RadarAdd').off('click').on('click', function () {
                var kw = $('#h5RadarKw').val().trim();
                if (!kw) { toast('请输入关键词'); return; }
                var payload = { keyword: kw, channel: $('#h5RadarCh').val() };
                if (payload.channel === 'dingtalk') payload.robotId = $('#h5RadarRobot').val();
                else payload.email = $('#h5RadarEmail').val().trim();
                postJSON('radar/add', payload).done(function (r) {
                    if (r.errorCode === '000000') { loadRadarH5(); toast('已添加'); }
                    else toast(r.errorMsg || '添加失败');
                });
            });
        }).fail(function () { $('#h5RadarList').html('<div class="h5-empty">加载失败</div>'); });
    }

    window.registerH5Module('welfare', renderWelfare);
})();
