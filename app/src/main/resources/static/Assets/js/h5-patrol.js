/* ============================================================
   巡检日报（H5 移动端）—— 零配置系统自检
   数据：/api/patrol/status、/api/patrol/runNow、/api/patrol/setting、/api/patrol/testPush
   注册：registerH5Module('patrol', renderPatrol)
   依赖：h5.js 的 esc / api / postJSON / toast / loading / openSheet / setFab
   ============================================================ */
(function () {

    var STATUS_TXT = { ok: '正常', warn: '警告', fail: '异常', unknown: '未知' };
    var STATUS_COLOR = { ok: '#43e97b', warn: '#e0a800', fail: '#f5576c', unknown: '#888780' };

    function statusBadge(s) {
        var key = STATUS_TXT[s] ? s : 'unknown';
        return '<span class="h5-badge" style="background:' + key + '22;color:' + STATUS_COLOR[key] + '">' +
            (STATUS_TXT[key] || '未知') + '</span>';
    }

    function renderPatrol($page) {
        setFab(false);
        $page.html(
            '<div class="h5-section-title">🛡️ 巡检日报' +
            '<div>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5PatrolRun">▶ 立即巡检</span>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5PatrolSettingBtn">⚙ 设置</span>' +
            '</div></div>' +
            '<div id="h5PatrolSetting" style="display:none;margin-bottom:10px"></div>' +
            '<div id="h5PatrolList"><div class="h5-empty">加载中…</div></div>'
        );
        loadReportH5();

        $page.off('click').on('click', '#h5PatrolRun', function () {
            loading(true);
            postJSON('patrol/runNow', {}).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    var r = res.body || {};
                    toast('巡检完成：异常 ' + (r.failCount || 0) + ' · 警告 ' + (r.warnCount || 0));
                    renderReportH5(r);
                } else toast(res.errorMsg || '巡检失败');
            }).fail(function () { loading(false); toast('请求失败'); });
        });

        $page.on('click', '#h5PatrolSettingBtn', function () {
            openSheetH5();
        });
    }

    function renderReportH5(r) {
        var list = (r && r.results) || [];
        if (!list.length) {
            $('#h5PatrolList').html('<div class="h5-empty">尚未巡检，点上方「立即巡检」</div>');
            return;
        }
        var html = '';
        if (r.lastRunTime) {
            html += '<div class="h5-card-sub" style="margin-bottom:6px">巡检时间：' + esc(r.lastRunTime) +
                ' · 异常 ' + (r.failCount || 0) + ' · 警告 ' + (r.warnCount || 0) + '</div>';
        }
        html += list.map(function (it) {
            return '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + esc(it.name) + '</div>' +
                '<div class="h5-card-sub">' + esc(it.detail || '') + '</div>' +
                '</div>' + statusBadge(it.status) + '</div></div>';
        }).join('');
        $('#h5PatrolList').html(html);
    }

    function loadReportH5() {
        api({ url: 'patrol/status', type: 'GET' }).done(function (res) {
            if (res.errorCode === '000000') renderReportH5(res.body);
            else $('#h5PatrolList').html('<div class="h5-empty">加载失败</div>');
        }).fail(function () {
            $('#h5PatrolList').html('<div class="h5-empty">加载失败</div>');
        });
    }

    function openSheetH5() {
        var hours = '';
        for (var h = 0; h < 24; h++) hours += '<option value="' + h + '">每天 ' + h + ' 点</option>';
        openSheet('<div class="h5-sheet-head"><h3>⚙ 日报设置</h3></div>' +
            '<div class="h5-field"><label>日报推送时间</label><select class="h5-select" id="ptHour">' + hours + '</select></div>' +
            '<div class="h5-field"><label>推送通道</label><select class="h5-select" id="ptChannel">' +
            '<option value="dingtalk">钉钉推送</option><option value="email">邮件推送</option></select></div>' +
            '<div class="h5-field" id="ptRobotField"><label>钉钉机器人</label><select class="h5-select" id="ptRobot"></select></div>' +
            '<div class="h5-field" id="ptEmailField" style="display:none"><label>接收邮箱</label><input class="h5-input" id="ptEmail"></div>' +
            '<button class="h5-btn-primary h5-btn-block" id="ptSave">保存设置</button>' +
            '<button class="h5-btn h5-btn-block" id="ptTestPush" style="color:#38bdf8">发送测试日报</button>');
        api({ url: 'robot/list', type: 'GET' }).done(function (r) {
            var robots = (r && r.body) || [];
            $('#ptRobot').html(robots.length
                ? robots.map(function (x) { return '<option value="' + x.id + '">' + esc(x.name || ('机器人#' + x.id)) + '</option>'; }).join('')
                : '<option value="">（请先到「钉钉机器人」添加）</option>');
        });
        /* 回显当前设置 */
        api({ url: 'patrol/setting', type: 'GET' }).done(function (r) {
            var s = (r && r.body) || {};
            if (s.push_hour != null) $('#ptHour').val(s.push_hour);
            if (s.channel) {
                $('#ptChannel').val(s.channel);
                $('#ptChannel').trigger('change');
                if (s.robot_id) $('#ptRobot').val(s.robot_id);
                if (s.email) $('#ptEmail').val(s.email);
            }
        });
        $('#ptChannel').off('change').on('change', function () {
            var isDing = $(this).val() === 'dingtalk';
            $('#ptRobotField').toggle(isDing);
            $('#ptEmailField').toggle(!isDing);
        });
        $('#ptSave').off('click').on('click', function () {
            var payload = {
                pushHour: Number($('#ptHour').val()),
                channel: $('#ptChannel').val()
            };
            if (payload.channel === 'dingtalk') payload.robotId = $('#ptRobot').val();
            else payload.email = $('#ptEmail').val().trim();
            postJSON('patrol/setting', payload).done(function (res) {
                res.errorCode === '000000' ? toast('设置已保存') : toast(res.errorMsg || '保存失败');
            }).fail(function () { toast('保存失败'); });
        });
        $('#ptTestPush').off('click').on('click', function () {
            toast('巡检并发送中…');
            postJSON('patrol/testPush', {}).done(function (res) {
                res.errorCode === '000000' ? toast(res.body) : toast(res.errorMsg || '测试失败');
            }).fail(function () { toast('测试失败'); });
        });
    }

    window.registerH5Module('patrol', renderPatrol);
})();