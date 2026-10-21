/**
 * 批次任务前端逻辑（PC，仅管理员）。
 * 后端：batch/list（列表）、batch/save（保存可编辑项）、batch/run（手动执行）、batch/log（日志尾部）。
 * 执行计划为 6 段 cron；页面提供「每天 HH:mm」快捷生成。
 */
var BatchTask = (function () {
    var initialized = false;
    var rows = [];
    var currentId = null;

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function alertMsg(msg) {
        if (window.alterModal) alterModal(msg); else alert(msg);
    }

    function postJSON(url, body) {
        return $.ajax({
            url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: body || {} })
        });
    }

    function statusHtml(t) {
        if (t.enabled !== 1) return '<span style="color:var(--text-5);">⏸ 已停用</span>';
        switch (t.lastStatus) {
            case 'running': return '<span style="color:#38bdf8;"><span class="gw-spin"></span>运行中</span>';
            case 'ok': return '<span style="color:#4ade80;">🟢 成功</span>';
            case 'fail': return '<span style="color:#f87171;">🔴 失败' + (t.lastExitCode != null ? ' (' + t.lastExitCode + ')' : '') + '</span>';
            case 'timeout': return '<span style="color:#e0a800;">🟠 超时</span>';
            default: return '<span style="color:var(--text-5);">未执行</span>';
        }
    }

    function render(list) {
        rows = list || [];
        var $tb = $('#batchTaskBody').html('');
        $('#batchEmpty').toggle(!rows.length);
        rows.forEach(function (t) {
            var $tr = $('<tr/>');
            $tr.append('<td><code style="color:var(--accent);">' + esc(t.taskCode) + '</code></td>');
            $tr.append('<td>' + esc(t.name || '') + '</td>');
            $tr.append('<td style="font-size:12px;word-break:break-all;">' + esc(t.workDir) + '</td>');
            $tr.append('<td style="font-size:12px;word-break:break-all;"><code>' + esc(t.script) + '</code></td>');
            $tr.append('<td>' + (t.execMode === 'parallel' ? '并行' : '串行') + '</td>');
            $tr.append('<td><code style="font-size:12px;">' + esc(t.plan || '仅手动') + '</code></td>');
            $tr.append('<td style="font-size:12px;">' + esc(t.lastRunTime || '从未') + '</td>');
            $tr.append('<td style="font-size:12px;">' + esc(t.nextRunTime || '-') + '</td>');
            $tr.append('<td>' + (t.runCount == null ? 0 : t.runCount) + '</td>');
            $tr.append('<td>' + statusHtml(t) + '</td>');
            var ops = $('<td style="white-space:nowrap;"/>');
            ops.append('<button class="btn-main bt-run" data-id="' + t.id + '" style="padding:3px 8px;font-size:12px;margin-right:4px;">执行</button>');
            ops.append('<button class="btn-main bt-edit" data-id="' + t.id + '" style="padding:3px 8px;font-size:12px;margin-right:4px;">编辑</button>');
            ops.append('<button class="btn-main bt-log" data-id="' + t.id + '" style="padding:3px 8px;font-size:12px;">日志</button>');
            $tr.append(ops);
            $tb.append($tr);
        });
    }

    function load() {
        var $btn = $('#batchRefresh');
        $btn.text('刷新中…');
        $.ajax({ url: 'batch/list', type: 'GET', dataType: 'json', cache: false })
            .done(function (res) {
                if (res.errorCode !== '000000') {
                    render([]);
                    $('#batchRefreshHint').text('刷新失败：' + (res.errorMsg || '无权限'));
                    return;
                }
                render((res.body && res.body.list) || []);
                var d = new Date();
                function p(n) { return n < 10 ? '0' + n : n; }
                $('#batchRefreshHint').text('已刷新 ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds()));
                setTimeout(function () { $('#batchRefreshHint').text(''); }, 4000);
            })
            .fail(function () {
                $('#batchRefreshHint').text('刷新失败，请重试');
            })
            .always(function () {
                $btn.text('🔄 刷新');
            });
    }

    function findTask(id) {
        for (var i = 0; i < rows.length; i++) if (rows[i].id === id) return rows[i];
        return null;
    }

    /** cron -> HH:mm（仅识别「0 m h * * ?」形态，供快捷设置回填） */
    function planToTime(plan) {
        var m = /^0\s+(\d{1,2})\s+(\d{1,2})\s+\*\s+\*\s+\?$/.exec((plan || '').trim());
        if (!m) return '';
        function p(n) { return n.length < 2 ? '0' + n : n; }
        return p(m[2]) + ':' + p(m[1]);
    }

    /** 预览计划接下来 5 次执行时间（同时用于表达式校验） */
    function fetchPreview(plan) {
        return $.ajax({ url: 'batch/preview', type: 'GET', dataType: 'json', cache: false, data: { plan: plan || '', count: 5 } });
    }

    /** 输入时自动校验表达式，结果实时显示在输入框下方 */
    function checkPlan() {
        var plan = $.trim($('#batchPlan').val() || '');
        if (!plan) {
            $('#batchPlanHint').css('color', 'var(--text-5)').text('留空 = 仅手动执行');
            return;
        }
        fetchPreview(plan).done(function (res) {
            if (res.errorCode !== '000000') {
                $('#batchPlanHint').css('color', '#f87171').text('✗ ' + (res.errorMsg || '表达式不合法'));
                return;
            }
            var list = (res.body && res.body.list) || [];
            if (!list.length) {
                $('#batchPlanHint').css('color', '#e0a800').text('⚠ 表达式有效，但没有后续执行时间');
            } else {
                $('#batchPlanHint').css('color', '#4ade80').text('✓ 表达式有效，下次执行：' + list[0]);
            }
        }).fail(function (xhr) {
            var r = xhr.responseJSON || {};
            $('#batchPlanHint').css('color', '#f87171').text('✗ ' + (r.errorMsg || '校验失败'));
        });
    }

    /** 点击「预览」：弹窗列出接下来 5 次执行时间 */
    function openPreview() {
        var plan = $.trim($('#batchPlan').val() || '');
        if (!plan) { alertMsg('执行计划为空，仅支持手动执行'); return; }
        fetchPreview(plan).done(function (res) {
            if (res.errorCode !== '000000') {
                alertMsg(res.errorMsg || '表达式不合法');
                return;
            }
            var list = (res.body && res.body.list) || [];
            $('#batchPreviewPlan').text('表达式：' + plan + ' · 接下来 ' + list.length + ' 次执行时间');
            var $ol = $('#batchPreviewList').html('');
            if (!list.length) {
                $ol.append('<li style="color:var(--text-5);list-style:none;">没有后续执行时间</li>');
            } else {
                list.forEach(function (t) { $ol.append('<li>' + esc(t) + '</li>'); });
            }
            $('#batchPreviewModal').show();
        }).fail(function (xhr) {
            var r = xhr.responseJSON || {};
            alertMsg(r.errorMsg || '预览失败');
        });
    }

    function openEdit(t) {
        currentId = t ? t.id : null;
        $('#batchEditTitle').text(t ? '编辑任务' : '新增任务');
        $('#batchCode').val(t ? t.taskCode : '（保存后自动生成）');
        $('#batchName').val(t ? (t.name || '') : '');
        $('#batchWorkDir').val(t ? (t.workDir || '') : '');
        $('#batchScript').val(t ? (t.script || '') : '');
        $('#batchExecMode').val(t && t.execMode === 'parallel' ? 'parallel' : 'serial');
        $('#batchPlan').val(t ? (t.plan || '') : '0 0 8 * * ?');
        $('#batchDailyTime').val(t ? planToTime(t.plan) : '08:00');
        $('#batchTimeout').val(t && t.timeoutSec ? t.timeoutSec : 3600);
        $('#batchEnabled').prop('checked', t ? t.enabled === 1 : true);
        $('#batchEditModal').show();
        checkPlan();
    }

    function save() {
        var payload = {
            name: $.trim($('#batchName').val() || ''),
            workDir: $.trim($('#batchWorkDir').val() || ''),
            script: $.trim($('#batchScript').val() || ''),
            execMode: $('#batchExecMode').val(),
            plan: $.trim($('#batchPlan').val() || ''),
            enabled: $('#batchEnabled').prop('checked'),
            timeoutSec: Number($('#batchTimeout').val() || 3600)
        };
        var creating = !currentId;
        if (!creating) payload.id = currentId;
        postJSON(creating ? 'batch/add' : 'batch/save', payload).done(function (res) {
            if (res.errorCode !== '000000') {
                alertMsg(res.errorMsg || '保存失败');
                return;
            }
            $('#batchEditModal').hide();
            var code = res.body && res.body.taskCode ? ('任务ID：' + res.body.taskCode + '；') : '';
            alertMsg((creating ? '已创建。' : '已保存。') + code
                + '下次执行时间：' + ((res.body && res.body.nextRunTime) || '未安排'));
            load();
        }).fail(function (xhr) {
            var r = xhr.responseJSON || {};
            alertMsg(r.errorMsg || '保存失败');
        });
    }

    function openLog(id) {
        var t = findTask(id);
        $('#batchLogMeta').text((t ? t.taskCode + ' · ' + (t.name || '') + ' · ' : '')
            + '最近状态：' + (t && t.lastStatus ? t.lastStatus : '未执行'));
        $('#batchLogContent').text('加载中…');
        $('#batchLogModal').data('id', id).show();
        loadLog(id);
    }

    function loadLog(id) {
        $.ajax({ url: 'batch/log', type: 'GET', dataType: 'json', cache: false, data: { id: id, lines: 400 } }).done(function (res) {
            if (res.errorCode !== '000000') {
                $('#batchLogContent').text(res.errorMsg || '读取失败');
                return;
            }
            var text = (res.body && res.body.log) || '';
            $('#batchLogContent').text(text);
            var el = document.getElementById('batchLogContent');
            if (el) el.scrollTop = el.scrollHeight;
        }).fail(function () {
            $('#batchLogContent').text('读取失败');
        });
    }

    function bind() {
        $('#batchRefresh').off('click.bt').on('click.bt', load);
        $('#batchAddBtn').off('click.bt').on('click.bt', function () { openEdit(null); });

        $('#batchTaskBody').on('click', '.bt-edit', function () {
            var t = findTask(Number($(this).data('id')));
            if (t) openEdit(t);
        });

        $('#batchTaskBody').on('click', '.bt-run', function () {
            var id = Number($(this).data('id'));
            var t = findTask(id);
            var go = function () {
                postJSON('batch/run', { id: id }).done(function (res) {
                    if (res.errorCode !== '000000') {
                        alertMsg(res.errorMsg || '执行失败');
                        return;
                    }
                    alertMsg(res.body || '已开始执行');
                    setTimeout(load, 3000);
                }).fail(function (xhr) {
                    var r = xhr.responseJSON || {};
                    alertMsg(r.errorMsg || '执行失败');
                });
            };
            if (window.confirmModal) {
                confirmModal('立即执行任务「' + ((t && t.name) || id) + '」？', go);
            } else if (window.confirm('立即执行该任务？')) {
                go();
            }
        });

        $('#batchTaskBody').on('click', '.bt-log', function () {
            openLog(Number($(this).data('id')));
        });

        $('#batchLogReload').off('click').on('click', function () {
            var id = $('#batchLogModal').data('id');
            if (id) loadLog(id);
        });

        $('#batchDailyApply').off('click').on('click', function () {
            var v = $('#batchDailyTime').val();
            if (!v) { alertMsg('请先选择时间'); return; }
            var parts = v.split(':');
            $('#batchPlan').val('0 ' + Number(parts[1]) + ' ' + Number(parts[0]) + ' * * ?');
            checkPlan();
        });

        $('#batchManualOnly').off('click').on('click', function () {
            $('#batchPlan').val('');
            checkPlan();
        });

        var planTimer = null;
        $('#batchPlan').off('input').on('input', function () {
            if (planTimer) clearTimeout(planTimer);
            planTimer = setTimeout(checkPlan, 400);
        });

        $('#batchPreviewBtn').off('click').on('click', openPreview);

        $('#batchSave').off('click').on('click', save);
    }

    return {
        init: function () {
            if (!initialized) {
                initialized = true;
                bind();
            }
            load();
        }
    };
})();
