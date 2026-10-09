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
    var state = { page: 0, loading: false, done: false, shareId: null };

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
                state.shareId = null;
                try { history.pushState(null, '', urlWithWelfare(null)); } catch (e) {}
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
        api({ url: 'welfare/list?page=' + (state.page + 1) + '&size=' + PAGE_SIZE, type: 'GET' })
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
        $page.html(
            '<div class="h5-section-title">🎁 福利专区</div>' +
            '<div id="wfList"></div>' +
            '<div class="h5-empty" id="wfEmpty" style="display:none">暂无福利</div>' +
            '<div class="wf-loading-more" id="wfLoading" style="display:none">加载中…</div>'
        );
        loadMore($page);
        $page.off('click').on('click', '.welfare-card', function () {
            showDetail($(this).data('id'));
        });
        /* .h5-main 是滚动容器：滚到底部自动加载（仅当本页可见时） */
        $('.h5-main').off('scroll.welfare').on('scroll.welfare', function () {
            if (!$page.hasClass('active') || state.loading || state.done) return;
            if (this.scrollHeight - this.scrollTop - this.clientHeight < 300) loadMore($page);
        });
    }

    window.registerH5Module('welfare', renderWelfare);
})();
