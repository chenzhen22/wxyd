/* ============================================================
   金融日报（H5 移动端）
   数据：/api/dayinfo/list、/api/dayinfo/detail、/api/dayinfo/subscribe/*
   注册：registerH5Module('dayinfo', renderDayInfo)
   依赖：h5.js 的 esc / api / postJSON / toast / loading
         Assets/js/welfare-md.js 的 window.WelfareMd
   ============================================================ */
(function () {

    var PAGE_SIZE = 50;

    function renderDayInfo($page) {
        $page.html(
            '<div class="h5-section-title">金融日报</div>' +
            '<div style="display:flex;gap:8px;align-items:center;margin-bottom:10px;">' +
            '<select id="h5DayinfoDateSel" style="flex:1;padding:10px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);"></select>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5DayinfoSubBtn">🔔 订阅</span>' +
            '</div>' +
            '<div id="h5DayinfoSub" style="display:none;margin-bottom:12px;padding:12px;border-radius:12px;background:var(--glass-2);border:1px solid var(--glass-5);">' +
            '<div style="display:flex;gap:8px;align-items:center;margin-bottom:8px;">' +
            '<select id="h5DayinfoSubChannel" style="flex:1;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' +
            '<option value="dingtalk">钉钉机器人</option><option value="email">邮件</option></select>' +
            '<select id="h5DayinfoSubRobot" style="flex:1;display:none;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);"></select>' +
            '<input id="h5DayinfoSubEmail" placeholder="接收邮箱" style="flex:1;display:none;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5DayinfoSubAdd">添加</span>' +
            '</div>' +
            '<div id="h5DayinfoSubList"></div>' +
            '<div class="h5-empty" id="h5DayinfoSubEmpty" style="display:none;">还没有订阅</div></div>' +
            '<div class="wf-body" id="h5DayinfoArticle" style="padding:12px;border-radius:12px;background:var(--glass-2);border:1px solid var(--glass-5);"><div class="h5-empty">加载中…</div></div>' +
            '<div class="h5-empty" id="h5DayinfoEmpty" style="display:none">暂无日报</div>');

        bindChannelToggle();
        bindSubscribe();

        $.getJSON('dayinfo/list', { pageNum: 1, pageSize: PAGE_SIZE }).done(function (res) {
            var b = (res.errorCode === '000000' && res.body) || {};
            var list = b.list || [];
            if (!list.length) {
                $('#h5DayinfoArticle').hide();
                $('#h5DayinfoEmpty').show();
                return;
            }
            var $sel = $('#h5DayinfoDateSel').html('');
            list.forEach(function (d) {
                $sel.append('<option value="' + d.id + '">' + esc(d.infoDate) + '</option>');
            });
            $sel.val(String(list[0].id)); // 倒序，首条即最新
            loadDetail(list[0].id);
            $sel.off('change').on('change', function () {
                if (this.value) loadDetail(this.value);
            });
        }).fail(function () {
            $('#h5DayinfoArticle').html('<div class="h5-empty">加载失败</div>');
        });

        $('#h5DayinfoSubBtn').off('click').on('click', function () {
            var $s = $('#h5DayinfoSub');
            $s.toggle();
            if ($s.is(':visible')) loadRobots().then(loadSubs);
        });
    }

    function loadDetail(id) {
        loading(true);
        $.getJSON('dayinfo/detail', { id: id }).done(function (res) {
            loading(false);
            if (res.errorCode !== '000000' || !res.body) {
                $('#h5DayinfoArticle').html('<div class="h5-empty">' + esc(res.errorMsg || '加载失败') + '</div>');
                return;
            }
            var d = res.body;
            var brief = (d.brief || '').trim();
            $('#h5DayinfoEmpty').hide();
            $('#h5DayinfoArticle').show().html(
                '<h2 style="margin:0 0 6px;color:var(--title);word-break:break-all;">' + esc(d.title) + '</h2>' +
                '<div style="font-size:12px;color:var(--text-5);margin-bottom:12px;">' + esc(d.infoDate || '') + '</div>' +
                (brief ? '<div style="padding:10px 12px;margin-bottom:12px;border-left:3px solid var(--accent);' +
                    'background:var(--glass-3);border-radius:8px;color:var(--text-2);font-size:13px;line-height:1.7;">' +
                    '📌 今日要点：' + esc(brief) + '</div>' : '') +
                window.WelfareMd.render(d.content));
        }).fail(function () {
            loading(false);
            $('#h5DayinfoArticle').html('<div class="h5-empty">加载失败</div>');
        });
    }

    function bindChannelToggle() {
        $('#h5DayinfoSubChannel').off('change').on('change', function () {
            var isDing = $(this).val() === 'dingtalk';
            $('#h5DayinfoSubRobot').toggle(isDing);
            $('#h5DayinfoSubEmail').toggle(!isDing);
        });
    }

    var robotNames = {}; // robotId -> name，展示用

    function loadRobots() {
        return api({ url: 'robot/list', type: 'GET' }).then(function (r) {
            var list = (r && r.body) || [];
            var html = '';
            robotNames = {};
            list.forEach(function (rb) {
                robotNames[rb.id] = rb.name || ('机器人 #' + rb.id);
                html += '<option value="' + rb.id + '">' + esc(robotNames[rb.id]) + '</option>';
            });
            $('#h5DayinfoSubRobot').html(html);
        });
    }

    function subTargetHtml(s) {
        return s.channel === 'dingtalk'
            ? '🤖 ' + esc(robotNames[s.robotId] || ('机器人 #' + s.robotId))
            : '✉️ ' + esc(s.email || '');
    }

    function renderSubs(list) {
        var $l = $('#h5DayinfoSubList').html('');
        $('#h5DayinfoSubList').toggle(list.length > 0);
        $('#h5DayinfoSubEmpty').toggle(list.length === 0);
        list.forEach(function (s) {
            $l.append(
                '<div style="display:flex;gap:6px;align-items:center;padding:6px 0;border-bottom:1px solid var(--glass-5);">' +
                '<div style="flex:1;min-width:0;font-size:13px;word-break:break-all;">' + subTargetHtml(s) +
                (s.enabled == 1 ? ' <span style="color:#4ade80;">·启用</span>' : ' <span style="color:#f87171;">·暂停</span>') + '</div>' +
                '<span class="h5-btn-ghost h5-btn-sm" data-op="test" data-id="' + s.id + '" style="color:#38bdf8;">测试</span>' +
                '<span class="h5-btn-ghost h5-btn-sm" data-op="toggle" data-id="' + s.id + '" data-on="' + s.enabled + '">' + (s.enabled == 1 ? '暂停' : '恢复') + '</span>' +
                '<span class="h5-btn-ghost h5-btn-sm" data-op="del" data-id="' + s.id + '" style="color:#f87171;">删除</span>' +
                '</div>');
        });
    }

    function loadSubs() {
        api({ url: 'dayinfo/subscribe/my', type: 'GET' }).done(function (res) {
            renderSubs((res.errorCode === '000000' && res.body) || []);
        });
    }

    function bindSubscribe() {
        $('#h5DayinfoSubAdd').off('click').on('click', function () {
            var payload = { channel: $('#h5DayinfoSubChannel').val() };
            if (payload.channel === 'dingtalk') {
                payload.robotId = $('#h5DayinfoSubRobot').val();
                if (!payload.robotId) { toast('请先添加钉钉机器人'); return; }
            } else {
                payload.email = $('#h5DayinfoSubEmail').val().trim();
                if (!payload.email) { toast('请填写接收邮箱'); return; }
            }
            loading(true);
            postJSON('dayinfo/subscribe/add', payload).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    $('#h5DayinfoSubEmail').val('');
                    loadSubs();
                } else {
                    toast(res.errorMsg || '添加失败');
                }
            }).fail(function () { loading(false); });
        });

        $('#h5DayinfoSubList').off('click', '[data-op]').on('click', '[data-op]', function () {
            var $b = $(this), id = $b.data('id'), op = $b.data('op');
            if (op === 'test') {
                loading(true);
                postJSON('dayinfo/subscribe/test', { id: id }).done(function (res) {
                    loading(false);
                    toast(res.errorCode === '000000' ? (res.body || '推送成功，请查收') : (res.errorMsg || '测试失败'));
                }).fail(function () { loading(false); });
            } else if (op === 'toggle') {
                loading(true);
                postJSON('dayinfo/subscribe/toggle', { id: id, enabled: $b.data('on') != 1 }).done(function (res) {
                    loading(false);
                    if (res.errorCode === '000000') loadSubs();
                    else toast(res.errorMsg || '操作失败');
                }).fail(function () { loading(false); });
            } else if (op === 'del') {
                confirm('确定删除该订阅？', function () {
                    loading(true);
                    postJSON('dayinfo/subscribe/delete', { id: id }).done(function (res) {
                        loading(false);
                        if (res.errorCode === '000000') loadSubs();
                        else toast(res.errorMsg || '删除失败');
                    }).fail(function () { loading(false); });
                });
            }
        });
    }

    window.registerH5Module('dayinfo', renderDayInfo);
})();
