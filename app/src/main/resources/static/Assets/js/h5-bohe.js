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
    /* 好友查看条 + 切回我（主页/统计共用形态） */
    function viewingBarHtml() {
        if (!BoheViewing.friendId) return '';
        return '<div class="bohe-viewing">正在看 <b>' + esc(BoheViewing.friendName) + '</b> 的薄荷账' +
            '<button class="h5-btn bohe-btn-back" data-bohe-back>切回我</button></div>';
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
    /* BoheController 系列是 @RequestBody Map 平铺读取（无 body 包裹层），
       必须发 flat JSON；postJSON 的 {"body":{...}} 包裹只适配 @RequestBody Result 控制器 */
    function postFlat(url, obj) {
        return api({ url: url, type: 'POST', contentType: 'application/json', data: JSON.stringify(obj) });
    }

    /* ---------------- 薄荷账主页 ---------------- */

    var boheAnchor = null; /* yyyy-MM-01 */
    function curMonth() { return (boheAnchor || todayStr()).substring(0, 7); }

    function renderBohe($page) {
        if (!boheAnchor) boheAnchor = todayStr().substring(0, 7) + '-01';
        var month = curMonth();
        $page.html(
            '<div class="bohe-head">' +
            '<div class="bohe-month"><span class="bohe-nav" data-bohe-prev>‹</span>' + month +
            '<span class="bohe-nav" data-bohe-next>›</span></div>' +
            '<div class="bohe-total" id="boheTotal">--</div>' +
            '<div class="bohe-total-sub">本月支出（元）</div>' +
            '</div>' + viewingBarHtml() +
            '<div class="bohe-entries">' +
            '<div class="bohe-entry" data-bohe-go="boheStats"><span>📊</span>统计</div>' +
            '<div class="bohe-entry" data-bohe-go="boheFriends"><span>🤝</span>关注</div>' +
            '</div>' +
            '<div class="h5-section-title">账单明细</div>' +
            '<div id="boheBillList" class="bohe-billlist"><div class="bohe-empty">加载中…</div></div>' +
            (BoheViewing.friendId ? '' : '<button class="bohe-fab" data-bohe-add>＋ 记一笔</button>')
        );
        bindViewingBack($page, function () { renderBohe($page); });

        $page.find('[data-bohe-prev]').one('click', function () { boheAnchor = shiftMonth(boheAnchor, -1); renderBohe($page); });
        $page.find('[data-bohe-next]').one('click', function () { boheAnchor = shiftMonth(boheAnchor, 1); renderBohe($page); });
        $page.find('[data-bohe-go]').one('click', function () { goPage($(this).data('boheGo')); });
        $page.find('[data-bohe-add]').one('click', function () { showAddSheet($page); });

        $.when(
            api({ url: 'bohe/bill/list?month=' + month + viewingParams(), type: 'GET' }),
            api({ url: 'bohe/stats?month=' + month + viewingParams(), type: 'GET' })
        ).done(function (billResp, statResp) {
            var br = billResp[0], sr = statResp[0];
            if (br.errorCode !== '000000') { $('#boheBillList').html('<div class="bohe-empty">' + esc(br.errorMsg || '获取账单失败') + '</div>'); return; }
            if (sr.errorCode === '000000' && sr.body) {
                $('#boheTotal').text('¥' + boheYuan(sr.body.total));
            }
            renderBillList(br.body && br.body.bills || []);
        }).fail(function () {
            $('#boheBillList').html('<div class="bohe-empty">网络错误</div>');
        });
    }

    function renderBillList(bills) {
        if (!bills.length) { $('#boheBillList').html('<div class="bohe-empty">本月还没有账单</div>'); return; }
        var html = '';
        bills.forEach(function (b) {
            var cat = boheCat(b.category);
            html += '<div class="bohe-bill">' +
                '<div class="bohe-bill-ico">' + cat.icon + '</div>' +
                '<div class="bohe-bill-main"><div class="bohe-bill-title">' + esc(b.note || cat.label) + '</div>' +
                '<div class="bohe-bill-sub">' + cat.label + ' · ' + esc(String(b.billDate || '').substring(0, 10)) + '</div></div>' +
                '<div class="bohe-bill-amt">-¥' + boheYuan(b.amount) + '</div>' +
                (BoheViewing.friendId ? '' : '<div class="bohe-bill-del" data-del-id="' + esc(b.id) + '">删</div>') +
                '</div>';
        });
        $('#boheBillList').html(html);
    }

    /* 删除（事件委托，一次绑定） */
    $(document).on('click', '#page-bohe [data-del-id]', function () {
        var id = $(this).data('delId');
        confirm('删除该笔账单？', function () {
            postFlat('bohe/bill/delete', { id: Number(id) }).done(function (res) {
                if (res.errorCode === '000000') { toast('已删除'); renderBohe($('#page-bohe')); }
                else toast(res.errorMsg || '删除失败');
            });
        });
    });

    /* ---------------- 记一笔抽屉 ---------------- */

    function showAddSheet($page) {
        var grid = '';
        BOHE_CATEGORIES.forEach(function (c, i) {
            grid += '<div class="bohe-cat' + (i === 0 ? ' on' : '') + '" data-cat="' + c.key + '">' +
                '<div class="bohe-cat-ico">' + c.icon + '</div>' + c.label + '</div>';
        });
        openSheet(
            '<h3 style="margin:0 0 12px">记一笔</h3>' +
            '<input class="h5-input bohe-amt" id="boheAmt" type="number" step="0.01" min="0" inputmode="decimal" placeholder="金额 ¥">' +
            '<div class="bohe-cats" id="boheCats">' + grid + '</div>' +
            '<input class="h5-input" id="boheNote" type="text" maxlength="50" placeholder="备注（可选）">' +
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
            postFlat('bohe/bill/save', {
                amount: amt, category: category,
                note: $('#boheNote').val() || null,
                date: $('#boheDate').val() || todayStr()
            }).done(function (res) {
                if (res.errorCode === '000000') { toast('已记一笔'); closeSheet(); renderBohe($page); }
                else toast(res.errorMsg || '记账失败');
            });
        });
    }

    /* ---------------- 统计页 ---------------- */

    var statsAnchor = null;    /* yyyy-MM-01 锚点 */
    var statsPeriod = 'month'; /* day | month | year */

    function periodValue() {
        var a = statsAnchor || todayStr();
        if (statsPeriod === 'day') return a.substring(0, 10);
        if (statsPeriod === 'year') return a.substring(0, 4);
        return a.substring(0, 7);
    }
    function periodLabel() {
        var v = periodValue();
        if (statsPeriod === 'year') return v + ' 年';
        if (statsPeriod === 'day') return v;
        return v.replace('-', ' 年 ') + ' 月';
    }
    function shiftPeriod(dir) {
        var a = statsAnchor || todayStr();
        var step = statsPeriod === 'day' ? 'day' : (statsPeriod === 'year' ? 'year' : 'month');
        var y = Number(a.substring(0, 4)), m = Number(a.substring(5, 7)) - 1, d = Number(a.substring(8, 10));
        if (step === 'month') { var dt = new Date(y, m + dir, 1); statsAnchor = dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-01'; }
        else if (step === 'year') { statsAnchor = (y + dir) + '-01-01'; }
        else { var dd = new Date(y, m, d + dir); statsAnchor = dd.getFullYear() + '-' + String(dd.getMonth() + 1).padStart(2, '0') + '-' + String(dd.getDate()).padStart(2, '0'); }
    }

    function renderBoheStats($page) {
        if (!statsAnchor) statsAnchor = todayStr().substring(0, 7) + '-01';
        var chips = '';
        [['day', '日'], ['month', '月'], ['year', '年']].forEach(function (p) {
            chips += '<span class="bohe-chip' + (statsPeriod === p[0] ? ' on' : '') + '" data-period="' + p[0] + '">' + p[1] + '</span>';
        });
        $page.html(
            '<div class="bohe-head">' +
            '<div class="bohe-month"><span class="bohe-nav" data-bohe-prev>‹</span>' +
            '<span id="boheStatsLabel">' + periodLabel() + '</span>' +
            '<span class="bohe-nav" data-bohe-next>›</span></div>' +
            '<div class="bohe-total" id="boheStatsTotal">--</div>' +
            '<div class="bohe-total-sub">支出合计（元）</div>' +
            '<div class="bohe-chips">' + chips + '</div>' +
            '</div>' + viewingBarHtml() +
            '<div class="h5-section-title">分类占比</div>' +
            '<div id="boheCatList" class="bohe-catlist"><div class="bohe-empty">加载中…</div></div>'
        );
        bindViewingBack($page, function () { renderBoheStats($page); });
        $page.find('.bohe-chip').one('click', function () { statsPeriod = $(this).data('period'); renderBoheStats($page); });
        $page.find('[data-bohe-prev]').one('click', function () { shiftPeriod(-1); renderBoheStats($page); });
        $page.find('[data-bohe-next]').one('click', function () { shiftPeriod(1); renderBoheStats($page); });

        api({ url: 'bohe/stats?month=' + periodValue() + '&period=' + statsPeriod + viewingParams(), type: 'GET' })
            .done(function (r) {
                if (r.errorCode !== '000000') { $('#boheCatList').html('<div class="bohe-empty">' + esc(r.errorMsg || '获取统计失败') + '</div>'); return; }
                var total = Number(r.body && r.body.total || 0);
                $('#boheStatsTotal').text('¥' + boheYuan(total));
                var cats = (r.body && r.body.byCategory) || [];
                if (!cats.length) { $('#boheCatList').html('<div class="bohe-empty">该时期还没有支出</div>'); return; }
                var html = '';
                cats.forEach(function (c) {
                    var cat = boheCat(c.category);
                    var pct = total > 0 ? Math.round(Number(c.total) / total * 100) : 0;
                    html += '<div class="bohe-catrow">' +
                        '<div class="bohe-catrow-head"><span>' + cat.icon + ' ' + cat.label + '</span>' +
                        '<span>¥' + boheYuan(c.total) + ' · ' + pct + '%</span></div>' +
                        '<div class="bohe-bar"><div class="bohe-bar-in" style="width:' + pct + '%"></div></div>' +
                        '</div>';
                });
                $('#boheCatList').html(html);
            })
            .fail(function () { $('#boheCatList').html('<div class="bohe-empty">网络错误</div>'); });
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
