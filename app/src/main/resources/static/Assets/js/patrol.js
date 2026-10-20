/**
 * 巡检日报前端逻辑（PC）。
 * 零配置系统自检：固定四项（远程 MySQL / 应用自身 / 磁盘水位 / 推送通道）结果展示 + 日报设置。
 * 后端：patrol/status（最近结果）、patrol/runNow（即时巡检）、patrol/setting、patrol/testPush（Result 包裹）。
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
        return { ok: '正常', warn: '警告', fail: '异常', unknown: '未知' }[s] || '未知';
    }

    function renderReport(r) {
        var list = (r && r.results) || [];
        if (!list.length) {
            $('#patrolReportList').html('<div class="welfare-empty">尚未巡检，点上方「立即巡检」</div>');
            return;
        }
        var html = '';
        if (r.lastRunTime) {
            html += '<div class="welfare-date" style="margin:6px 2px">巡检时间：' + esc(r.lastRunTime) +
                ' · 异常 ' + (r.failCount || 0) + ' · 警告 ' + (r.warnCount || 0) + '</div>';
        }
        html += list.map(function (it) {
            return '<div class="welfare-card" style="cursor:default">' +
                '<div>' + statusDot(it.status) + ' <b>' + esc(it.name) + '</b>' +
                ' <span class="welfare-date">' + statusText(it.status) + '</span></div>' +
                '<div class="welfare-date" style="margin-top:4px">' + esc(it.detail || '') + '</div>' +
                '</div>';
        }).join('');
        $('#patrolReportList').html(html);
    }

    function loadReport() {
        $.getJSON('patrol/status', function (res) {
            if (res && res.errorCode === '000000') renderReport(res.body);
            else $('#patrolReportList').html('<div class="welfare-empty">加载失败</div>');
        }).fail(function () {
            $('#patrolReportList').html('<div class="welfare-empty">加载失败</div>');
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
                    renderReport(r);
                } else {
                    alterModal((res && res.errorMsg) || '巡检失败');
                }
            },
            error: function () { $('#patrolRunNow').text('▶ 立即巡检'); alterModal('巡检失败'); }
        });
    }

    function init() {
        if (loaded || !$('#patrolReportList').length) return;
        loaded = true;
        fillHours();
        fillRobots();
        loadReport();
        loadSetting();

        $('#patrolChannel').on('change', function () {
            syncPatrolChannelFields();
        });
        // 初始渲染按当前通道显隐（修复：默认钉钉但机器人下拉藏着）
        syncPatrolChannelFields();

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