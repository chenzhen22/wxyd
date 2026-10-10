/* ============================================================
   巡检日报（H5 移动端）
   数据：/api/patrol/list、/api/patrol/add、/api/patrol/runNow、/api/patrol/setting
   注册：registerH5Module('patrol', renderPatrol)
   依赖：h5.js 的 esc / api / postJSON / toast / loading / openSheet
   ============================================================ */
(function () {

    var STATUS_TXT = { ok: '正常', warn: '警告', fail: '异常', unknown: '未知' };
    var STATUS_COLOR = { ok: '#43e97b', warn: '#e0a800', fail: '#f5576c', unknown: '#888780' };
    var TYPE_LABEL = { url: 'URL 存活', domain: '域名到期', server: '服务器资源' };

    function statusBadge(s) {
        var key = STATUS_TXT[s] ? s : 'unknown';
        return '<span class="h5-badge" style="background:' + key + '22;color:' + STATUS_COLOR[key] + '">' +
            (STATUS_TXT[key] || '未巡检') + '</span>';
    }

    function renderPatrol($page) {
        $page.html(
            '<div class="h5-section-title">🛡️ 巡检日报' +
            '<div>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5PatrolRun">▶ 立即巡检</span>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5PatrolSettingBtn">⚙ 设置</span>' +
            '</div></div>' +
            '<div id="h5PatrolSetting" style="display:none;margin-bottom:10px"></div>' +
            '<div id="h5PatrolList"><div class="h5-empty">加载中…</div></div>'
        );
        loadAssetsH5();
        loadSettingH5();

        $page.off('click').on('click', '#h5PatrolRun', function () {
            loading(true);
            postJSON('patrol/runNow', {}).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    var r = res.body || {};
                    toast('巡检完成：异常 ' + (r.failCount || 0) + ' · 警告 ' + (r.warnCount || 0));
                    loadAssetsH5();
                } else toast(res.errorMsg || '巡检失败');
            }).fail(function () { loading(false); toast('请求失败'); });
        });

        $page.on('click', '#h5PatrolSettingBtn', function () {
            var $s = $('#h5PatrolSetting');
            $s.toggle();
            if ($s.is(':visible')) loadSettingH5(true);
        });
    }

    function loadAssetsH5() {
        api({ url: 'patrol/list', type: 'GET' }).done(function (res) {
            var list = (res.errorCode === '000000' && res.body) || [];
            if (!list.length) {
                $('#h5PatrolList').html('<div class="h5-empty">还没有资产，点下方 ＋ 添加</div>');
                setFab(true, openAddSheetH5);
                return;
            }
            var html = list.map(function (a) {
                return '<div class="h5-card"><div class="h5-card-row"><div style="flex:1;min-width:0">' +
                    '<div class="h5-card-title">' + esc(a.name) + '</div>' +
                    '<div class="h5-card-sub">' + (TYPE_LABEL[a.assetType] || a.assetType) + ' · ' + esc(a.target) + '</div>' +
                    (a.lastStatus
                        ? '<div class="h5-card-sub">' + (STATUS_TXT[a.lastStatus] || '未知') + ' · ' + esc(a.lastDetail || '') + ' · ' + esc(a.lastCheckTime || '') + '</div>'
                        : '<div class="h5-card-sub">尚未巡检</div>') +
                    '</div><div style="display:flex;flex-direction:column;gap:4px">' +
                    statusBadge(a.lastStatus) +
                    '<button class="h5-btn-danger h5-btn-sm" data-del="' + a.id + '">删</button>' +
                    '</div></div></div>';
            }).join('');
            $('#h5PatrolList').html(html);
            setFab(true, openAddSheetH5);
            $('#h5PatrolList').off('click', '[data-del]').on('click', '[data-del]', function () {
                postJSON('patrol/delete', { id: $(this).data('del') }).done(function (r) {
                    r.errorCode === '000000' ? loadAssetsH5() : toast(r.errorMsg || '删除失败');
                });
            });
        }).fail(function () {
            $('#h5PatrolList').html('<div class="h5-empty">加载失败</div>');
        });
    }

    function openAddSheetH5() {
        var hours = '';
        for (var h = 0; h < 24; h++) hours += '<option value="' + h + '">每天 ' + h + ' 点</option>';
        openSheet('<div class="h5-sheet-head"><h3>＋ 添加巡检资产</h3></div>' +
            '<div class="h5-field"><label>资产名</label><input class="h5-input" id="ptName" maxlength="64" placeholder="如：生产库 MySQL"></div>' +
            '<div class="h5-field"><label>类型</label><select class="h5-select" id="ptType">' +
            '<option value="url">URL 存活</option><option value="domain">域名到期</option>' +
            '<option value="server">服务器资源（SSH）</option></select></div>' +
            '<div class="h5-field"><label>探测目标</label><input class="h5-input" id="ptTarget" placeholder="https://… 或 example.com 或主机 IP"></div>' +
            '<div id="ptServerFields" style="display:none">' +
            '<div class="h5-field"><label>SSH 端口</label><input class="h5-input" id="ptPort" type="number" value="22"></div>' +
            '<div class="h5-field"><label>SSH 用户</label><input class="h5-input" id="ptUser"></div>' +
            '<div class="h5-field"><label>SSH 密码（SM4 加密存储）</label><input class="h5-input" id="ptPwd" type="password"></div>' +
            '</div>' +
            '<div class="h5-field"><label>日报推送时间</label><select class="h5-select" id="ptHour">' + hours + '</select></div>' +
            '<div class="h5-field"><label>推送通道</label><select class="h5-select" id="ptChannel">' +
            '<option value="dingtalk">钉钉推送</option><option value="email">邮件推送</option></select></div>' +
            '<div class="h5-field" id="ptRobotField"><label>钉钉机器人</label><select class="h5-select" id="ptRobot"></select></div>' +
            '<div class="h5-field" id="ptEmailField" style="display:none"><label>接收邮箱</label><input class="h5-input" id="ptEmail"></div>' +
            '<button class="h5-btn-primary h5-btn-block" id="ptAdd">保存</button>' +
            '<button class="h5-btn h5-btn-block" id="ptTestPush" style="color:#38bdf8">发送测试日报</button>');
        api({ url: 'robot/list', type: 'GET' }).done(function (r) {
            var robots = (r && r.body) || [];
            $('#ptRobot').html(robots.length
                ? robots.map(function (x) { return '<option value="' + x.id + '">' + esc(x.name || ('机器人#' + x.id)) + '</option>'; }).join('')
                : '<option value="">（请先到「钉钉机器人」添加）</option>');
        });
        $('#ptType').off('change').on('change', function () {
            $('#ptServerFields').toggle($(this).val() === 'server');
        });
        $('#ptChannel').off('change').on('change', function () {
            var isDing = $(this).val() === 'dingtalk';
            $('#ptRobotField').toggle(isDing);
            $('#ptEmailField').toggle(!isDing);
        });
        $('#ptAdd').off('click').on('click', function () {
            var payload = {
                name: $('#ptName').val().trim(),
                assetType: $('#ptType').val(),
                target: $('#ptTarget').val().trim(),
                pushHour: Number($('#ptHour').val()),
                channel: $('#ptChannel').val()
            };
            if (payload.assetType === 'server') {
                payload.port = Number($('#ptPort').val()) || 22;
                payload.sshUser = $('#ptUser').val().trim();
                payload.sshPassword = $('#ptPwd').val();
            }
            if (payload.channel === 'dingtalk') payload.robotId = $('#ptRobot').val();
            else payload.email = $('#ptEmail').val().trim();
            postJSON('patrol/add', payload).done(function (res) {
                if (res.errorCode === '000000') { closeSheet(); loadAssetsH5(); toast('已添加'); }
                else toast(res.errorMsg || '保存失败');
            }).fail(function () { toast('保存失败'); });
            /* 设置与资产同表单提交：日报时间/通道单独存 */
            postJSON('patrol/setting', {
                pushHour: payload.pushHour, channel: payload.channel,
                robotId: payload.robotId, email: payload.email
            });
        });
        $('#ptTestPush').off('click').on('click', function () {
            toast('巡检并发送中…');
            postJSON('patrol/testPush', {}).done(function (res) {
                res.errorCode === '000000' ? toast(res.body) : toast(res.errorMsg || '测试失败');
            }).fail(function () { toast('测试失败'); });
        });
    }

    function loadSettingH5() { /* 设置并入添加抽屉，此处占位保持结构清晰 */ }

    window.registerH5Module('patrol', renderPatrol);
})();
