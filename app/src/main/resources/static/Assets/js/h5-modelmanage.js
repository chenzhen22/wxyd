(function () {

    var initialized = false;
    var state = { models: [], defaultModel: '', admin: false, autoRunning: {}, autoData: null };

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function postJSON(url, body) {
        return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify(body == null ? {} : { body: body }) });
    }

    function fmtNum(n) {
        n = Number(n) || 0;
        return n >= 10000 ? (n / 1000).toFixed(1) + 'k' : String(n);
    }

    function fmtMs(ms) {
        if (!ms) return '-';
        var d = new Date(Number(ms));
        function p(n) { return n < 10 ? '0' + n : n; }
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
    }

    /* ================= 状态徽章（账号池视角） ================= */

    function refreshStatus() {
        var $b = $('#h5MmStatus').text('🟡 …');
        $.getJSON('gateway/status').done(function (res) {
            if (res.errorCode !== '000000' || !res.body) {
                $b.text('🔴 凭据不可用');
                return;
            }
            var st = res.body;
            var rows = st.accounts || [];
            var totalCredits = 0, hasErr = false;
            rows.forEach(function (r) {
                if (typeof r.credits === 'number') totalCredits += r.credits;
                else hasErr = true;
            });
            if (!rows.length) $b.text('🔴 账号池为空');
            else $b.text('🟢 池 ' + (st.healthy || 0) + '/' + rows.length + ' · 积分 ' + totalCredits + (hasErr ? '+' : ''));
        }).fail(function () {
            $b.text('🔴 凭据不可用');
        });
    }

    /* ================= 管理员区（仅管理员，默认全收起） ================= */

    function loadAdmin() {
        $.getJSON('gateway/admin/config').done(function (res) {
            if (res.errorCode !== '000000') return; /* 非管理员：隐藏整个区块 */
            state.admin = true;
            $('#h5MmStatus').show();
            $('#h5MmAdmin').show();
            refreshStatus();
            var on = res.body && res.body.modelSwitch;
            $('#h5MmSwitch').prop('checked', !!on);
            $('#h5MmSwitchLabel').text(on ? '开（用户自选模型生效）' : '关（静默映射免费模型）');
            loadAdminKeys();
            loadPool();
            loadUsage();
            loadAutomation();
            loadFreePriority();
            $.getJSON('gateway/admin/models').done(function (r2) {
                if (r2.errorCode !== '000000') return;
                var b = r2.body || {};
                $('#h5MmModelsSource').text('目录：' + (b.source === 'live' ? '实时' : '兜底'));
                var $l = $('#h5MmAdminModels').html('');
                (b.list || []).forEach(function (m) {
                    $l.append('<div style="display:flex;justify-content:space-between;font-size:12px;color:var(--text-2);padding:3px 0;">' +
                        '<span>' + esc(m.id) + '</span><span>' + (m.free ? '<span style="color:#4ade80;">免费</span>' : '计费') + '</span></div>');
                });
            });
        }).fail(function () { /* 非管理员 */ });
    }

    function loadAdminKeys() {
        $.getJSON('gateway/admin/keys').done(function (res) {
            if (res.errorCode !== '000000') return;
            var list = (res.body && res.body.list) || [];
            var $l = $('#h5MmAdminKeys').html('');
            $('#h5MmAdminKeysEmpty').toggle(!list.length);
            list.forEach(function (k) {
                var ov = k.override || 'auto';
                var $card = $('<div/>').css({ background: 'var(--glass-3)', border: '1px solid var(--glass-5)', borderRadius: '10px', padding: '10px', marginBottom: '8px' });
                $card.append('<div style="display:flex;justify-content:space-between;font-size:13px;color:var(--text-1);margin-bottom:4px;">' +
                    '<span>' + esc((k.ownerName || k.username || '?') + ' · ' + (k.name || '(未命名)')) + '</span>' +
                    '<span style="font-size:11px;">' + (k.enabled ? '🟢' : '⏸️') + '</span></div>');
                $card.append('<div style="font-size:12px;color:var(--text-5);margin-bottom:6px;"><code style="color:var(--accent);">' + esc(k.maskedKey) + '</code> · 模型：' + esc(k.model || '跟随系统（自动）') + '</div>');
                $card.append('<select class="h5-mm-admin-ov" data-id="' + k.id + '" style="width:100%;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' +
                    '<option value="auto"' + (ov === 'auto' ? ' selected' : '') + '>模型开关：跟随全局</option>' +
                    '<option value="on"' + (ov === 'on' ? ' selected' : '') + '>模型开关：开（覆盖）</option>' +
                    '<option value="off"' + (ov === 'off' ? ' selected' : '') + '>模型开关：关（覆盖）</option></select>');
                $l.append($card);
            });
        });
    }

    /* ================= 免费模型优先级 ================= */

    function renderFreePriority(b) {
        b = b || {};
        var manual = b.manual || [], auto = b.auto || [], eff = b.effective || [], bad = b.invalid || [];
        $('#h5MmFree').val(manual.join('\n'));
        var h = '自动：' + (auto.length ? esc(auto.join(' → ')) : '（暂无）');
        h += '<br>生效：' + (eff.length ? esc(eff.join(' → ')) : '（没有免费模型）');
        if (bad.length) h += '<br><span style="color:#f87171;">已失效（忽略）：' + esc(bad.join('、')) + '</span>';
        $('#h5MmFreeHint').html(h);
    }

    function loadFreePriority() {
        $.ajax({ url: 'gateway/admin/free-priority', type: 'GET', dataType: 'json', cache: false })
            .done(function (res) {
                if (res.errorCode === '000000') renderFreePriority(res.body);
            });
    }

    /* ================= 账号池 ================= */

    function loadPool() {
        $.getJSON('gateway/status').done(function (res) {
            if (res.errorCode !== '000000') return;
            var rows = (res.body && res.body.accounts) || [];
            var $l = $('#h5MmPool').html('');
            $('#h5MmPoolEmpty').toggle(!rows.length);
            var opts = (state.models || []).map(function (m) { return '<option>' + esc(m) + '</option>'; }).join('');
            if (!opts) opts = '<option>（目录加载中）</option>';
            rows.forEach(function (r) {
                var warn = '';
                if (r.credentialDeadUntilMs) warn = ' <span style="color:#f87171;">⚠ 失效</span>';
                else if (r.cooldownUntilMs) warn = ' <span style="color:#e0a800;">⚠ 冷却</span>';
                var credits = typeof r.credits === 'number' ? r.credits : esc(r.creditsError || '?');
                var $card = $('<div/>').css({ background: 'var(--glass-3)', border: '1px solid var(--glass-5)', borderRadius: '10px', padding: '10px', marginBottom: '8px' });
                $card.append('<div style="display:flex;justify-content:space-between;font-size:13px;color:var(--text-1);margin-bottom:4px;">' +
                    '<span>' + esc(r.label || r.id) + warn + '</span>' +
                    '<span style="font-size:11px;color:var(--text-5);">' + esc(r.source === 'live' || r.source === 'desktop' ? '桌面登录' : '快照') + '</span></div>');
                $card.append('<div style="font-size:12px;color:var(--text-5);margin-bottom:2px;">剩余积分：<b style="color:var(--accent);">' + credits + '</b> · 令牌到期：' + fmtMs(r.expiresAtMs) + '</div>');
                $card.append('<select style="width:100%;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' + opts + '</select>');
                $l.append($card);
            });
        });
    }

    /* ================= 令牌消耗统计 ================= */

    function loadUsage() {
        var date = $('#h5MmUsageDate').val() || '';
        $.getJSON('gateway/admin/usage/summary').done(function (res) {
            if (res.errorCode !== '000000') return;
            var b = res.body || {};
            var t = b.totals || {}, td = b.today || {};
            var chip = function (l, v) { return '<span style="padding:4px 10px;border-radius:8px;background:var(--glass-3);border:1px solid var(--glass-5);font-size:11px;color:var(--text-5);">' + l + ' <b style="color:var(--accent);">' + esc(v) + '</b></span>'; };
            $('#h5MmUsageChips').html(chip('总请求', t.count || 0) + chip('输入', fmtNum(t.inputTokens)) + chip('输出', fmtNum(t.outputTokens)) + chip('今日', td.count || 0));
        });
        $.getJSON('gateway/admin/usage/logs', { pageNum: 1, pageSize: 20, date: date }).done(function (res) {
            if (res.errorCode !== '000000') return;
            var logs = (res.body && res.body.list) || [];
            var $l = $('#h5MmUsageLogs').html('');
            $('#h5MmUsageEmpty').toggle(!logs.length);
            logs.forEach(function (x) {
                var $row = $('<div/>').css({ padding: '6px 0', borderBottom: '1px solid var(--glass-5)', fontSize: '12px' });
                $row.append('<div style="display:flex;justify-content:space-between;align-items:center;">' +
                    '<div><span style="color:var(--text-1);">' + esc(x.ownerName || x.userId) + '</span>' +
                    ' <span style="color:var(--text-5);">' + esc(x.usedModel || '?') + '</span>' +
                    '<div style="color:var(--text-5);font-size:11px;">' + esc(x.createTime) + ' · in ' + fmtNum(x.inputTokens) + ' / out ' + fmtNum(x.outputTokens) + ' · ' + Math.round(x.durationMs / 100) / 10 + 's</div></div>' +
                    '<span class="h5-btn-ghost h5-btn-sm h5-mm-detail" data-id="' + x.id + '">详情</span></div>');
                $l.append($row);
            });
        });
    }

    function openUsageDetail(id) {
        loading(true);
        $.getJSON('gateway/admin/usage/detail', { id: id }).done(function (res) {
            loading(false);
            if (res.errorCode !== '000000' || !res.body) {
                toast(res.errorMsg || '详情获取失败');
                return;
            }
            var b = res.body;
            var fmt = function (s) {
                if (!s) return '(无)';
                try { return JSON.stringify(JSON.parse(s), null, 2); } catch (e) { return s; }
            };
            openDialog('<h3>调用详情 <span style="font-weight:400;font-size:12px;color:var(--text-5);">' + esc((b.ownerName || '') + ' · ' + (b.createTime || '')) + '</span></h3>' +
                '<div style="max-height:60vh;overflow:auto;">' +
                '<div style="font-size:12px;font-weight:600;color:var(--text-2);margin:4px 0;">请求数据</div>' +
                '<pre style="margin:0 0 10px;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);font-size:11px;white-space:pre-wrap;word-break:break-all;max-height:220px;overflow:auto;">' + esc(fmt(b.requestData)) + '</pre>' +
                '<div style="font-size:12px;font-weight:600;color:var(--text-2);margin-bottom:4px;">应答数据</div>' +
                '<pre style="margin:0;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);font-size:11px;white-space:pre-wrap;word-break:break-all;max-height:220px;overflow:auto;">' + esc(fmt(b.responseData)) + '</pre></div>' +
                '<div class="h5-dialog-actions"><button class="h5-btn-primary" onclick="closeDialog()">关闭</button></div>');
        }).fail(function () {
            loading(false);
            toast('详情获取失败');
        });
    }

    /* ================= 积分自动化 ================= */

    var AUTO_KIND_LABEL = { checkin: '签到', report: '活跃上报', tasks: '任务领奖', streak: '连登兑换+抽奖', travel: '猫猫旅行', compensation: '故障补偿' };

    function renderAuto(b) {
        b = b || state.autoData || {};
        $('#h5MmAutoEnabled').prop('checked', !!b.enabled);
        $('#h5MmAutoEnabledLabel').text(b.enabled ? '已开启' : '已关闭');
        var $l = $('#h5MmAutoList').html('');
        (b.jobs || []).forEach(function (j) {
            var running = !!state.autoRunning[j.kind];
            var $card = $('<div/>').css({ background: 'var(--glass-3)', border: '1px solid var(--glass-5)', borderRadius: '10px', padding: '10px', marginBottom: '8px' });
            var hs = (j.hours || []).join(',') + ' 点';
            var run = running ? '<span class="gw-spin"></span>执行中…' : (j.lastRunAtMs ? fmtMs(j.lastRunAtMs) : '尚未运行');
            $card.append('<div style="display:flex;justify-content:space-between;font-size:13px;color:var(--text-1);margin-bottom:4px;">' +
                '<span>' + esc(AUTO_KIND_LABEL[j.kind] || j.kind) + ' <span style="font-size:11px;color:var(--text-5);">' + esc(hs) + '</span></span>' +
                (running ? '<span style="font-size:11px;color:var(--text-5);">执行中</span>' : '<span class="h5-btn-ghost h5-btn-sm h5-mm-auto-run" data-kind="' + esc(j.kind) + '">立即执行</span>') + '</div>');
            $card.append('<div style="font-size:11px;color:var(--text-5);margin-bottom:2px;">' + run + ' · ' + esc(j.message || '') + '</div>');
            var det = (j.detail || []);
            if (det.length) $card.append('<div style="font-size:11px;color:var(--text-2);">' + det.map(esc).join('<br>') + '</div>');
            $l.append($card);
        });
    }

    function loadAutomation() {
        $.getJSON('gateway/admin/automation').done(function (res) {
            if (res.errorCode !== '000000') return;
            state.autoData = res.body || {};
            renderAuto(state.autoData);
        });
    }

    /* ================= 渲染 ================= */

    function renderModelManage($page) {
        $page.html(
            '<div class="h5-section-title">模型管理 <span id="h5MmStatus" style="display:none;font-size:12px;color:var(--text-5);cursor:pointer;">🟡 …</span></div>' +
            '<div id="h5MmAdmin" style="display:none;">' +
            /* 模型开关 */
            '<div class="h5-mm-sec-head" data-sec="h5MmSecSwitch" style="font-weight:600;color:var(--text-1);margin-bottom:6px;cursor:pointer;">⚙️ 模型开关 <span id="h5MmModelsSource" style="font-weight:400;font-size:11px;color:var(--text-5);"></span> <span style="float:right;color:var(--text-5);">▸</span></div>' +
            '<div id="h5MmSecSwitch" style="display:none;">' +
            '<label style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--text-2);cursor:pointer;">' +
            '<input type="checkbox" id="h5MmSwitch"> <span id="h5MmSwitchLabel"></span></label>' +
            '<div style="font-size:11px;color:var(--text-5);margin:6px 0 8px;">关 = 所有请求自动映射到免费模型（对用户不可见）</div>' +
            '<div id="h5MmAdminModels"></div></div>' +
            /* 免费模型优先级 */
            '<div class="h5-mm-sec-head" data-sec="h5MmSecFree" style="font-weight:600;color:var(--text-1);margin:10px 0 6px;cursor:pointer;border-top:1px solid var(--glass-5);padding-top:10px;">🎯 免费模型优先级 <span style="float:right;color:var(--text-5);">▸</span></div>' +
            '<div id="h5MmSecFree" style="display:none;">' +
            '<div style="font-size:11px;color:var(--text-5);line-height:1.7;margin-bottom:6px;">模型开关关闭时的静默映射顺序，每行一个模型 id，越靠前越优先；留空或失效时按目录自动顺序。</div>' +
            '<textarea id="h5MmFree" spellcheck="false" style="width:100%;box-sizing:border-box;min-height:96px;padding:10px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);font-family:Consolas,Menlo,monospace;font-size:12px;line-height:1.7;"></textarea>' +
            '<div id="h5MmFreeHint" style="font-size:11px;color:var(--text-5);line-height:1.8;margin:6px 0;"></div>' +
            '<div style="display:flex;gap:8px;"><span class="h5-btn-primary h5-btn-sm" id="h5MmFreeSave">保存优先级</span>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5MmFreeReset">恢复自动</span></div></div>' +
            /* 全部用户 Key */
            '<div class="h5-mm-sec-head" data-sec="h5MmSecKeys" style="font-weight:600;color:var(--text-1);margin:10px 0 6px;cursor:pointer;border-top:1px solid var(--glass-5);padding-top:10px;">🗂️ 全部用户 Key <span style="float:right;color:var(--text-5);">▸</span></div>' +
            '<div id="h5MmSecKeys" style="display:none;">' +
            '<div id="h5MmAdminKeys"></div>' +
            '<div class="h5-empty" id="h5MmAdminKeysEmpty" style="display:none;">还没有用户创建 Key</div></div>' +
            /* 账号池 */
            '<div class="h5-mm-sec-head" data-sec="h5MmSecPool" style="font-weight:600;color:var(--text-1);margin:10px 0 6px;cursor:pointer;border-top:1px solid var(--glass-5);padding-top:10px;">🧑‍💻 账号池 <span style="float:right;color:var(--text-5);">▸</span></div>' +
            '<div id="h5MmSecPool" style="display:none;">' +
            '<div id="h5MmPool"></div>' +
            '<div class="h5-empty" id="h5MmPoolEmpty" style="display:none;">池内没有账号（桌面 App 未登录？）</div></div>' +
            /* 令牌消耗统计 */
            '<div class="h5-mm-sec-head" data-sec="h5MmSecUsage" style="font-weight:600;color:var(--text-1);margin:10px 0 6px;cursor:pointer;border-top:1px solid var(--glass-5);padding-top:10px;">📊 令牌消耗统计 <span style="float:right;color:var(--text-5);">▸</span></div>' +
            '<div id="h5MmSecUsage" style="display:none;">' +
            '<div id="h5MmUsageChips" style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:8px;"></div>' +
            '<input type="date" id="h5MmUsageDate" style="width:100%;box-sizing:border-box;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);margin-bottom:8px;color-scheme:dark;">' +
            '<div id="h5MmUsageLogs"></div>' +
            '<div class="h5-empty" id="h5MmUsageEmpty" style="display:none;">当日暂无调用记录</div></div>' +
            /* 积分自动化 */
            '<div class="h5-mm-sec-head" data-sec="h5MmSecAuto" style="font-weight:600;color:var(--text-1);margin:10px 0 6px;cursor:pointer;border-top:1px solid var(--glass-5);padding-top:10px;">🎁 积分自动化 <span style="float:right;color:var(--text-5);">▸</span></div>' +
            '<div id="h5MmSecAuto" style="display:none;">' +
            '<div style="display:flex;align-items:center;gap:10px;margin-bottom:8px;flex-wrap:wrap;">' +
            '<label style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--text-2);cursor:pointer;">' +
            '<input type="checkbox" id="h5MmAutoEnabled"> <span id="h5MmAutoEnabledLabel" style="color:var(--text-5);"></span></label>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5MmAutoRunAll">▶ 一键执行全部</span></div>' +
            '<div id="h5MmAutoList"></div>' +
            '<div style="font-size:11px;color:var(--text-5);">每日每任务最多自动跑一次（按配置小时整点触发）；「立即执行」不占用当日档位。</div></div>' +
            '</div>');

        if (!initialized) {
            initialized = true;
            $('#h5MmAdmin').on('click', '.h5-mm-sec-head', function () {
                var sec = $('#' + $(this).data('sec'));
                var open = sec.is(':visible');
                sec.toggle(!open);
                $(this).find('span:last').text(open ? '▸' : '▾');
            });
            $('#h5MmAdminKeys').on('change', '.h5-mm-admin-ov', function () {
                var $sel = $(this);
                postJSON('gateway/admin/keys/switch', { id: Number($sel.data('id')), override: $sel.val() }).done(function (res) {
                    if (res.errorCode !== '000000') {
                        toast(res.errorMsg || '设置失败');
                        loadAdminKeys();
                        return;
                    }
                    toast('已设置');
                });
            });
            $('#h5MmUsageLogs').on('click', '.h5-mm-detail', function () {
                openUsageDetail(Number($(this).data('id')));
            });
            $('#h5MmUsageDate').on('change', function () { loadUsage(); });
            $('#h5MmFreeSave').off('click').on('click', function () {
                var lines = ($('#h5MmFree').val() || '').split(/\r?\n/)
                    .map(function (s) { return $.trim(s); }).filter(function (s) { return !!s; });
                postJSON('gateway/admin/free-priority', { list: lines }).done(function (res) {
                    if (res.errorCode !== '000000') { toast(res.errorMsg || '保存失败'); return; }
                    renderFreePriority(res.body);
                    toast('优先级已保存');
                }).fail(function () { toast('保存失败'); });
            });
            $('#h5MmFreeReset').off('click').on('click', function () {
                postJSON('gateway/admin/free-priority', { list: [] }).done(function (res) {
                    if (res.errorCode !== '000000') { toast(res.errorMsg || '操作失败'); return; }
                    renderFreePriority(res.body);
                    toast('已恢复自动顺序');
                }).fail(function () { toast('操作失败'); });
            });

            $('#h5MmSwitch').off('change').on('change', function () {
                var on = $(this).prop('checked');
                postJSON('gateway/admin/switch', { on: on }).done(function (res) {
                    if (res.errorCode !== '000000') {
                        toast(res.errorMsg || '切换失败');
                        $('#h5MmSwitch').prop('checked', !on);
                        return;
                    }
                    $('#h5MmSwitchLabel').text(on ? '开（用户自选模型生效）' : '关（静默映射免费模型）');
                });
            });
            $('#h5MmStatus').off('click').on('click', refreshStatus);
            $('#h5MmAutoEnabled').on('change', function () {
                var on = $(this).prop('checked');
                postJSON('gateway/admin/automation/enabled', { enabled: on }).done(function (res) {
                    if (res.errorCode === '000000') {
                        $('#h5MmAutoEnabledLabel').text(on ? '已开启' : '已关闭');
                        toast(on ? '积分自动化已开启' : '积分自动化已关闭');
                    } else {
                        toast(res.errorMsg || '保存失败');
                        $('#h5MmAutoEnabled').prop('checked', !on);
                    }
                }).fail(function () { toast('保存失败'); $('#h5MmAutoEnabled').prop('checked', !on); });
            });
            $('#h5MmAutoRunAll').on('click', function () {
                var $btn = $(this);
                $btn.text('执行中…');
                Object.keys(AUTO_KIND_LABEL).forEach(function (k) { state.autoRunning[k] = true; });
                renderAuto();
                postJSON('gateway/admin/automation/runAll', {}).done(function (res) {
                    $btn.text('▶ 一键执行全部');
                    state.autoRunning = {};
                    if (res.errorCode === '000000') {
                        var rows = res.body || [];
                        toast(rows.map(function (s) { return (AUTO_KIND_LABEL[s.kind] || s.kind) + ': ' + (s.message || ''); }).join(' | '));
                        loadAutomation();
                    } else { toast(res.errorMsg || '执行失败'); renderAuto(); }
                }).fail(function () { $btn.text('▶ 一键执行全部'); state.autoRunning = {}; renderAuto(); toast('执行失败'); });
            });
            $('#h5MmAutoList').on('click', '.h5-mm-auto-run', function () {
                var kind = $(this).data('kind');
                state.autoRunning[kind] = true;
                renderAuto();
                postJSON('gateway/admin/automation/run', { kind: kind }).done(function (res) {
                    $btn.text('立即执行');
                    if (res.errorCode === '000000') {
                        toast('执行完成：' + ((res.body && res.body.message) || ''));
                        loadAutomation();
                    } else {
                        toast(res.errorMsg || '执行失败');
                    }
                }).fail(function () { $btn.text('立即执行'); toast('执行失败'); });
            });
        }

        /* 日期默认当日（本地时区） */
        if (!$('#h5MmUsageDate').val()) {
            var d = new Date();
            function p2(n) { return n < 10 ? '0' + n : n; }
            $('#h5MmUsageDate').val(d.getFullYear() + '-' + p2(d.getMonth() + 1) + '-' + p2(d.getDate()));
        }
        /* 模型目录（账号池下拉用） */
        $.getJSON('gateway/models').done(function (res) {
            if (res.errorCode === '000000') state.models = (res.body && res.body.models) || [];
        }).always(function () { loadAdmin(); });
    }

    registerH5Module('modelManage', renderModelManage);

})();