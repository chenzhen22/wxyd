/* ================= 薄荷账 H5（照 bookkeeping 小程序移植） =================
   金额：后端整数分；展示 分/100 两位小数。分类 8 类与 Android BoheCategories 对齐。 */
(function () {
    'use strict';

    window.BOHE_CATEGORIES = [
        { key: 'food', label: '餐饮', icon: '🍜' },
        { key: 'transport', label: '交通', icon: '🚌' },
        { key: 'shop', label: '购物', icon: '🛍️' },
        { key: 'house', label: '居住', icon: '🏠' },
        { key: 'fun', label: '娱乐', icon: '🎮' },
        { key: 'medical', label: '医疗', icon: '💊' },
        { key: 'study', label: '教育', icon: '📚' },
        { key: 'other', label: '其他', icon: '📦' }
    ];

    /* 好友查看模式：null=我；跨页共享（主页/统计/关注） */
    window.BoheViewing = { friendId: null, friendName: null };

    function boheYuan(fen) { return (Math.round(Number(fen || 0)) / 100).toFixed(2); }
    function boheFen(yuanStr) { return Math.round(parseFloat(yuanStr || '0') * 100); }
    function boheCat(key) {
        for (var i = 0; i < BOHE_CATEGORIES.length; i++) {
            if (BOHE_CATEGORIES[i].key === key) return BOHE_CATEGORIES[i];
        }
        return BOHE_CATEGORIES[7]; /* 未知分类兜底"其他" */
    }
    function todayStr() {
        var d = new Date();
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    }
    /* yyyy-MM-01 锚点 ±n 月 */
    function shiftMonth(anchor, n) {
        var y = Number(anchor.substring(0, 4)), m = Number(anchor.substring(5, 7)) - 1 + n;
        var d = new Date(y, m, 1);
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-01';
    }
    /* 好友查看条 + 切回我（主页/统计共用形态，照小程序 viewer-bar） */
    function viewingBarHtml() {
        if (!BoheViewing.friendId) return '';
        return '<div class="bh-viewer-bar"><span class="bh-vb-name">「' + esc(BoheViewing.friendName) + '」的账单</span>' +
            '<span class="bh-vb-back" data-bohe-back>切回我 ›</span></div>';
    }
    function bindViewingBack($page, rerender) {
        $page.find('[data-bohe-back]').one('click', function () {
            BoheViewing.friendId = null; BoheViewing.friendName = null;
            rerender();
        });
    }
    var viewingParams = function () {
        return BoheViewing.friendId ? '&friendId=' + BoheViewing.friendId : '';
    };
    function monthLabel(anchor) {
        var y = Number(anchor.substring(0, 4)), m = Number(anchor.substring(5, 7));
        return y + '年' + m + '月';
    }
    /* BoheController 系列是 @RequestBody Map 平铺读取（无 body 包裹层），
       必须发 flat JSON；postJSON 的 {"body":{...}} 包裹只适配 @RequestBody Result 控制器 */
    function postFlat(url, obj) {
        return api({ url: url, type: 'POST', contentType: 'application/json', data: JSON.stringify(obj) });
    }

    /* ---------------- 薄荷账主页（照小程序 pages/bills 布局） ---------------- */

    var boheAnchor = null; /* yyyy-MM-01 */
    function curMonth() { return (boheAnchor || todayStr()).substring(0, 7); }
    /* 查看他人 = 只读（账单页 readOnly，与小程序一致） */
    function isViewerMode() { return !!BoheViewing.friendId; }

    function renderBohe($page) {
        if (!boheAnchor) boheAnchor = todayStr().substring(0, 7) + '-01';
        var month = curMonth();
        var viewer = isViewerMode();
        $page.html(
            '<div class="bh-hero">' +
            '<div class="bh-hero-sub">' + monthLabel(boheAnchor) + ' · 支出' + (viewer ? ' · ' + esc(BoheViewing.friendName) : '') + '</div>' +
            '<div class="bh-hero-amt" id="bhMonthTotal">¥ --</div>' +
            '<div class="bh-hero-foot" id="bhMonthCount"></div>' +
            '</div>' +
            '<div class="bh-month-bar">' +
            '<span class="bh-m-btn" data-bohe-prev>‹ ' + monthLabel(boheAnchor) + '</span>' +
            '<span class="bh-m-mid">本月</span>' +
            '<span class="bh-m-btn bh-muted' + (month === todayStr().substring(0, 7) ? ' bh-dis' : '') + '" data-bohe-next>下月 ›</span>' +
            '</div>' +
            '<div class="bh-list" id="bhBillList"><div class="bh-empty">加载中…</div></div>' +
            (viewer ? '' : '<div class="bh-fab" data-bohe-add>＋</div>') +
            viewingBarHtml()
        );
        bindViewingBack($page, function () { renderBohe($page); });

        $page.find('[data-bohe-prev]').one('click', function () { boheAnchor = shiftMonth(boheAnchor, -1); renderBohe($page); });
        $page.find('[data-bohe-next]').one('click', function () {
            if (curMonth() >= todayStr().substring(0, 7)) return; /* 不能看未来（与小程序一致） */
            boheAnchor = shiftMonth(boheAnchor, 1); renderBohe($page);
        });
        $page.find('[data-bohe-add]').one('click', function () { showAddSheet($page); });

        api({ url: 'bohe/bill/list?month=' + month + viewingParams(), type: 'GET' })
            .done(function (br) {
                if (br.errorCode !== '000000') { $('#bhBillList').html('<div class="bh-empty">' + esc(br.errorMsg || '获取账单失败') + '</div>'); return; }
                renderBillList(br.body && br.body.bills || []);
            })
            .fail(function () { $('#bhBillList').html('<div class="bh-empty">网络错误</div>'); });
    }

    /* 按日分组渲染（dateLabel: 今天/昨天/M月D日 + 每日合计），照小程序 groups 结构 */
    function renderBillList(bills) {
        var viewer = isViewerMode();
        var total = 0;
        var map = {}, order = [];
        var today = todayStr(), yesterday = (function () {
            var d = new Date(); d.setDate(d.getDate() - 1);
            return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
        })();
        bills.forEach(function (b) {
            var day = String(b.billDate || '').substring(0, 10);
            var key = day === today ? '今天' : (day === yesterday ? '昨天' : Number(day.substring(5, 7)) + '月' + Number(day.substring(8, 10)) + '日');
            if (!map[key]) { map[key] = { day: day, items: [] }; order.push(key); }
            map[key].items.push(b);
            total += Number(b.amount || 0);
        });
        $('#bhMonthTotal').text('¥ ' + boheYuan(total));
        $('#bhMonthCount').text('共 ' + bills.length + ' 笔');
        if (!order.length) {
            $('#bhBillList').html(viewer ? '<div class="bh-empty">对方本月暂无记录</div>' :
                '<div class="bh-empty-block"><div class="bh-empty-leaf">🌱</div>' +
                '<div class="bh-empty-title">本月还没有记录</div>' +
                '<div class="bh-empty-sub">随手记一笔，月底看清钱花哪儿了</div>' +
                '<div class="bh-empty-cta" data-bohe-add2>＋ 记第一笔</div></div>');
            $('[data-bohe-add2]').one('click', function () { showAddSheet($('#page-bohe')); });
            return;
        }
        var html = '';
        order.forEach(function (key) {
            var g = map[key];
            var dayTotal = 0;
            g.items.forEach(function (b) { dayTotal += Number(b.amount || 0); });
            html += '<div class="bh-grp-head"><span>' + esc(key) + '</span><span>¥ ' + boheYuan(dayTotal) + '</span></div>' +
                '<div class="bh-card bh-grp">';
            g.items.forEach(function (b) {
                var cat = boheCat(b.category);
                html += '<div class="bh-swipe" data-bill-id="' + esc(b.id) + '">' +
                    '<div class="bh-acts">' +
                    '<div class="bh-act bh-act-edit" data-act-edit="' + esc(b.id) + '">编辑</div>' +
                    '<div class="bh-act bh-act-del" data-del-id="' + esc(b.id) + '">删除</div>' +
                    '</div>' +
                    '<div class="bh-row bh-trans">' +
                    '<div class="bh-ico">' + cat.icon + '</div>' +
                    '<div class="bh-mid"><div class="bh-title">' + esc(b.note || cat.label) + '</div>' +
                    '<div class="bh-sub">' + cat.label + '</div></div>' +
                    '<div class="bh-amt">-' + boheYuan(b.amount) + '</div>' +
                    '</div></div>';
            });
            html += '</div>';
        });
        $('#bhBillList').html(html);
    }

    /* 左滑露出编辑/删除（touch，照小程序 swipe-row：-150px 全露，滑动中无过渡，过半吸附） */
    var bhSwipeOpen = null; /* 当前已滑开的行 */
    $(document).on('touchstart', '#bhBillList .bh-row', function (e) {
        var t = e.originalEvent.touches[0];
        this._sx = t.clientX; this._sy = t.clientY;
        if (bhSwipeOpen && bhSwipeOpen !== this) { bhSwipeOpen._x = 0; bhSwipeOpen.style.transform = ''; }
    });
    $(document).on('touchmove', '#bhBillList .bh-row', function (e) {
        var t = e.originalEvent.touches[0];
        var dx = t.clientX - (this._sx || 0);
        if (Math.abs(dx) < 8 || Math.abs(dx) <= Math.abs(t.clientY - (this._sy || 0))) return;
        var x = Math.max(-150, Math.min(0, dx + (this._x || 0)));
        this._x = x;
        this.classList.remove('bh-trans');
        this.style.transform = 'translateX(' + x + 'px)';
    });
    $(document).on('touchend', '#bhBillList .bh-row', function () {
        this.classList.add('bh-trans');
        /* 超过一半就全开，否则回弹 */
        this._x = this._x < -75 ? -150 : 0;
        this.style.transform = 'translateX(' + this._x + 'px)';
        bhSwipeOpen = this._x < 0 ? this : null;
    });
    /* 点击已滑开的行先收起；未滑开时进入编辑（readOnly 禁用，与小程序一致） */
    $(document).on('click', '#bhBillList .bh-row', function () {
        if (this._x < 0) { this._x = 0; this.style.transform = ''; bhSwipeOpen = null; return; }
        if (isViewerMode()) return;
        editBill($(this).closest('.bh-swipe').data('billId'), $('#page-bohe'));
    });

    /* 编辑/删除 */
    $(document).on('click', '#page-bohe [data-act-edit]', function () {
        editBill($(this).data('actEdit'), $('#page-bohe'));
    });
    $(document).on('click', '#page-bohe [data-del-id]', function () {
        var id = $(this).data('delId');
        confirm('删除该笔账单？', function () {
            postFlat('bohe/bill/delete', { id: Number(id) }).done(function (res) {
                if (res.errorCode === '000000') { toast('已删除'); renderBohe($('#page-bohe')); }
                else toast(res.errorMsg || '删除失败');
            });
        });
    });
    /* 编辑：抽屉预填原值，保存走同一 save 接口（id 非空=编辑，仅属主） */
    function editBill(id, $page) {
        var $swipe = $('#page-bohe .bh-swipe[data-bill-id="' + id + '"]');
        var catLabel = $swipe.find('.bh-sub').text();
        var key = null;
        BOHE_CATEGORIES.forEach(function (c) { if (c.label === catLabel) key = c.key; });
        var title = $swipe.find('.bh-title').text();
        var note = '';
        BOHE_CATEGORIES.forEach(function (c) { if (c.label === title) note = ''; });
        var amt = $swipe.find('.bh-amt').text().replace('-', '');
        showAddSheet($page, { id: id, category: key || 'other', note: (title !== catLabel ? title : ''), amountYuan: amt });
    }

    /* ---------------- 记一笔抽屉（edit 传原值 = 编辑模式） ---------------- */

    function showAddSheet($page, edit) {
        edit = edit || null;
        var selKey = edit ? edit.category : 'food';
        var grid = '';
        BOHE_CATEGORIES.forEach(function (c) {
            grid += '<div class="bohe-cat' + (c.key === selKey ? ' on' : '') + '" data-cat="' + c.key + '">' +
                '<div class="bohe-cat-ico">' + c.icon + '</div>' + c.label + '</div>';
        });
        openSheet(
            '<h3 style="margin:0 0 12px">' + (edit ? '编辑账单' : '记一笔') + '</h3>' +
            '<input class="h5-input bh-amt" id="boheAmt" type="number" step="0.01" min="0" inputmode="decimal" placeholder="金额 ¥" value="' + (edit ? esc(edit.amountYuan) : '') + '">' +
            '<div class="bohe-cats" id="boheCats">' + grid + '</div>' +
            '<input class="h5-input" id="boheNote" type="text" maxlength="50" placeholder="备注（可选）" value="' + (edit && edit.note ? esc(edit.note) : '') + '">' +
            '<input class="h5-input" id="boheDate" type="date" value="' + todayStr() + '">' +
            '<button class="h5-btn-primary h5-btn-block" id="boheSaveBtn" style="margin-top:12px">保存</button>'
        );
        $('#boheCats').on('click', '.bohe-cat', function () {
            $('#boheCats .bohe-cat').removeClass('on');
            $(this).addClass('on');
        });
        $('#boheSaveBtn').off('click').on('click', function () {
            var amt = boheFen($('#boheAmt').val());
            if (!(amt > 0)) { toast('请输入正确金额'); return; }
            var category = $('#boheCats .bohe-cat.on').data('cat') || 'food';
            var body = {
                amount: amt, category: category,
                note: $('#boheNote').val() || null,
                date: $('#boheDate').val() || todayStr()
            };
            if (edit) body.id = Number(edit.id);
            postFlat('bohe/bill/save', body).done(function (res) {
                if (res.errorCode === '000000') { toast(edit ? '已保存' : '已记一笔'); closeSheet(); renderBohe($page); }
                else toast(res.errorMsg || '记账失败');
            });
        });
    }

    /* ---------------- 统计页（照小程序 pages/stats 布局：seg 分段器 + 环形图 + 趋势） ---------------- */

    var statsAnchor = null;    /* yyyy-MM-dd 锚点 */
    var statsPeriod = 'month'; /* day | month | year */
    /* 与小程序 RING_COLORS 一致 */
    var RING_COLORS = ['#3eb575', '#8fd3a8', '#cfe9d9', '#a8d5c0', '#e8f7ef', '#bfe3cf', '#d9efe2', '#eef2f5'];

    function periodValue() {
        var a = statsAnchor || todayStr();
        if (statsPeriod === 'day') return a.substring(0, 10);
        if (statsPeriod === 'year') return a.substring(0, 4);
        return a.substring(0, 7);
    }
    function periodLabel() {
        var v = periodValue();
        if (statsPeriod === 'year') return v + '年';
        if (statsPeriod === 'day') return v;
        return v.replace('-', '年') + '月';
    }
    function shiftPeriod(dir) {
        var a = statsAnchor || todayStr();
        var y = Number(a.substring(0, 4)), m = Number(a.substring(5, 7)) - 1, d = Number(a.substring(8, 10));
        if (statsPeriod === 'month') { var dt = new Date(y, m + dir, 1); statsAnchor = dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-01'; }
        else if (statsPeriod === 'year') { statsAnchor = (y + dir) + '-01-01'; }
        else { var dd = new Date(y, m, d + dir); statsAnchor = dd.getFullYear() + '-' + String(dd.getMonth() + 1).padStart(2, '0') + '-' + String(dd.getDate()).padStart(2, '0'); }
    }
    /* 切粒度回今天（与小程序 switchRange 一致） */
    function resetPeriodToToday() {
        if (statsPeriod === 'month') statsAnchor = todayStr().substring(0, 7) + '-01';
        else if (statsPeriod === 'year') statsAnchor = todayStr().substring(0, 4) + '-01-01';
        else statsAnchor = todayStr();
    }
    /* 环比上月洞察（照小程序 insight：总额环比 + 变化最大分类，纯前端计算） */
    function computeInsight(cur, prev) {
        var ct = cur.total || 0;
        if (ct === 0) return null;
        var pt = prev.total || 0;
        var delta = ct - pt;
        var hasPrev = pt > 0;
        var pct = hasPrev ? Math.round(delta / pt * 100) : null;
        var dir = delta > 0 ? 'up' : (delta < 0 ? 'down' : 'flat');
        var cMap = {}, pMap = {};
        (cur.byCategory || []).forEach(function (c) { cMap[c.category] = Number(c.total || 0); });
        (prev.byCategory || []).forEach(function (c) { pMap[c.category] = Number(c.total || 0); });
        var top = null;
        Object.keys(cMap).concat(Object.keys(pMap)).forEach(function (k) {
            var dd = (cMap[k] || 0) - (pMap[k] || 0);
            if (dd === 0) return;
            if (!top || Math.abs(dd) > Math.abs(top.delta)) top = { key: k, delta: dd };
        });
        var summary;
        if (pt === 0) summary = '上月还没记账，这个月从零开始积累';
        else if (dir === 'flat') summary = '支出和上月基本持平';
        else {
            var word = dir === 'up' ? '多' : '少';
            summary = '比上月' + word + '花' + (pct > 0 ? '+' : '') + pct + '%';
            if (top) summary += '，主要是「' + boheCat(top.key).label + '」' + (top.delta > 0 ? '增加' : '减少');
        }
        return { dir: dir, deltaLabel: (delta > 0 ? '+' : '-') + boheYuan(Math.abs(delta)), hasPrev: hasPrev, summary: summary };
    }

    function renderBoheStats($page) {
        if (!statsAnchor) resetPeriodToToday();
        var segs = '';
        [['day', '日'], ['month', '月'], ['year', '年']].forEach(function (p) {
            segs += '<div class="bh-seg-item' + (statsPeriod === p[0] ? ' on' : '') + '" data-period="' + p[0] + '">' + p[1] + '</div>';
        });
        $page.html(
            '<div class="bh-seg">' + segs + '</div>' +
            '<div class="bh-nav">' +
            '<span class="bh-nav-btn" data-bohe-prev>‹</span>' +
            '<span class="bh-nav-label">' + periodLabel() + '</span>' +
            '<span class="bh-nav-btn" data-bohe-next>›</span>' +
            '</div>' +
            '<div class="bh-card bh-total-card">' +
            '<div class="bh-muted">当期支出</div>' +
            '<div class="bh-total" id="bhStatsTotal">¥ --</div>' +
            '<div class="bh-total-foot"><span class="bh-muted" id="bhStatsCount"></span></div>' +
            '</div>' +
            '<div id="bhInsight"></div>' +
            '<div class="bh-card bh-sec">' +
            '<div class="bh-sec-title">支出分类</div>' +
            '<div id="bhRingWrap"><div class="bh-muted bh-empty">加载中…</div></div>' +
            '</div>' +
            '<div class="bh-card bh-sec">' +
            '<div class="bh-sec-title" id="bhTrendTitle"></div>' +
            '<div id="bhTrendWrap"></div>' +
            '</div>' +
            viewingBarHtml()
        );
        bindViewingBack($page, function () { renderBoheStats($page); });
        $page.find('.bh-seg-item').one('click', function () { statsPeriod = $(this).data('period'); resetPeriodToToday(); renderBoheStats($page); });
        $page.find('[data-bohe-prev]').one('click', function () { shiftPeriod(-1); renderBoheStats($page); });
        $page.find('[data-bohe-next]').one('click', function () {
            /* 不能看未来（与小程序一致：day=今天/月=本月/年=今年时禁 next） */
            var today = todayStr();
            var a = statsAnchor;
            var beyond = statsPeriod === 'day' ? a >= today
                : statsPeriod === 'year' ? a.substring(0, 4) >= today.substring(0, 4)
                : a.substring(0, 7) >= today.substring(0, 7);
            if (beyond) return;
            shiftPeriod(1); renderBoheStats($page);
        });

        loadStats($page);
    }

    function loadStats($page) {
        var v = viewingParams();
        var base = 'bohe/stats?month=' + periodValue() + '&period=' + statsPeriod + v;
        var calls = [api({ url: base, type: 'GET' })];
        /* 环比洞察：仅月视图且看自己时拉上月（照小程序） */
        if (statsPeriod === 'month' && !isViewerMode()) {
            calls.push(api({ url: 'bohe/stats?month=' + shiftMonth(statsAnchor, -1).substring(0, 7) + '&period=month', type: 'GET' }));
        }
        $.when.apply($, calls).done(function (r1, r2) {
            var r = r1[0] || r1;
            if (r.errorCode !== '000000') {
                $('#bhRingWrap').html('<div class="bh-muted bh-empty">' + esc(r.errorMsg || '获取统计失败') + '</div>');
                return;
            }
            var total = Number(r.body && r.body.total || 0);
            var cats = (r.body && r.body.byCategory) || [];
            $('#bhStatsTotal').text('¥ ' + boheYuan(total));
            /* count 后端未返回：按分类行不可靠 → 用账单列表补（月/年都拉当期账单计数；日=明细即账单） */
            api({ url: 'bohe/bill/list?month=' + periodValue() + '&period=' + statsPeriod + v, type: 'GET' })
                .done(function (br) {
                    var bills = (br.errorCode === '000000' && br.body && br.body.bills) || [];
                    $('#bhStatsCount').text('共 ' + bills.length + ' 笔');
                    renderTrend(bills);
                });
            renderRing(cats, total);
            /* 环比 */
            if (statsPeriod === 'month' && !isViewerMode()) {
                var pr = r2[0];
                if (pr && pr.errorCode === '000000' && pr.body) {
                    var ins = computeInsight({ total: total, byCategory: cats }, { total: Number(pr.body.total || 0), byCategory: pr.body.byCategory || [] });
                    if (ins) {
                        $('#bhInsight').html(
                            '<div class="bh-card bh-insight bh-ins-' + ins.dir + '">' +
                            '<div class="bh-ins-top"><span class="bh-ins-tag">环比上月</span>' +
                            '<span class="bh-ins-delta">' + ins.deltaLabel + '</span></div>' +
                            '<div class="bh-ins-sum">' + esc(ins.summary) + '</div></div>'
                        );
                    }
                }
            }
        }).fail(function () {
            $('#bhRingWrap').html('<div class="bh-muted bh-empty">网络错误</div>');
        });
    }

    /* 环形图（SVG）+ 图例，照小程序 chart drawRing + legend */
    function renderRing(cats, total) {
        if (!cats.length) { $('#bhRingWrap').html('<div class="bh-muted bh-empty">暂无支出</div>'); return; }
        var R = 56, CX = 66, CY = 66, SW = 14, CIRC = 2 * Math.PI * R;
        var svg = '<svg class="bh-ring" viewBox="0 0 132 132">' +
            '<circle cx="' + CX + '" cy="' + CY + '" r="' + R + '" fill="none" stroke="#eef2f5" stroke-width="' + SW + '"/>';
        var offset = 0;
        cats.forEach(function (c, i) {
            var frac = total > 0 ? Number(c.total || 0) / total : 0;
            var len = frac * CIRC;
            svg += '<circle cx="' + CX + '" cy="' + CY + '" r="' + R + '" fill="none" stroke="' + RING_COLORS[i % RING_COLORS.length] +
                '" stroke-width="' + SW + '" stroke-dasharray="' + len + ' ' + (CIRC - len) + '" stroke-dashoffset="' + (-offset) + '" ' +
                'transform="rotate(-90 ' + CX + ' ' + CY + ')"/>';
            offset += len;
        });
        svg += '<text x="' + CX + '" y="' + (CY - 4) + '" text-anchor="middle" font-size="10" fill="#9aa5b1">支出</text>' +
            '<text x="' + CX + '" y="' + (CY + 14) + '" text-anchor="middle" font-size="12" font-weight="bold" fill="#1a2b33">¥' + boheYuan(total) + '</text>' +
            '</svg>';
        var legend = '<div class="bh-legend">';
        cats.forEach(function (c, i) {
            var cat = boheCat(c.category);
            var pct = total > 0 ? Math.round(Number(c.total || 0) / total * 100) : 0;
            legend += '<div class="bh-lg-row"><span class="bh-lg-dot" style="background:' + RING_COLORS[i % RING_COLORS.length] + '"></span>' +
                '<span class="bh-lg-name">' + cat.label + '</span>' +
                '<span class="bh-lg-val">¥' + boheYuan(c.total) + ' ' + pct + '%</span></div>';
        });
        legend += '</div>';
        $('#bhRingWrap').html('<div class="bh-ring-wrap">' + svg + '</div>' + legend);
    }

    /* 趋势：day=当日明细列表；month=每日柱状；year=每月柱状（本地分桶，照小程序） */
    function renderTrend(bills) {
        var title = statsPeriod === 'day' ? '当日明细' : (statsPeriod === 'month' ? '每日趋势' : '每月趋势');
        $('#bhTrendTitle').text(title);
        if (statsPeriod === 'day') {
            if (!bills.length) { $('#bhTrendWrap').html('<div class="bh-muted bh-empty">当天暂无记录</div>'); return; }
            var html = '<div class="bh-detail">';
            bills.forEach(function (b) {
                html += '<div class="bh-dt-row"><span>' + esc(b.note || boheCat(b.category).label) + '</span><span>¥' + boheYuan(b.amount) + '</span></div>';
            });
            $('#bhTrendWrap').html(html + '</div>');
            return;
        }
        var buckets = [], amounts = [];
        if (statsPeriod === 'month') {
            var y = Number(statsAnchor.substring(0, 4)), m = Number(statsAnchor.substring(5, 7)) - 1;
            var days = new Date(y, m + 1, 0).getDate();
            for (var i = 1; i <= days; i++) buckets.push(String(i));
        } else {
            for (var j = 1; j <= 12; j++) buckets.push(j + '月');
        }
        amounts = buckets.map(function () { return 0; });
        bills.forEach(function (b) {
            var day = String(b.billDate || '').substring(0, 10);
            var idx = statsPeriod === 'month' ? Number(day.substring(8, 10)) - 1 : Number(day.substring(5, 7)) - 1;
            if (idx >= 0 && idx < amounts.length) amounts[idx] += Number(b.amount || 0);
        });
        var max = Math.max.apply(null, amounts.concat([1]));
        var bars = '<div class="bh-bars">';
        buckets.forEach(function (label, i) {
            var h = Math.max(Math.round(amounts[i] / max * 100), amounts[i] > 0 ? 2 : 0);
            bars += '<div class="bh-bar-col"><div class="bh-bar-v" style="height:' + h + '%' + (amounts[i] === max && max > 0 ? ';background:#3eb575' : '') + '"></div>' +
                (statsPeriod === 'month' && (i + 1) % 5 !== 0 ? '' : '<span class="bh-bar-label">' + label + '</span>') +
                '</div>';
        });
        $('#bhTrendWrap').html(bars + '</div>');
    }

    /* ---------------- 关注页 ---------------- */

    function friendLabel(u) { return (u.displayName && String(u.displayName).trim()) || u.username || String(u.userId); }

    function renderBoheFriends($page) {
        $page.html(
            '<div class="bohe-search">' +
            '<input class="h5-input" id="boheSearchKw" type="text" placeholder="搜索用户名/昵称" style="flex:1">' +
            '<button class="h5-btn" id="boheSearchBtn">搜索</button></div>' +
            '<div id="boheSearchResult"></div>' +
            '<div class="h5-section-title">好友</div>' +
            '<div id="boheFriendList"><div class="bohe-empty">加载中…</div></div>' +
            '<div class="h5-card bohe-viscard"><div class="bohe-visrow">' +
            '<span>👁️ 允许好友查看我的薄荷账</span>' +
            '<label class="bohe-switch"><input type="checkbox" id="boheVisChk"><span class="bohe-slider"></span></label>' +
            '</div></div>'
        );
        $('#boheSearchBtn').off('click').on('click', doSearch);
        $('#boheSearchKw').on('keydown', function (e) { if (e.key === 'Enter') doSearch(); });
        loadFriends();
        api({ url: 'bohe/visibility', type: 'GET' }).done(function (r) {
            if (r.errorCode === '000000' && r.body) $('#boheVisChk').prop('checked', Number(r.body.hidden) !== 1);
        });
        $('#boheVisChk').on('change', function () {
            var allow = $(this).prop('checked');
            postFlat('bohe/visibility', { hidden: !allow }).done(function (r) {
                if (r.errorCode === '000000') toast(allow ? '已允许好友查看' : '已对好友隐藏');
                else { toast(r.errorMsg || '设置失败'); $('#boheVisChk').prop('checked', !allow); }
            });
        });
    }

    function doSearch() {
        var kw = ($('#boheSearchKw').val() || '').trim();
        if (!kw) return;
        $('#boheSearchResult').html('<div class="bohe-empty">搜索中…</div>');
        api({ url: 'bohe/user/search?keyword=' + encodeURIComponent(kw), type: 'GET' }).done(function (r) {
            var users = (r.body && r.body.users) || [];
            if (r.errorCode !== '000000') { $('#boheSearchResult').html('<div class="bohe-empty">' + esc(r.errorMsg) + '</div>'); return; }
            if (!users.length) { $('#boheSearchResult').html('<div class="bohe-empty">没有找到用户</div>'); return; }
            var html = '';
            users.forEach(function (u) {
                /* searchUsers 返回 snake_case {id, username, display_name}（controller 未重映射） */
                html += '<div class="bohe-friend"><div class="bohe-friend-main">' +
                    '<div class="bohe-bill-title">' + esc(u.display_name || u.username || String(u.id)) + '</div>' +
                    '<div class="bohe-bill-sub">' + esc(u.username || '') + '</div></div>' +
                    '<div class="h5-btn-primary bohe-fbtn" data-follow="' + esc(u.id) + '" data-follow-name="' + esc(u.username || '') + '">+ 关注</div></div>';
            });
            $('#boheSearchResult').html(html);
        });
    }
    $(document).on('click', '#page-boheFriends [data-follow]', function () {
        var btn = $(this);
        /* 控制器按 username 解析关注对象（与 Android 客户端一致） */
        postFlat('bohe/friend/request', { username: btn.data('followName') }).done(function (r) {
            if (r.errorCode === '000000') { toast('已提交关注申请'); btn.text('已申请').off('click'); }
            else toast(r.errorMsg || '申请失败');
        });
    });

    function loadFriends() {
        api({ url: 'bohe/friend/list', type: 'GET' }).done(function (r) {
            if (r.errorCode !== '000000') { $('#boheFriendList').html('<div class="bohe-empty">' + esc(r.errorMsg || '获取失败') + '</div>'); return; }
            var fs = (r.body && r.body.friends) || [];
            var pending = fs.filter(function (f) { return Number(f.status) === 0 && !f.outgoing; });
            var outgoing = fs.filter(function (f) { return Number(f.status) === 0 && f.outgoing; });
            var ok = fs.filter(function (f) { return Number(f.status) === 1; });
            var html = '';
            if (pending.length) {
                html += '<div class="bohe-group">待我处理</div>';
                pending.forEach(function (f) {
                    html += '<div class="bohe-friend"><div class="bohe-friend-main">' +
                        '<div class="bohe-bill-title">' + esc(friendLabel(f)) + '</div>' +
                        '<div class="bohe-bill-sub">请求关注你的薄荷账</div></div>' +
                        '<div class="h5-btn-primary bohe-fbtn" data-accept="' + esc(f.userId) + '">同意</div>' +
                        '<div class="h5-btn bohe-fbtn" data-reject="' + esc(f.userId) + '">拒绝</div></div>';
                });
            }
            if (outgoing.length) {
                html += '<div class="bohe-group">我发起的</div>';
                outgoing.forEach(function (f) {
                    html += '<div class="bohe-friend"><div class="bohe-friend-main">' +
                        '<div class="bohe-bill-title">' + esc(friendLabel(f)) + '</div>' +
                        '<div class="bohe-bill-sub">等待对方通过</div></div>' +
                        '<div class="h5-btn bohe-fbtn" data-cancel="' + esc(f.userId) + '">撤回</div></div>';
                });
            }
            html += ok.length ? '<div class="bohe-group">已通过（点击查看 TA 的账单）</div>' : '';
            ok.forEach(function (f) {
                html += '<div class="bohe-friend bohe-friend-ok" data-view="' + esc(f.userId) + '" data-name="' + esc(friendLabel(f)) + '">' +
                    '<div class="bohe-friend-main">' +
                    '<div class="bohe-bill-title">' + esc(friendLabel(f)) + '</div>' +
                    '<div class="bohe-bill-sub">' + esc(f.username || '') + '</div></div>' +
                    '<div class="h5-btn bohe-fbtn" data-unfollow="' + esc(f.userId) + '" data-unname="' + esc(friendLabel(f)) + '">取消关注</div></div>';
            });
            if (!fs.length) html += '<div class="bohe-empty">还没有好友，先搜索添加吧</div>';
            $('#boheFriendList').html(html);
        }).fail(function () { $('#boheFriendList').html('<div class="bohe-empty">网络错误</div>'); });
    }

    $(document).on('click', '#page-boheFriends [data-accept]', function () {
        handleFriend(Number($(this).data('accept')), true);
    });
    $(document).on('click', '#page-boheFriends [data-reject]', function () {
        handleFriend(Number($(this).data('reject')), false);
    });
    function handleFriend(fid, accept) {
        postFlat('bohe/friend/handle', { userId: fid, accept: accept }).done(function (r) {
            if (r.errorCode === '000000') { toast(accept ? '已同意' : '已拒绝'); loadFriends(); }
            else toast(r.errorMsg || '操作失败');
        });
    }
    $(document).on('click', '#page-boheFriends [data-cancel]', function () {
        var fid = Number($(this).data('cancel'));
        confirm('撤回该关注申请？', function () {
            postFlat('bohe/friend/remove', { userId: fid }).done(function (r) {
                if (r.errorCode === '000000') { toast('已撤回'); loadFriends(); }
                else toast(r.errorMsg || '操作失败');
            });
        });
    });
    $(document).on('click', '#page-boheFriends [data-unfollow]', function (e) {
        e.stopPropagation();
        var fid = Number($(this).data('unfollow'));
        var name = $(this).data('unname');
        confirm('取消关注「' + name + '」？', function () {
            postFlat('bohe/friend/remove', { userId: fid }).done(function (r) {
                if (r.errorCode === '000000') { toast('已取消关注'); loadFriends(); }
                else toast(r.errorMsg || '操作失败');
            });
        });
    });
    $(document).on('click', '#page-boheFriends [data-view]', function () {
        BoheViewing.friendId = Number($(this).data('view'));
        BoheViewing.friendName = $(this).data('name');
        goPage('bohe');
    });

    /* 注册 */
    window.registerH5Module('bohe', renderBohe);
    window.registerH5Module('boheStats', renderBoheStats);
    window.registerH5Module('boheFriends', renderBoheFriends);
})();
