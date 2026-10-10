/**
 * 巡检日报前端逻辑（PC）。
 * 资产卡片列表（状态色点 + last_detail）+ 添加资产（按类型动态字段）+ 日报设置。
 * 后端：patrol/list、patrol/add、patrol/delete、patrol/runNow、patrol/setting（Result 包裹）。
 */
var Patrol = (function () {
    var loaded = false;

    function esc(s) {
        if (!s) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function statusDot(s) {
        var cls = ['ok', 'warn', 'fail', 'unknown'].indexOf(s) >= 0 ? s : 'unknown';
        return '<span class="patrol-dot ' + cls + '"></span>';
    }

    function statusText(s) {
        return { ok: '正常', warn: '警告', fail: '异常', unknown: '未知' }[s] || '未巡检';
    }

    function typeLabel(t) {
        return { url: 'URL 存活', domain: '域名到期', server: '服务器资源' }[t] || t;
    }

    function loadAssets() {
        $.getJSON('patrol/list', function (res) {
            var list = (res && res.body) || [];
            if (!list.length) {
                $('#patrolAssetList').html('<div class="welfare-empty">还没有资产，在下方添加（URL / 域名 / 服务器）</div>');
                return;
            }
            var html = list.map(function (a) {
                return '<div class="welfare-card" style="cursor:default">' +
                    '<div style="display:flex;align-items:flex-start;gap:8px">' +
                    '<div style="flex:1;min-width:0">' +
                    '<div>' + statusDot(a.lastStatus) + ' <b>' + esc(a.name) + '</b>' +
                    ' <span class="welfare-date">' + typeLabel(a.assetType) + ' · ' + esc(a.target) +
                    (a.assetType === 'server' && a.port ? ' · 端口 ' + a.port : '') + '</span></div>' +
                    '<div class="welfare-date" style="margin-top:4px">' +
                    (a.lastStatus ? statusText(a.lastStatus) + ' · ' + esc(a.lastDetail || '') + ' · ' + esc(a.lastCheckTime || '')
                        : '尚未巡检，点上方「立即巡检」') + '</div>' +
                    '</div>' +
                    '<button class="btn-main" data-del="' + a.id + '" style="padding:3px 10px;font-size:12px;color:#f5576c;">删除</button>' +
                    '</div></div>';
            }).join('');
            $('#patrolAssetList').html(html);
        }).fail(function () {
            $('#patrolAssetList').html('<div class="welfare-empty">加载失败</div>');
        });
    }

    function loadSetting() {
        $.getJSON('patrol/setting', function (res) {
            var s = (res && res.body) || {};
            if (s.push_hour != null) $('#patrolHour').val(s.push_hour);
            if (s.channel) {
                $('#patrolChannel').val(s.channel);
                $('#patrolChannel').trigger('change');
                if (s.robot_id) $('#patrolRobot').val(s.robot_id);
                if (s.email) $('#patrolEmail').val(s.email);
            }
        });
    }

    function fillHours() {
        var html = '';
        for (var h = 0; h < 24; h++) html += '<option value="' + h + '">每天 ' + h + ' 点</option>';
        $('#patrolHour').html(html);
    }

    function fillRobots() {
        $.getJSON('robot/list').done(function (res) {
            var list = (res && res.body) || [];
            $('#patrolRobot').html(list.length
                ? list.map(function (r) { return '<option value="' + r.id + '">' + esc(r.name || ('机器人#' + r.id)) + '</option>'; }).join('')
                : '<option value="">（请先添加钉钉机器人）</option>');
        });
    }

    function syncPatrolChannelFields() {
        var isDing = $('#patrolChannel').val() === 'dingtalk';
        $('#patrolRobot').toggle(isDing);
        $('#patrolEmail').toggle(!isDing);
    }

    function runNow() {
        $('#patrolRunNow').text('巡检中…');
        $.ajax({
            url: 'patrol/runNow', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: {} }),
            success: function (res) {
                $('#patrolRunNow').text('▶ 立即巡检');
                if (res && res.errorCode === '000000') {
                    var r = res.body || {};
                    alterModal('巡检完成：异常 ' + (r.failCount || 0) + ' · 警告 ' + (r.warnCount || 0) + ' · 共 ' + ((r.results || []).length) + ' 项');
                    loadAssets();
                } else {
                    alterModal((res && res.errorMsg) || '巡检失败');
                }
            },
            error: function () { $('#patrolRunNow').text('▶ 立即巡检'); alterModal('巡检失败'); }
        });
    }

    function init() {
        if (loaded || !$('#patrolAssetList').length) return;
        loaded = true;
        fillHours();
        fillRobots();
        loadAssets();
        loadSetting();

        $('#patrolType').on('change', function () {
            $('#patrolServerFields').toggle($(this).val() === 'server');
        });
        $('#patrolChannel').on('change', function () {
            syncPatrolChannelFields();
        });
        // 初始渲染按当前通道显隐（修复：默认钉钉但机器人下拉藏着）
        syncPatrolChannelFields();

        $('#patrolAddBtn').off('click').on('click', function () {
            var payload = {
                name: $('#patrolName').val().trim(),
                assetType: $('#patrolType').val(),
                target: $('#patrolTarget').val().trim()
            };
            if (payload.assetType === 'server') {
                payload.port = $('#patrolPort').val();
                payload.sshUser = $('#patrolSshUser').val().trim();
                payload.sshPassword = $('#patrolSshPwd').val();
            }
            $.ajax({
                url: 'patrol/add', type: 'POST', contentType: 'application/json',
                data: JSON.stringify({ body: payload }),
                success: function (res) {
                    if (res && res.errorCode === '000000') {
                        alterModal('已添加');
                        $('#patrolName, #patrolTarget, #patrolPort, #patrolSshUser, #patrolSshPwd').val('');
                        loadAssets();
                    } else {
                        alterModal((res && res.errorMsg) || '添加失败');
                    }
                },
                error: function () { alterModal('添加失败'); }
            });
        });

        $('#patrolAssetList').off('click', '[data-del]').on('click', '[data-del]', function () {
            var id = $(this).data('del');
            $.ajax({
                url: 'patrol/delete', type: 'POST', contentType: 'application/json',
                data: JSON.stringify({ body: { id: id } }),
                success: function (res) { res && res.errorCode === '000000' ? loadAssets() : alterModal('删除失败'); }
            });
        });

        $('#patrolRunNow').off('click').on('click', runNow);

        $('#patrolSettingBtn').off('click').on('click', function () {
            $('#patrolSettingCard').toggle();
        });
        $('#patrolSaveSetting').off('click').on('click', function () {
            var payload = {
                pushHour: $('#patrolHour').val(),
                channel: $('#patrolChannel').val()
            };
            if (payload.channel === 'dingtalk') payload.robotId = $('#patrolRobot').val();
            else payload.email = $('#patrolEmail').val().trim();
            $.ajax({
                url: 'patrol/setting', type: 'POST', contentType: 'application/json',
                data: JSON.stringify({ body: payload }),
                success: function (res) { alterModal(res && res.errorCode === '000000' ? '设置已保存' : ((res && res.errorMsg) || '保存失败')); }
            });
        });
        $('#patrolTestPush').off('click').on('click', function () {
            alterModal('巡检并发送中…');
            $.ajax({
                url: 'patrol/testPush', type: 'POST', contentType: 'application/json',
                data: JSON.stringify({ body: {} }),
                success: function (res) {
                    if (res && res.errorCode === '000000') alterModal(res.body);
                    else alterModal((res && res.errorMsg) || '测试失败');
                },
                error: function () { alterModal('测试失败'); }
            });
        });
    }

    return { init: init };
})();
