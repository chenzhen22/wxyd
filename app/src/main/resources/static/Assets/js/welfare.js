/* ============================================================
   福利专区（PC 桌面版）
   数据：/api/welfare/list、/api/welfare/detail（只读 dataScan 抓取的 scan_post）
   纯展示：卡片=首图+标题+摘要+时间；详情=标题+时间+图文正文。
   Markdown 渲染依赖 Assets/js/welfare-md.js（window.WelfareMd）。
   ============================================================ */
(function () {

    var PAGE_SIZE = 20;
    var state = { page: 0, loading: false, done: false, observer: null };

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
        $.getJSON('welfare/list', { page: state.page + 1, size: PAGE_SIZE }).done(function (res) {
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

    window.Welfare = {
        init: function () {
            if (!$('#welfareGrid').length) return;
            reset();
            // 事件委托（卡片动态生成）
            $('#welfareGrid').off('click').on('click', '.welfare-card', function () {
                showDetail($(this).data('id'));
            });
            $('#welfareBackBtn').off('click').on('click', showList);
        }
    };
})();
