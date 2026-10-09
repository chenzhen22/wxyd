/* ============================================================
   福利专区（H5 移动端）
   数据：/api/welfare/list、/api/welfare/detail（只读 dataScan 抓取的 scan_post）
   注册：registerH5Module('welfare', renderWelfare)
   依赖：h5.js 的 esc / api / toast / loading
         Assets/js/welfare-md.js 的 window.WelfareMd
   详情：全屏覆盖层展示（非底部抽屉）
   ============================================================ */
(function () {

    var PAGE_SIZE = 20;
    var state = { page: 0, loading: false, done: false };

    /* ---- 全屏详情覆盖层 ---- */
    function ensureFullscreen() {
        var $fs = $('#wfFullscreen');
        if (!$fs.length) {
            $fs = $(
                '<div class="wf-fullscreen" id="wfFullscreen">' +
                '<div class="wf-fs-head">' +
                '<span class="wf-fs-back" id="wfFsBack">‹ 返回</span>' +
                '<div class="wf-fs-head-title">福利详情</div>' +
                '</div>' +
                '<div class="wf-fs-body" id="wfFsBody"></div>' +
                '</div>'
            );
            $('body').append($fs);
            $fs.on('click', '#wfFsBack', function () { $fs.removeClass('show'); });
        }
        return $fs;
    }

    function showDetail(id) {
        loading(true);
        api({ url: 'welfare/detail?id=' + id, type: 'GET' }).done(function (res) {
            loading(false);
            if (res.errorCode !== '000000' || !res.body) {
                toast(res.errorMsg || '加载失败');
                return;
            }
            var b = res.body;
            var urlMap = {};
            (b.images || []).forEach(function (img) {
                if (img.url && img.webUrl) urlMap[img.url] = img.webUrl;
            });
            var $fs = ensureFullscreen();
            $fs.find('#wfFsBody').html(
                '<h3 class="wf-title">' + esc(b.title) + '</h3>' +
                '<div class="wf-meta">' + esc(b.publishedAt || '') + '</div>' +
                '<div class="wf-body">' + window.WelfareMd.render(b.contentMd, urlMap) + '</div>'
            );
            $fs.addClass('show');
            $fs.find('#wfFsBody').scrollTop(0);
        }).fail(function () {
            loading(false);
            toast('网络错误，请稍后重试');
        });
    }

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
