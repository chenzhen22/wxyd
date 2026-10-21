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
        $.getJSON('batch/list').done(function (res) {
            if (res.errorCode !== '000000') {
                render([]);
                return;
            }
            render((res.body && res.body.list) || []);
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

    function openEdit(t) {
        currentId = t.id;
        $('#batchCode').val(t.taskCode);
        $('#batchName').val(t.name || '');
        $('#batchWorkDir').val(t.workDir || '');
        $('#batchScript').val(t.script || '');
        $('#batchExecMode').val(t.execMode === 'parallel' ? 'parallel' : 'serial');
        $('#batchPlan').val(t.plan || '');
        $('#batchDailyTime').val(planToTime(t.plan));
        $('#batchTimeout').val(t.timeoutSec || 3600);
        $('#batchEnabled').prop('checked', t.enabled === 1);
        $('#batchEditModal').show();
    }

    function save() {
        var payload = {
            id: currentId,
            name: $.trim($('#batchName').val() || ''),
            workDir: $.trim($('#batchWorkDir').val() || ''),
            script: $.trim($('#batchScript').val() || ''),
            execMode: $('#batchExecMode').val(),
            plan: $.trim($('#batchPlan').val() || ''),
            enabled: $('#batchEnabled').prop('checked'),
            timeoutSec: Number($('#batchTimeout').val() || 3600)
        };
        postJSON('batch/save', payload).done(function (res) {
            if (res.errorCode !== '000000') {
                alertMsg(res.errorMsg || '保存失败');
                return;
            }
            $('#batchEditModal').hide();
            alertMsg('已保存，下次执行时间：' + ((res.body && res.body.nextRunTime) || '未安排'));
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
        $.getJSON('batch/log', { id: id, lines: 400 }).done(function (res) {
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
        $('#batchRefresh').off('click').on('click', load);

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
        });

        $('#batchManualOnly').off('click').on('click', function () {
            $('#batchPlan').val('');
        });

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
