/* ============================================================
   金融日报（PC 桌面版）
   数据：/api/dayinfo/list、/api/dayinfo/latest、/api/dayinfo/detail（登录可看）
         /api/dayinfo/subscribe/*（每用户一条订阅提醒）
   纯展示：按日期选择查看，md 渲染复用 welfare-md.js（window.WelfareMd）
   注册：index.js showPage 懒加载 window.DayInfo.init()
   ============================================================ */
(function () {

    var state = { initialized: false, list: [], currentId: null, subLoaded: false };

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function postJSON(url, body) {
        return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: body || {} }) });
    }

    function showArticle(info) {
        if (!info || !info.id) {
            $('#dayinfoArticle').hide();
            $('#dayinfoEmpty').show();
            return;
        }
        var brief = (info.brief || '').trim();
        $('#dayinfoEmpty').hide();
        $('#dayinfoArticle').show().html(
            '<h1 class="wf-title">' + esc(info.title) + '</h1>' +
            '<div class="wf-meta">' + esc(info.infoDate || '') +
            (info.updateTime ? ' · 更新于 ' + esc(info.updateTime) : '') + '</div>' +
            (brief ? '<div style="padding:10px 14px;margin-bottom:14px;border-left:3px solid var(--accent);' +
                'background:var(--glass-2);border-radius:8px;color:var(--text-2);font-size:13px;line-height:1.7;">' +
                '📌 今日要点：' + esc(brief) + '</div>' : '') +
            '<div class="wf-body">' + window.WelfareMd.render(info.content) + '</div>');
    }

    function loadDetail(id) {
        state.currentId = id;
        $('#dayinfoArticle').html('<div class="welfare-empty">加载中…</div>').show();
        $.getJSON('dayinfo/detail', { id: id }).done(function (res) {
            if (res.errorCode !== '000000' || !res.body) {
                $('#dayinfoArticle').html('<div class="welfare-empty">' + esc(res.errorMsg || '加载失败') + '</div>');
                return;
            }
            showArticle(res.body);
        }).fail(function () {
            $('#dayinfoArticle').html('<div class="welfare-empty">加载失败，请稍后重试</div>');
        });
    }

    function loadList(preferId) {
        $.getJSON('dayinfo/list', { pageNum: 1, pageSize: 50 }).done(function (res) {
            var b = (res.errorCode === '000000' && res.body) || {};
            state.list = b.list || [];
            var $sel = $('#dayinfoDateSel').html('');
            if (!state.list.length) {
                $('#dayinfoArticle').hide();
                $('#dayinfoEmpty').show();
                return;
            }
            state.list.forEach(function (d) {
                $sel.append('<option value="' + d.id + '">' + esc(d.infoDate) + '</option>');
            });
            var target = preferId;
            if (!target) {
                // 默认最新一期（列表按日期倒序，首条即最新）
                target = state.list[0].id;
            }
            $sel.val(String(target));
            loadDetail(target);
        }).fail(function () {
            $('#dayinfoArticle').html('<div class="welfare-empty">加载失败，请稍后重试</div>').show();
        });
    }

    /* ================= 订阅提醒（一人多条：钉钉机器人/邮箱可配多个） ================= */

    var robotNames = {}; // robotId -> name，展示用

    function loadRobots() {
        return $.getJSON('robot/list').then(function (r) {
            var list = (r && r.body) || [];
            var html = '';
            robotNames = {};
            list.forEach(function (rb) {
                robotNames[rb.id] = rb.name || ('机器人 #' + rb.id);
                html += '<option value="' + rb.id + '">' + esc(robotNames[rb.id]) + '</option>';
            });
            $('#dayinfoSubRobot').html(html);
        });
    }

    function subTargetHtml(s) {
        return s.channel === 'dingtalk'
            ? '🤖 ' + esc(robotNames[s.robotId] || ('机器人 #' + s.robotId))
            : '✉️ ' + esc(s.email || '');
    }

    function renderSubs(list) {
        var $tb = $('#dayinfoSubTableBody').html('');
        $('#dayinfoSubTable').toggle(list.length > 0);
        $('#dayinfoSubEmpty').toggle(list.length === 0);
        list.forEach(function (s) {
            $tb.append(
                '<tr>' +
                '<td>' + (s.channel === 'dingtalk' ? '钉钉' : '邮件') + '</td>' +
                '<td>' + subTargetHtml(s) + '</td>' +
                '<td>' + (s.enabled == 1 ? '<span style="color:#4ade80;">已启用</span>' : '<span style="color:#f87171;">已暂停</span>') + '</td>' +
                '<td>' +
                '<button class="btn-main" data-op="test" data-id="' + s.id + '" style="color:#38bdf8;">测试推送</button> ' +
                '<button class="btn-main" data-op="toggle" data-id="' + s.id + '" data-on="' + s.enabled + '" style="background:var(--glass-3);color:var(--text-1);">' + (s.enabled == 1 ? '暂停' : '恢复') + '</button> ' +
                '<button class="btn-del" data-op="del" data-id="' + s.id + '">删除</button>' +
                '</td></tr>');
        });
    }

    function loadSubs() {
        $.getJSON('dayinfo/subscribe/my').done(function (res) {
            renderSubs((res.errorCode === '000000' && res.body) || []);
        });
    }

    function init() {
        if (state.initialized) return;
        state.initialized = true;

        // 日期切换
        $('#dayinfoDateSel').on('change', function () {
            var id = $(this).val();
            if (id) loadDetail(id);
        });

        // 订阅卡片显隐
        $('#dayinfoSubBtn').on('click', function () {
            var $c = $('#dayinfoSubCard');
            $c.toggle();
            if ($c.is(':visible')) {
                loadRobots().then(loadSubs);
            }
        });

        // 渠道切换：钉钉选机器人 / 邮件填邮箱
        $('#dayinfoSubChannel').on('change', function () {
            var isDing = $(this).val() === 'dingtalk';
            $('#dayinfoSubRobot').toggle(isDing);
            $('#dayinfoSubEmail').toggle(!isDing);
        });

        // 添加订阅
        $('#dayinfoSubAdd').on('click', function () {
            var payload = { channel: $('#dayinfoSubChannel').val() };
            if (payload.channel === 'dingtalk') {
                payload.robotId = $('#dayinfoSubRobot').val();
                if (!payload.robotId) { alterModal('请先在「钉钉机器人」中添加机器人'); return; }
            } else {
                payload.email = $('#dayinfoSubEmail').val().trim();
                if (!payload.email) { alterModal('请填写接收邮箱'); return; }
            }
            loading(true);
            postJSON('dayinfo/subscribe/add', payload).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    $('#dayinfoSubEmail').val('');
                    loadSubs();
                } else {
                    alterModal(res.errorMsg || '添加失败');
                }
            }).fail(function () { loading(false); });
        });

        // 订阅行操作：测试 / 开关 / 删除
        $('#dayinfoSubTableBody').on('click', '[data-op]', function () {
            var $b = $(this), id = $b.data('id'), op = $b.data('op');
            if (op === 'test') {
                loading(true);
                postJSON('dayinfo/subscribe/test', { id: id }).done(function (res) {
                    loading(false);
                    alterModal(res.errorCode === '000000' ? (res.body || '推送成功，请查收') : (res.errorMsg || '测试失败'));
                }).fail(function () { loading(false); });
            } else if (op === 'toggle') {
                loading(true);
                postJSON('dayinfo/subscribe/toggle', { id: id, enabled: $b.data('on') != 1 }).done(function (res) {
                    loading(false);
                    if (res.errorCode === '000000') loadSubs();
                    else alterModal(res.errorMsg || '操作失败');
                }).fail(function () { loading(false); });
            } else if (op === 'del') {
                confirmModal('确定删除该订阅？', function () {
                    loading(true);
                    postJSON('dayinfo/subscribe/delete', { id: id }).done(function (res) {
                        loading(false);
                        if (res.errorCode === '000000') loadSubs();
                        else alterModal(res.errorMsg || '删除失败');
                    }).fail(function () { loading(false); });
                });
            }
        });

        loadList();
    }

    window.DayInfo = { init: init };
})();
