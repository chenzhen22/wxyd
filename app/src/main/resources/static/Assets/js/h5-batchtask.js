/**
 * 批次任务（H5，仅管理员）：卡片列表 + 执行 / 编辑 / 日志。
 * 后端：batch/list、batch/add、batch/save、batch/run、batch/log、batch/preview。
 */
(function () {

    var rows = [];

    function statusHtml(t) {
        if (t.enabled !== 1) return '<span style="color:var(--text-5);">⏸ 已停用</span>';
        switch (t.lastStatus) {
            case 'running': return '<span style="color:#38bdf8;">运行中</span>';
            case 'ok': return '<span style="color:#4ade80;">🟢 成功</span>';
            case 'fail': return '<span style="color:#f87171;">🔴 失败' + (t.lastExitCode != null ? ' (' + t.lastExitCode + ')' : '') + '</span>';
            case 'timeout': return '<span style="color:#e0a800;">🟠 超时</span>';
            default: return '<span style="color:var(--text-5);">未执行</span>';
        }
    }

    function row(label, value) {
        return '<div style="display:flex;gap:8px;font-size:12px;line-height:1.9;">' +
            '<span style="color:var(--text-5);flex:0 0 52px;">' + label + '</span>' +
            '<span style="color:var(--text-2);word-break:break-all;">' + value + '</span></div>';
    }

    function renderList() {
        var $l = $('#h5BtList').html('');
        if (!rows.length) {
            $l.html('<div class="h5-empty">暂无任务</div>');
            return;
        }
        rows.forEach(function (t) {
            var $c = $('<div style="background:var(--glass-2);border:1px solid var(--glass-5);border-radius:12px;padding:12px;margin-bottom:10px;"/>');
            $c.append('<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">' +
                '<span style="font-weight:600;color:var(--text-1);">' + esc(t.name || t.taskCode) + '</span>' +
                '<span style="font-size:12px;">' + statusHtml(t) + '</span></div>');
            $c.append(row('ID', '<code style="color:var(--accent);">' + esc(t.taskCode) + '</code>'));
            $c.append(row('目录', esc(t.workDir)));
            $c.append(row('脚本', '<code>' + esc(t.script) + '</code>'));
            $c.append(row('计划', '<code>' + esc(t.plan || '仅手动') + '</code> · ' + (t.execMode === 'parallel' ? '并行' : '串行')));
            $c.append(row('上次', esc(t.lastRunTime || '从未') + ' · 共 ' + (t.runCount || 0) + ' 次'));
            $c.append(row('下次', esc(t.nextRunTime || '-')));
            var $ops = $('<div style="display:flex;gap:8px;margin-top:10px;"/>');
            $ops.append('<span class="h5-btn-primary h5-btn-sm h5-bt-run" data-id="' + t.id + '">执行</span>');
            $ops.append('<span class="h5-btn-ghost h5-btn-sm h5-bt-edit" data-id="' + t.id + '">编辑</span>');
            $ops.append('<span class="h5-btn-ghost h5-btn-sm h5-bt-log" data-id="' + t.id + '">日志</span>');
            $c.append($ops);
            $l.append($c);
        });
    }

    function load(silent) {
        $.ajax({ url: 'batch/list', type: 'GET', dataType: 'json', cache: false })
            .done(function (res) {
                if (res.errorCode !== '000000') {
                    renderList();
                    if (!silent) toast('刷新失败：' + (res.errorMsg || '无权限'));
                    return;
                }
                rows = (res.body && res.body.list) || [];
                renderList();
                if (!silent) toast('已刷新');
            })
            .fail(function () {
                $('#h5BtList').html('<div class="h5-empty">加载失败，请稍后重试</div>');
                if (!silent) toast('刷新失败，请重试');
            });
    }

    function findTask(id) {
        for (var i = 0; i < rows.length; i++) if (rows[i].id === id) return rows[i];
        return null;
    }

    /* ================= 计划校验 / 预览 ================= */

    function planHint(plan) {
        if (!plan) {
            $('#h5BtHint').css('color', 'var(--text-5)').text('留空 = 仅手动执行');
            return;
        }
        $.ajax({ url: 'batch/preview', type: 'GET', dataType: 'json', cache: false, data: { plan: plan, count: 5 } }).done(function (res) {
            if (res.errorCode !== '000000') {
                $('#h5BtHint').css('color', '#f87171').text('✗ ' + (res.errorMsg || '表达式不合法'));
                return;
            }
            var list = (res.body && res.body.list) || [];
            $('#h5BtHint').css('color', list.length ? '#4ade80' : '#e0a800')
                .text(list.length ? '✓ 有效，下次：' + list[0] : '⚠ 有效，但没有后续执行时间');
        }).fail(function () {
            $('#h5BtHint').css('color', '#f87171').text('✗ 校验失败');
        });
    }

    function previewInline() {
        var plan = $.trim($('#h5BtPlan').val() || '');
        if (!plan) { toast('执行计划为空，仅支持手动执行'); return; }
        $.ajax({ url: 'batch/preview', type: 'GET', dataType: 'json', cache: false, data: { plan: plan, count: 5 } }).done(function (res) {
            if (res.errorCode !== '000000') { toast(res.errorMsg || '表达式不合法'); return; }
            var list = (res.body && res.body.list) || [];
            var html = list.length
                ? list.map(function (t, i) { return '<div style="font-size:12px;color:var(--text-2);line-height:1.9;">' + (i + 1) + '. ' + esc(t) + '</div>'; }).join('')
                : '<div style="font-size:12px;color:var(--text-5);">没有后续执行时间</div>';
            $('#h5BtPreview').html('<div style="font-size:12px;color:var(--text-5);margin:6px 0 2px;">接下来 ' + list.length + ' 次执行时间</div>' + html).show();
        }).fail(function () { toast('预览失败'); });
    }

    /* ================= 新增 / 编辑 ================= */

    function planToTime(plan) {
        var m = /^0\s+(\d{1,2})\s+(\d{1,2})\s+\*\s+\*\s+\?$/.exec((plan || '').trim());
        if (!m) return '08:00';
        function p(n) { return n.length < 2 ? '0' + n : n; }
        return p(m[2]) + ':' + p(m[1]);
    }

    function openEdit(t) {
        var creating = !t;
        var inStyle = 'width:100%;box-sizing:border-box;padding:10px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);margin-bottom:8px;font-size:13px;';
        openDialog('<h3>' + (creating ? '新增任务' : '编辑任务') + '</h3>' +
            '<div style="max-height:58vh;overflow:auto;-webkit-overflow-scrolling:touch;">' +
            '<div style="font-size:12px;color:var(--text-5);margin-bottom:6px;">任务ID：' + (creating ? '保存后由系统自动生成' : esc(t.taskCode)) + '</div>' +
            '<input class="h5-input" id="h5BtName" placeholder="任务名称" value="' + (creating ? '' : esc(t.name || '')) + '" style="' + inStyle + '">' +
            '<input class="h5-input" id="h5BtDir" placeholder="任务目录（如 /apps/GIT/aiAgent）" value="' + (creating ? '' : esc(t.workDir || '')) + '" style="' + inStyle + '">' +
            '<input class="h5-input" id="h5BtScript" placeholder="执行脚本（如 .venv/bin/python agent.py）" value="' + (creating ? '' : esc(t.script || '')) + '" style="' + inStyle + '">' +
            '<select id="h5BtMode" style="' + inStyle + '">' +
            '<option value="serial"' + (creating || t.execMode !== 'parallel' ? ' selected' : '') + '>串行（运行中跳过）</option>' +
            '<option value="parallel"' + (!creating && t.execMode === 'parallel' ? ' selected' : '') + '>并行</option></select>' +
            '<div style="display:flex;gap:8px;align-items:center;margin-bottom:4px;">' +
            '<input class="h5-input" id="h5BtPlan" placeholder="0 0 8 * * ?" value="' + (creating ? '0 0 8 * * ?' : esc(t.plan || '')) + '" style="flex:1;margin-bottom:0;">' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5BtPreviewBtn" style="white-space:nowrap;">预览</span></div>' +
            '<div id="h5BtHint" style="font-size:12px;color:var(--text-5);margin:4px 0 6px;">留空 = 仅手动执行</div>' +
            '<div id="h5BtPreview" style="display:none;background:var(--glass-2);border-radius:8px;padding:8px;margin-bottom:8px;"></div>' +
            '<div style="display:flex;gap:8px;align-items:center;margin-bottom:8px;">' +
            '<input type="time" id="h5BtTime" value="' + (creating ? '08:00' : planToTime(t.plan)) + '" style="flex:1;padding:8px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5BtDaily" style="white-space:nowrap;">设为每天</span>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5BtManual" style="white-space:nowrap;">仅手动</span></div>' +
            '<input type="number" id="h5BtTimeout" min="1" max="86400" placeholder="超时秒数" value="' + (creating ? 3600 : (t.timeoutSec || 3600)) + '" style="' + inStyle + '">' +
            '<label style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--text-2);margin-bottom:10px;">' +
            '<input type="checkbox" id="h5BtEnabled"' + (creating || t.enabled === 1 ? ' checked' : '') + ' style="transform:scale(1.2);">按计划自动执行</label>' +
            '</div>' +
            '<div class="h5-dialog-actions">' +
            '<button class="h5-btn" onclick="closeDialog()">取消</button>' +
            '<button class="h5-btn-primary" id="h5BtSave">保存</button></div>');

        var id = creating ? null : t.id;
        $('#h5BtPlan').on('input', function () {
            if (this._t) clearTimeout(this._t);
            var v = this.value;
            this._t = setTimeout(function () { planHint($.trim(v)); }, 400);
        });
        $('#h5BtPreviewBtn').on('click', previewInline);
        $('#h5BtDaily').on('click', function () {
            var v = $('#h5BtTime').val() || '08:00';
            var p = v.split(':');
            $('#h5BtPlan').val('0 ' + Number(p[1]) + ' ' + Number(p[0]) + ' * * ?');
            planHint($('#h5BtPlan').val());
        });
        $('#h5BtManual').on('click', function () {
            $('#h5BtPlan').val('');
            $('#h5BtPreview').hide();
            planHint('');
        });
        planHint($('#h5BtPlan').val());

        $('#h5BtSave').on('click', function () {
            var payload = {
                name: $.trim($('#h5BtName').val() || ''),
                workDir: $.trim($('#h5BtDir').val() || ''),
                script: $.trim($('#h5BtScript').val() || ''),
                execMode: $('#h5BtMode').val(),
                plan: $.trim($('#h5BtPlan').val() || ''),
                enabled: $('#h5BtEnabled').prop('checked'),
                timeoutSec: Number($('#h5BtTimeout').val() || 3600)
            };
            if (id) payload.id = id;
            postJSON(id ? 'batch/save' : 'batch/add', payload).done(function (res) {
                if (res.errorCode !== '000000') { toast(res.errorMsg || '保存失败'); return; }
                closeDialog();
                var code = res.body && res.body.taskCode;
                toast((id ? '已保存' : '已创建 ' + (code || '')) + (res.body && res.body.nextRunTime ? '，下次 ' + res.body.nextRunTime : ''));
                load();
            }).fail(function (xhr) {
                var r = xhr.responseJSON || {};
                toast(r.errorMsg || '保存失败');
            });
        });
    }

    /* ================= 日志 / 执行 ================= */

    function openLog(id) {
        var t = findTask(id);
        openSheet('<div style="font-weight:600;color:var(--text-1);margin-bottom:8px;">执行日志 · ' +
            esc(t ? (t.name || t.taskCode) : id) + '</div>' +
            '<pre id="h5BtLog" style="margin:0;font-size:11px;line-height:1.6;white-space:pre-wrap;word-break:break-all;color:var(--text-2);max-height:60vh;overflow:auto;">加载中…</pre>');
        $.ajax({ url: 'batch/log', type: 'GET', dataType: 'json', cache: false, data: { id: id, lines: 300 } }).done(function (res) {
            if (res.errorCode !== '000000') { $('#h5BtLog').text(res.errorMsg || '读取失败'); return; }
            $('#h5BtLog').text((res.body && res.body.log) || '（暂无日志）');
        }).fail(function () { $('#h5BtLog').text('读取失败'); });
    }

    function runTask(id) {
        var t = findTask(id);
        confirm('立即执行任务「' + ((t && (t.name || t.taskCode)) || id) + '」？', function () {
            postJSON('batch/run', { id: id }).done(function (res) {
                if (res.errorCode !== '000000') { toast(res.errorMsg || '执行失败'); return; }
                toast(res.body || '已开始执行');
                setTimeout(load, 3000);
            }).fail(function (xhr) {
                var r = xhr.responseJSON || {};
                toast(r.errorMsg || '执行失败');
            });
        });
    }

    /* ================= 入口 ================= */

    function render($page) {
        if ($page.data('h5bt')) {
            $('#h5BtList').html('<div class="h5-empty">加载中…</div>');
            load(true);
            setFab(true, function () { openEdit(null); });
            return;
        }
        $page.data('h5bt', true);
        $page.html(
            '<div class="h5-section-title">批次任务 <span style="font-size:12px;color:var(--text-5);font-weight:400;">仅管理员</span>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5BtRefresh" style="float:right;">刷新</span></div>' +
            '<div style="font-size:12px;color:var(--text-5);line-height:1.8;margin-bottom:12px;">' +
            '按执行计划在服务器目录执行脚本；计划为 6 段 cron（秒 分 时 日 月 周），留空 = 仅手动。</div>' +
            '<div id="h5BtList"><div class="h5-empty">加载中…</div></div>');

        $page.on('click', '#h5BtRefresh', function () {
            $(this).text('刷新中…');
            var $self = $(this);
            load(false);
            setTimeout(function () { $self.text('刷新'); }, 800);
        });
        $page.on('click', '.h5-bt-edit', function () { openEdit(findTask(Number($(this).data('id')))); });
        $page.on('click', '.h5-bt-log', function () { openLog(Number($(this).data('id'))); });
        $page.on('click', '.h5-bt-run', function () { runTask(Number($(this).data('id'))); });

        setFab(true, function () { openEdit(null); });
        load(true);
    }

    if (window.registerH5Module) window.registerH5Module('batchTask', render);

})();
