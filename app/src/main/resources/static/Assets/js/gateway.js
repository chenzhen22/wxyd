(function () {

    var initialized = false;
    var state = { models: [], defaultModel: '', admin: false, usagePage: 1, autoRunning: {}, autoData: null };

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function postJSON(url, body) {
        return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: body || {} }) });
    }

    /* 弹框统一走应用默认样式：alterModal（提示）/ confirmModal（确认），index.js 提供 */
    function gwAlert(msg) {
        if (window.alterModal) alterModal(msg);
        else alert(msg);
    }

    /* 复制文本：clipboard API 优先，http 非 localhost 环境退回 execCommand */
    function copyText(text, okMsg) {
        var done = function () { gwAlert(okMsg || '已复制到剪贴板'); };
        var fail = function () {
            var ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.opacity = '0';
            document.body.appendChild(ta);
            ta.select();
            try {
                document.execCommand('copy');
                done();
            } catch (e) {
                window.prompt('请手动复制：', text);
            }
            document.body.removeChild(ta);
        };
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(done, fail);
        } else {
            fail();
        }
    }

    /* ================= 状态徽标 ================= */

    function refreshStatus() {
        var $b = $('#gwStatusBadge').text('🟡 状态未知');
        $.getJSON('gateway/status').done(function (res) {
            if (res.errorCode !== '000000' || !res.body) {
                $b.text('🔴 凭据不可用');
                renderPool([]);
                $('#mmProviderCards').html('<div class="mm-empty">凭据不可用</div>');
                return;
            }
            var st = res.body;
            var rows = st.accounts || [];
            var totalCredits = 0, hasErr = false;
            rows.forEach(function (r) {
                if (typeof r.credits === 'number') totalCredits += r.credits;
                else hasErr = true;
            });
            if (!rows.length) {
                $b.text('🔴 账号池为空（桌面 App 未登录？）');
            } else {
                $b.text('🟢 池 ' + (st.healthy || 0) + '/' + rows.length + ' 可用 · 总积分 ' + totalCredits + (hasErr ? '（部分积分查询失败）' : ''));
            }
            state.poolRows = rows;
            renderPool(rows);
            renderProviderCards(st);
            $('#mmKpiAccounts').text(((st.healthy || 0) + '/' + rows.length));
            $('#mmKpiAccountsHint').text(rows.length ? '账号池实时状态' : '尚未发现账号');
            $('#mmKpiCredits').text(totalCredits > 0 ? totalCredits : (rows.length ? '0' : '-'));
        }).fail(function () {
            $b.text('🔴 凭据不可用');
            renderPool([]);
        });
    }

    function fmtMs(ms) {
        if (!ms) return '-';
        var d = new Date(Number(ms));
        function p(n) { return n < 10 ? '0' + n : n; }
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
    }

    function renderPool(rows) {
        var $t = $('#gwPoolBody').html('');
        if (!rows || !rows.length) {
            $t.append('<tr><td colspan="5" style="color:var(--text-5);">池内没有账号：在桌面 App 登录后点「刷新」，或检查桌面 App 路径配置</td></tr>');
            return;
        }
        rows.forEach(function (r) {
            var $tr = $('<tr/>');
            var warn = '';
            if (r.credentialDeadUntilMs) warn = ' <span style="color:#f87171;">⚠ 登录态失效</span>';
            else if (r.cooldownUntilMs) warn = ' <span style="color:#e0a800;">⚠ 积分冷却</span>';
            if (r.lastError) warn += ' <span style="font-size:11px;color:var(--text-5);" title="' + esc(r.lastError) + '">ℹ</span>';
            var credits = typeof r.credits === 'number' ? r.credits : ('<span style="color:var(--text-5);">' + esc(r.creditsError || '?') + '</span>');
            var opts = (state.models || []).map(function (m) { return '<option>' + esc(m) + '</option>'; }).join('');
            if (!opts) opts = '<option>（目录加载中）</option>';
            $tr.append('<td>' + esc(r.label || r.id) + warn + '</td>');
            $tr.append('<td>' + esc(r.source === 'live' || r.source === 'desktop' ? '桌面登录' : '历史快照') + ' <span style="color:var(--text-5);">' + esc(r.domain || '') + '</span></td>');
            $tr.append('<td>' + credits + '</td>');
            $tr.append('<td><select class="gw-pool-model" style="width:100%;padding:3px 6px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);font-size:12px;">' + opts + '</select></td>');
            $tr.append('<td>' + fmtMs(r.expiresAtMs) + '</td>');
            $t.append($tr);
        });
    }

    /* ================= 接入地址 ================= */

    /** 概览卡：用通用字段描述各通道（不显示厂商名） */
    function renderProviderCards(st) {
        var ps = (st && st.providers) || {};
        var html = '';
        Object.keys(ps).forEach(function (k) {
            var s = ps[k] || {};
            if (typeof s.poolSize !== 'undefined' || Array.isArray(s.accounts)) {
                var total = Number(s.poolSize) || 0;
                var healthy = Number(s.healthy) || 0;
                html += '<div class="mm-prov"><div class="mm-prov-h">主账号池 <span>' + healthy + '/' + total + ' 可用</span></div><div class="mm-prov-b">';
                (s.accounts || []).forEach(function (a) {
                    html += '<div>' + esc(a.label || a.id) + ' · ' +
                        (typeof a.credits === 'number' ? a.credits + ' 积分' : '积分未知') + '</div>';
                });
                html += '</div></div>';
            } else {
                html += '<div class="mm-prov"><div class="mm-prov-h">备用通道 <span>' + (s.signedIn ? '已登录' : '未登录') + '</span></div>' +
                    '<div class="mm-prov-b"><div>' + esc(s.user || '-') + '</div><div>余额：' +
                    (typeof s.credits === 'number' ? s.credits : '-') + ' 积分</div></div></div>';
            }
        });
        $('#mmProviderCards').html(html || '<div class="mm-empty">暂无数据</div>');
    }

    function showAdminTab(tab) {
        $('.mm-tab').removeClass('active');
        $('.mm-tab[data-tab="' + tab + '"]').addClass('active');
        $('.mm-panel').removeClass('active');
        $('.mm-panel[data-panel="' + tab + '"]').addClass('active');
        if (tab === 'usage') loadUsage(state.usagePage || 1);
    }

    function fillUrls() {
        var base = location.origin + '/api'; /* context-path 固定为 /api */
        $('#gwUrlAnthropic').text(base + '/anthropic');
        $('#gwUrlOpenai').text(base + '/openai/v1');
        $('#gwCopyAnthropic').off('click').on('click', function () {
            copyText($('#gwUrlAnthropic').text(), 'Anthropic 地址已复制');
        });
        $('#gwCopyOpenai').off('click').on('click', function () {
            copyText($('#gwUrlOpenai').text(), 'OpenAI 地址已复制');
        });
    }

    /* ================= Key 列表（模型只读展示，不可修改） ================= */

    function renderKeys(list, max) {
        var active = 0;
        var $l = $('#gwKeyList').html('');
        $('#gwKeyEmpty').toggle(!list.length);
        $('#gwKeyCount').text(list.length);
        $('#gwKeyQuota').text('最多 ' + (max || 5) + ' 把 · 已用 ' + list.length + ' 把');
        list.forEach(function (k) {
            if (k.enabled) active++;
            var status = k.enabled ? (k.adminExempt ? '长期有效' : '今日已启用')
                : (k.activeDate ? '已过期' : '已停用');
            var $c = $('<div class="gw-key"/>');
            $c.append('<div style="min-width:0;flex:1;">' +
                '<div class="gw-key-name">' + esc(k.name || '(未命名)') +
                '<span class="gw-chip">' + esc(k.model || '跟随系统') + '</span></div>' +
                '<div class="gw-key-code"><code>' + esc(k.maskedKey) + '</code>' +
                '<button class="gw-mini gw-copy" data-id="' + k.id + '">复制</button></div>' +
                '<div class="gw-key-meta">最近使用：' + esc(k.lastUsedTime || '从未')
                + (k.activeDate ? '　最近启用：' + esc(k.activeDate) : '') + '</div></div>');
            var $side = $('<div class="gw-key-side"/>');
            $side.append('<span class="gw-badge ' + (k.enabled ? 'on' : 'off') + '">' + status + '</span>');
            var $ops = $('<div class="gw-key-actions"/>');
            /* data-enabled 用 attr 读取，避免 jQuery .data() 把 "1" 自动转成数字导致判断失效 */
            $ops.append('<button class="gw-mini gw-toggle" data-id="' + k.id + '" data-enabled="' + (k.enabled ? '1' : '0') + '">' + (k.enabled ? '停用' : '启用') + '</button>');
            $ops.append('<button class="gw-mini gw-del" data-id="' + k.id + '">删除</button>');
            $side.append($ops);
            $c.append($side);
            $l.append($c);
        });
        $('#gwKeyActive').text(active);
    }

    function loadKeys() {
        $.getJSON('gateway/keys').done(function (res) {
            if (res.errorCode !== '000000') return;
            var b = res.body || {};
            renderKeys(b.list || [], b.max || 5);
        });
    }

    function loadModels() {
        return $.getJSON('gateway/models').done(function (res) {
            if (res.errorCode !== '000000') return;
            var b = res.body || {};
            state.models = b.models || [];
            state.defaultModel = b.default || '';
            /* 创建 Key：默认「跟随系统」；目录默认模型在文字里标注 */
            var $sel = $('#gwKeyModel').html('<option value="auto" selected>跟随系统（自动选择模型）</option>');
            state.models.forEach(function (m) {
                var label = (m === state.defaultModel && state.defaultModel) ? m + '（默认）' : m;
                $sel.append('<option value="' + esc(m) + '">' + esc(label) + '</option>');
            });
            /* 账号池的模型下拉同步目录（池先渲染时目录可能未就绪） */
            if (state.poolRows) renderPool(state.poolRows);
        });
    }

    /* ================= 管理员区（非管理员不可见） ================= */

    function loadAdmin() {
        /* cache:false：管理配置必须实时拉取，浏览器缓存会导致开关状态回旧值 */
        $.ajax({ url: 'gateway/admin/config', type: 'GET', dataType: 'json', cache: false }).done(function (res) {
            if (res.errorCode !== '000000') return; /* 非管理员：状态/刷新按钮与整个管理区块都保持隐藏 */
            state.admin = true;
            /* 状态徽标与刷新按钮仅管理员可见（gateway/status 接口也仅管理员可调） */
            $('#gwStatusBadge, #gwRefreshBtn').show();
            refreshStatus();
            $('#gwAdminCard').show();
            var on = res.body && res.body.modelSwitch;
            $('#gwSwitch').prop('checked', !!on);
            $('#gwSwitchLabel').text(on ? '开（用户自选模型生效）' : '关（静默映射免费模型）');
            loadAdminKeys();
            loadUsage(1);
            loadAutomation();
            loadFreePriority();
            loadQoderCheckin();
            $.getJSON('gateway/admin/models').done(function (r2) {
                if (r2.errorCode !== '000000') return;
                var b = r2.body || {};
                $('#gwModelsSource').text(b.source === 'live' ? '实时目录' : '内置兜底目录');
                var $tb = $('#gwAdminModelBody').html('');
                (b.list || []).forEach(function (m) {
                    var provs = (m.providers || '').split(',').filter(Boolean).map(function (p) {
                        return p === 'workbuddy' ? '主账号池' : '备用通道';
                    }).join(' + ');
                    $tb.append('<tr><td><code style="color:var(--accent);">' + esc(m.id) + '</code></td><td>' + esc(provs || '-') +
                        '</td><td>' + (m.free ? '<span style="color:#4ade80;">免费</span>' : '积分计费') + '</td></tr>');
                });
            });
        }).fail(function () { /* 非管理员，保持隐藏 */ });
    }

    /* ================= 管理员：Qoder 每日签到 ================= */

    function renderQoderCheckin(b) {
        b = b || {};
        var lines = [];
        if (!b.signedIn) {
            lines.push('未检测到 Qoder 登录态（请在桌面端登录后重试）');
        } else {
            var st = b.active
                ? (b.todayCheckedIn ? '<span style="color:#4ade80;">🟢 今日已领取</span>' : '<span style="color:#e0a800;">🟡 今日待领取</span>')
                : '<span style="color:var(--text-5);">⚪ 当前没有可领取的签到活动</span>';
            lines.push('签到状态：' + st);
            if (b.amount) {
                lines.push('奖励：<b style="color:var(--accent);">' + esc(b.amount) + '</b> ' + esc(b.unit || 'credits')
                    + (b.validDays ? '（有效期 ' + b.validDays + ' 天）' : ''));
            }
            if (b.endsAtMs) lines.push('活动截止：' + esc(fmtMs(b.endsAtMs)));
            if (typeof b.credits === 'number') {
                lines.push('当前余额：<b style="color:var(--accent);">' + esc(b.credits) + '</b> credits'
                    + (typeof b.creditsTotal === 'number' ? ' / 共 ' + esc(b.creditsTotal) : ''));
            }
        }
        lines.push('<label style="cursor:pointer;display:inline-flex;align-items:center;gap:6px;">自动领取：'
            + '<input type="checkbox" id="gwQoderAuto"' + (b.enabled ? ' checked' : '') + ' style="transform:scale(1.1);"></label>'
            + '<span style="font-size:12px;color:var(--text-5);margin-left:8px;">' + esc(b.autoHours || '') + '</span>');
        $('#gwQoderCard').html(lines.join('<br>'));
        $('#gwQoderAuto').off('change').on('change', function () {
            var on = $(this).prop('checked');
            postJSON('gateway/admin/qoder/checkin/enabled', { enabled: on }).done(function (res) {
                if (res.errorCode !== '000000') { gwAlert(res.errorMsg || '保存失败'); return; }
                renderQoderCheckin(res.body);
                gwAlert(on ? '已开启每日自动领取' : '已关闭每日自动领取');
            }).fail(function () { gwAlert('保存失败'); });
        });
    }

    function loadQoderCheckin() {
        $.ajax({ url: 'gateway/admin/qoder/checkin', type: 'GET', dataType: 'json', cache: false })
            .done(function (res) {
                if (res.errorCode !== '000000') return;
                renderQoderCheckin(res.body);
            });
    }

    /* ================= 管理员：免费模型优先级 ================= */

    function renderFreePriority(b) {
        b = b || {};
        var manual = b.manual || [], auto = b.auto || [], effective = b.effective || [], invalid = b.invalid || [];
        $('#gwFreePriority').val(manual.join('\n'));
        var $c = $('#gwFreeCandidates').html('');
        if (!auto.length) $c.append('<span style="font-size:12px;color:var(--text-5);">（当前没有免费模型）</span>');
        auto.forEach(function (m) {
            $c.append($('<span class="admin-badge" style="cursor:pointer;font-size:12px;">' + esc(m) + '</span>')
                .attr('data-free-id', m)
                .on('click', function () {
                    var cur = ($('#gwFreePriority').val() || '').split(/\r?\n/)
                        .map(function (s) { return $.trim(s); }).filter(function (s) { return !!s; });
                    if (cur.indexOf(m) < 0) cur.push(m);
                    $('#gwFreePriority').val(cur.join('\n'));
                }));
        });
        var html = '自动顺序：' + (auto.length ? esc(auto.join(' → ')) : '（暂无）');
        html += '<br>当前生效：' + (effective.length ? esc(effective.join(' → ')) : '（没有免费模型）');
        if (invalid.length) {
            html += '<br><span style="color:#f87171;">已失效（将被忽略）：' + esc(invalid.join('、')) + '</span>';
        }
        $('#gwFreeHint').html(html);
    }

    function loadFreePriority() {
        $.ajax({ url: 'gateway/admin/free-priority', type: 'GET', dataType: 'json', cache: false })
            .done(function (res) {
                if (res.errorCode !== '000000') return;
                renderFreePriority(res.body);
            });
    }

    /* ================= 管理员：全部用户 Key ================= */

    function loadAdminKeys() {
        /* cache:false：override 状态必须实时（保存后重拉 + 避免缓存回旧值） */
        $.ajax({ url: 'gateway/admin/keys', type: 'GET', dataType: 'json', cache: false }).done(function (res) {
            if (res.errorCode !== '000000') return;
            var list = (res.body && res.body.list) || [];
            $('#mmKpiKeys').text(list.length);
            var $tb = $('#gwAdminKeyBody').html('');
            $('#gwAdminKeysEmpty').toggle(!list.length);
            list.forEach(function (k) {
                var ov = k.override || 'auto';
                var ovLabel = ov === 'on' ? '开（覆盖）' : ov === 'off' ? '关（覆盖）' : '跟随全局';
                var $tr = $('<tr/>');
                $tr.append('<td>' + esc(k.ownerName || k.username || k.userId) + '</td>');
                $tr.append('<td>' + esc(k.name || '(未命名)') + '</td>');
                $tr.append('<td><code style="color:var(--accent);">' + esc(k.maskedKey) + '</code></td>');
                $tr.append('<td>' + esc(k.model || '跟随系统（自动）') + '</td>');
                $tr.append('<td>' + (k.enabled ? (Number(k.userRole) === 0 ? '🟢 长期' : '🟢') : '⏸️') + '</td>');
                $tr.append('<td><select class="gw-admin-ov" data-id="' + k.id + '" style="width:100%;padding:4px;border-radius:6px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' +
                    '<option value="auto"' + (ov === 'auto' ? ' selected' : '') + '>跟随全局</option>' +
                    '<option value="on"' + (ov === 'on' ? ' selected' : '') + '>开（覆盖）</option>' +
                    '<option value="off"' + (ov === 'off' ? ' selected' : '') + '>关（覆盖）</option>' +
                    '</select> <span style="font-size:11px;color:var(--text-5);">' + esc(ovLabel) + '</span></td>');
                $tr.append('<td>' + esc(k.lastUsedTime || '从未') + '</td>');
                $tb.append($tr);
            });
        });
    }

    /* ================= 令牌消耗统计 ================= */

    function fmtNum(n) {
        n = Number(n) || 0;
        return n >= 10000 ? (n / 1000).toFixed(1) + 'k' : String(n);
    }

    function chip(label, value) {
        return '<div style="padding:8px 14px;border-radius:10px;background:var(--glass-2);border:1px solid var(--glass-5);font-size:12px;color:var(--text-5);">' +
            label + ' <span style="color:var(--accent);font-size:15px;font-weight:600;margin-left:4px;">' + esc(value) + '</span></div>';
    }

    /** 用筛选下拉填充用户选项（保留当前选择） */
    function fillUsageUserOptions(byUser) {
        var cur = $('#gwUsageUser').val() || '';
        var opts = '<option value="">全部用户</option>';
        var seen = {};
        byUser.forEach(function (x) {
            var id = String(x.userId);
            if (seen[id]) return;
            seen[id] = true;
            opts += '<option value="' + id + '">' + esc(x.ownerName || id) + '</option>';
        });
        $('#gwUsageUser').html(opts);
        // 当前筛选的用户若不在统计里（如 7 天前调用过），补上避免选项丢失
        if (cur && !seen[cur]) {
            $('#gwUsageUser').append('<option value="' + esc(cur) + '">用户#' + esc(cur) + '</option>');
        }
        $('#gwUsageUser').val(cur);
    }

    function loadUsage(page) {
        state.usagePage = page || 1;
        $.getJSON('gateway/admin/usage/summary').done(function (res) {
            if (res.errorCode !== '000000') return;
            var b = res.body || {};
            fillUsageUserOptions(b.byUser || []);
            var t = b.totals || {}, td = b.today || {};
            $('#mmKpiToday').text(td.count || 0);
            $('#mmKpiTodayHint').text('输入 ' + fmtNum(td.inputTokens) + ' · 输出 ' + fmtNum(td.outputTokens));
            $('#gwUsageChips').html(
                chip('总请求', t.count || 0) + chip('输入 tokens', fmtNum(t.inputTokens)) + chip('输出 tokens', fmtNum(t.outputTokens)) +
                chip('今日请求', td.count || 0) + chip('今日输入', fmtNum(td.inputTokens)) + chip('今日输出', fmtNum(td.outputTokens)));
            var row = function (cols) { return '<tr>' + cols.map(function (c) { return '<td>' + esc(c) + '</td>'; }).join('') + '</tr>'; };
            var $u = $('#gwUsageByUser').html('');
            (b.byUser || []).forEach(function (x) {
                $u.append(row([x.ownerName || x.userId, x.count, fmtNum(x.inputTokens), fmtNum(x.outputTokens)]));
            });
            if (!(b.byUser || []).length) $u.append('<tr><td colspan="4" style="color:var(--text-5);">暂无数据</td></tr>');
            var $m = $('#gwUsageByModel').html('');
            (b.byModel || []).forEach(function (x) {
                $m.append(row([x.usedModel || '(未知)', x.count, fmtNum(x.inputTokens), fmtNum(x.outputTokens)]));
            });
            if (!(b.byModel || []).length) $m.append('<tr><td colspan="4" style="color:var(--text-5);">暂无数据</td></tr>');
        });
        var filterUser = $('#gwUsageUser').val() || '';
        var filterDate = $('#gwUsageDate').val() || '';
        $.getJSON('gateway/admin/usage/logs', {
            pageNum: state.usagePage, pageSize: 20,
            userId: filterUser, date: filterDate
        }).done(function (res) {
            if (res.errorCode !== '000000') return;
            var b = res.body || {};
            var total = b.total || 0;
            var pages = Math.max(1, Math.ceil(total / (b.pageSize || 20)));
            $('#gwUsagePageInfo').text('第 ' + (b.pageNum || 1) + ' / ' + pages + ' 页 · 共 ' + total + ' 条');
            var $tb = $('#gwUsageLogBody').html('');
            (b.list || []).forEach(function (x) {
                var $tr = $('<tr/>');
                $tr.append('<td>' + esc(x.createTime) + '</td>');
                $tr.append('<td>' + esc(x.ownerName || x.userId) + '</td>');
                $tr.append('<td>' + esc(x.requestedModel || '(默认)') + '</td>');
                $tr.append('<td>' + esc(x.usedModel || '?') + '</td>');
                $tr.append('<td>' + esc(x.account || '-') + '</td>');
                $tr.append('<td>' + (x.durationMs != null ? Math.round(x.durationMs / 100) / 10 + 's' : '-') + '</td>');
                $tr.append('<td>' + (x.status === 'ok' ? '<span style="color:#4ade80;">成功</span>' : '<span style="color:#f87171;">失败</span>' + (x.errorKind ? ' <span style="font-size:11px;color:var(--text-5);">' + esc(x.errorKind) + '</span>' : '')) + '</td>');
                $tr.append('<td><button class="btn-main gw-usage-detail" data-id="' + x.id + '" style="padding:2px 8px;font-size:12px;">详情</button></td>');
                $tb.append($tr);
            });
            if (!(b.list || []).length) $tb.append('<tr><td colspan="8" style="color:var(--text-5);">暂无调用记录</td></tr>');
        });
    }

    /* ================= 积分自动化 ================= */

    var AUTO_KIND_LABEL = { checkin: '签到', report: '活跃上报', events: '事件链补分', tasks: '任务领奖', streak: '连登兑换+抽奖', travel: '猫猫旅行', compensation: '故障补偿' };

    function renderAuto(b) {
        b = b || state.autoData || {};
        $('#gwAutoEnabled').prop('checked', !!b.enabled);
        $('#gwAutoEnabledLabel').text(b.enabled ? '已开启（按配置小时自动执行）' : '已关闭');
        var $t = $('#gwAutoBody').html('');
        (b.jobs || []).forEach(function (j) {
            var running = !!state.autoRunning[j.kind];
            var $tr = $('<tr/>');
            $tr.append('<td>' + esc(AUTO_KIND_LABEL[j.kind] || j.kind) + '</td>');
            var hs = (j.hours || []).join(' 点, ') + ' 点';
            $tr.append('<td>' + esc(hs) + '</td>');
            var run = running ? '<span class="gw-spin"></span>执行中…' : (j.lastRunAtMs ? fmtMs(j.lastRunAtMs) : '尚未运行');
            $tr.append('<td>' + run + ' <div style="color:var(--text-5);font-size:11px;">' + esc(j.message || '') + '</div></td>');
            var det = (j.detail || []);
            $tr.append('<td style="font-size:11px;color:var(--text-2);">' + (det.length ? det.map(esc).join('<br>') : '<span style="color:var(--text-5);">-</span>') + '</td>');
            $tr.append('<td><button class="btn-main gw-auto-run" data-kind="' + esc(j.kind) + '"' + (running ? ' disabled' : '') + ' style="padding:2px 8px;font-size:12px;">立即执行</button></td>');
            $t.append($tr);
        });
    }

    function loadAutomation() {
        $.getJSON('gateway/admin/automation').done(function (res) {
            if (res.errorCode !== '000000') return;
            state.autoData = res.body || {};
            renderAuto(state.autoData);
        });
    }

    /* ================= 管理员：浏览器扫码登录 ================= */

    var loginTimer = null;

    function stopLoginPolling() {
        if (loginTimer) { clearInterval(loginTimer); loginTimer = null; }
    }

    function setLoginStatus(html, show) {
        var $s = $('#gwLoginStatus');
        if (show === false) { $s.hide().html(''); return; }
        $s.html(html).show();
    }

    function startBrowserLogin() {
        var product = $('#gwLoginProduct').val() || 'buddy';
        stopLoginPolling();
        var $btn = $('#gwBrowserLoginBtn').prop('disabled', true).text('发起中…');
        postJSON('gateway/admin/browser-login/start', { product: product }).done(function (res) {
            $btn.prop('disabled', false).text('取消登录');
            if (res.errorCode !== '000000' || !res.body) {
                gwAlert(res.errorMsg || '发起登录失败');
                $btn.text('🌐 扫码/网页登录');
                return;
            }
            var sessionId = res.body.sessionId;
            var authUrl = res.body.authUrl;
            setLoginStatus('登录链接已生成，<a href="' + esc(authUrl) + '" target="_blank" rel="noopener">点此打开登录页</a>' +
                '（或复制到手机浏览器打开），完成扫码后本页自动检测。<a href="' + esc(authUrl) + '" id="gwLoginCopyUrl" style="color:var(--text-5);font-size:11px;">链接</a>');
            $('#gwLoginCopyUrl').on('click', function (e) {
                e.preventDefault();
                copyText(authUrl, '登录链接已复制');
            });
            var deadline = Date.now() + 5 * 60 * 1000;
            loginTimer = setInterval(function () {
                if (Date.now() > deadline) {
                    stopLoginPolling();
                    setLoginStatus('登录超时，请重新发起。', true);
                    $('#gwBrowserLoginBtn').text('🌐 扫码/网页登录');
                    return;
                }
                postJSON('gateway/admin/browser-login/poll', { sessionId: sessionId }).done(function (r) {
                    if (r.errorCode !== '000000' || !r.body) return;
                    var b = r.body;
                    if (b.status === 'authorized') {
                        stopLoginPolling();
                        setLoginStatus('🟢 ' + esc(b.message || '登录成功，账号已入池'), true);
                        $('#gwBrowserLoginBtn').text('🌐 扫码/网页登录');
                        refreshStatus();
                    } else if (b.status === 'failed') {
                        stopLoginPolling();
                        setLoginStatus('🔴 ' + esc(b.message || '登录失败'), true);
                        $('#gwBrowserLoginBtn').text('🌐 扫码/网页登录');
                    } else {
                        setLoginStatus('🟡 等待扫码授权中…（<a href="' + esc(authUrl) + '" target="_blank" rel="noopener">打开登录页</a>）', true);
                    }
                }).fail(function () { /* 网络抖动：下个周期重试 */ });
            }, 2000);
        }).fail(function (xhr) {
            $btn.prop('disabled', false).text('🌐 扫码/网页登录');
            var r = xhr.responseJSON || {};
            gwAlert(r.errorMsg || '发起登录失败');
        });
    }

    function cancelBrowserLogin() {
        stopLoginPolling();
        setLoginStatus('', false);
        $('#gwBrowserLoginBtn').text('🌐 扫码/网页登录');
    }

    /* ================= 事件绑定 ================= */

    function bindEvents() {
        $('#gwRefreshBtn').on('click', function () {
            refreshStatus();
            loadKeys();
            loadModels();
            if (state.admin) loadAdmin();
        });
        /* 说明：调用明细的刷新由日期选择框/用户下拉 change 事件触发，无独立按钮 */

        $('#gwKeyAdd').on('click', function () {
            $('#gwCreatePanel').addClass('show');
            $('#gwKeyName').trigger('focus');
        });

        $('#gwCreateCancel').on('click', function () {
            $('#gwCreatePanel').removeClass('show');
        });

        $('#gwKeyCreate').on('click', function () {
            var name = $.trim($('#gwKeyName').val() || '');
            var model = $('#gwKeyModel').val() || 'auto'; /* auto=跟随系统 */
            postJSON('gateway/keys/add', { name: name, model: model }).done(function (res) {
                if (res.errorCode !== '000000') {
                    gwAlert(res.errorMsg || '创建失败');
                    return;
                }
                $('#gwCreatedKey').text(res.body.apiKey);
                $('#gwCreatedModal').show();
                $('#gwKeyName').val('');
                $('#gwKeyModel').val('auto');
                $('#gwCreatePanel').removeClass('show');
                loadKeys();
            }).fail(function (xhr) {
                var r = xhr.responseJSON || {};
                gwAlert(r.errorMsg || '创建失败，请重试');
            });
        });

        /* 创建成功弹框：一键复制 */
        $('#gwCreatedCopy').on('click', function () {
            copyText($('#gwCreatedKey').text(), 'Key 已复制，请妥善保存');
        });

        $('#gwKeyList').on('click', '.gw-copy', function () {
            var id = Number($(this).data('id'));
            postJSON('gateway/keys/reveal', { id: id }).done(function (res) {
                if (res.errorCode !== '000000' || !res.body) {
                    gwAlert(res.errorMsg || '获取 Key 失败');
                    return;
                }
                copyText(res.body, 'Key 已复制到剪贴板');
            }).fail(function (xhr) {
                var r = xhr.responseJSON || {};
                gwAlert(r.errorMsg || '获取 Key 失败');
            });
        });

        $('#gwKeyList').on('click', '.gw-toggle', function () {
            var enabled = $(this).attr('data-enabled') === '1'; /* 用 attr，避免 .data() 类型转换 */
            postJSON('gateway/keys/toggle', { id: Number($(this).data('id')), enabled: !enabled }).done(function (res) {
                if (res.errorCode === '000000') loadKeys();
                else gwAlert(res.errorMsg || '操作失败');
            });
        });

        $('#gwKeyList').on('click', '.gw-del', function () {
            var id = Number($(this).data('id'));
            if (window.confirmModal) {
                confirmModal('确定删除这把 Key？使用它的 AI 工具将立即失效。', function () {
                    postJSON('gateway/keys/delete', { id: id }).done(function (res) {
                        if (res.errorCode === '000000') loadKeys();
                        else gwAlert(res.errorMsg || '删除失败');
                    });
                });
            } else if (window.confirm('确定删除这把 Key？')) {
                postJSON('gateway/keys/delete', { id: id }).done(function (res) {
                    if (res.errorCode === '000000') loadKeys();
                    else gwAlert(res.errorMsg || '删除失败');
                });
            }
        });

        $('#gwAdminCard').on('click', '.gw-auto-tab', function () {
            var tab = $(this).attr('data-tab');
            $('.gw-auto-tab').removeClass('active');
            $(this).addClass('active');
            $('#gwAutoWb').toggle(tab === 'wb');
            $('#gwAutoQoder').toggle(tab === 'qoder');
            if (tab === 'qoder') loadQoderCheckin();
        });

        $('#gwQoderRefresh').off('click').on('click', loadQoderCheckin);

        /* Qoder device 登录：浏览器授权链接 + 2s 轮询（凭据落盘后签到/对话立即生效） */
        var qoderLoginTimer = null;
        function stopQoderLoginPolling() { if (qoderLoginTimer) { clearInterval(qoderLoginTimer); qoderLoginTimer = null; } }
        function setQoderLoginStatus(html, show) {
            var $s = $('#gwQoderLoginStatus');
            if (show === false) { $s.hide().html(''); return; }
            $s.html(html).show();
        }
        $('#gwQoderLoginBtn').off('click').on('click', function () {
            var $btn = $(this);
            if (qoderLoginTimer) { // 进行中 → 取消
                postJSON('gateway/admin/qoder/device-login/cancel', { sessionId: state.qoderLoginSessionId || '' });
                stopQoderLoginPolling();
                setQoderLoginStatus('', false);
                $btn.text('🌐 扫码/网页登录');
                return;
            }
            $btn.prop('disabled', true).text('发起中…');
            postJSON('gateway/admin/qoder/device-login/start', {}).done(function (res) {
                $btn.prop('disabled', false).text('取消登录');
                if (res.errorCode !== '000000' || !res.body) {
                    gwAlert(res.errorMsg || '发起登录失败');
                    $btn.text('🌐 扫码/网页登录');
                    return;
                }
                state.qoderLoginSessionId = res.body.sessionId;
                var authUrl = res.body.authUrl;
                setQoderLoginStatus('登录链接已生成，<a href="' + esc(authUrl) + '" target="_blank" rel="noopener">点此打开授权页</a>选账号授权，完成后本页自动检测。' +
                    ' <a href="#" id="gwQoderLoginCopy" style="color:var(--text-5);font-size:11px;">复制链接</a>', true);
                $('#gwQoderLoginCopy').on('click', function (e) {
                    e.preventDefault();
                    copyText(authUrl, '授权链接已复制');
                });
                var deadline = Date.now() + 5 * 60 * 1000;
                qoderLoginTimer = setInterval(function () {
                    if (Date.now() > deadline) {
                        stopQoderLoginPolling();
                        setQoderLoginStatus('登录超时，请重新发起。', true);
                        $btn.text('🌐 扫码/网页登录');
                        return;
                    }
                    postJSON('gateway/admin/qoder/device-login/poll', { sessionId: state.qoderLoginSessionId }).done(function (r) {
                        if (r.errorCode !== '000000' || !r.body) return;
                        var b = r.body;
                        if (b.status === 'authorized') {
                            stopQoderLoginPolling();
                            setQoderLoginStatus('🟢 ' + esc(b.message || '登录成功'), true);
                            $btn.text('🌐 扫码/网页登录');
                            loadQoderCheckin();
                        } else if (b.status === 'failed') {
                            stopQoderLoginPolling();
                            setQoderLoginStatus('🔴 ' + esc(b.message || '登录失败'), true);
                            $btn.text('🌐 扫码/网页登录');
                        } else {
                            setQoderLoginStatus('🟡 等待授权中…（<a href="' + esc(authUrl) + '" target="_blank" rel="noopener">打开授权页</a>）', true);
                        }
                    }).fail(function () { /* 网络抖动：下个周期重试 */ });
                }, 2000);
            }).fail(function (xhr) {
                $btn.prop('disabled', false).text('🌐 扫码/网页登录');
                var r = xhr.responseJSON || {};
                gwAlert(r.errorMsg || '发起登录失败');
            });
        });
        $('#gwQoderClaim').off('click').on('click', function () {
            var $btn = $(this).prop('disabled', true).text('领取中…');
            postJSON('gateway/admin/qoder/checkin/claim', {}).done(function (res) {
                $btn.prop('disabled', false).text('🎁 立即领取');
                if (res.errorCode !== '000000') { gwAlert(res.errorMsg || '领取失败'); return; }
                renderQoderCheckin(res.body);
                var c = (res.body && res.body.claim) || {};
                gwAlert(c.message || '已处理');
            }).fail(function () {
                $btn.prop('disabled', false).text('🎁 立即领取');
                gwAlert('领取失败');
            });
        });

        $('#gwFreeSave').off('click').on('click', function () {
            var lines = ($('#gwFreePriority').val() || '').split(/\r?\n/)
                .map(function (s) { return $.trim(s); }).filter(function (s) { return !!s; });
            postJSON('gateway/admin/free-priority', { list: lines }).done(function (res) {
                if (res.errorCode !== '000000') { gwAlert(res.errorMsg || '保存失败'); return; }
                renderFreePriority(res.body);
                gwAlert('优先级已保存');
            }).fail(function () { gwAlert('保存失败'); });
        });

        $('#gwFreeReset').off('click').on('click', function () {
            postJSON('gateway/admin/free-priority', { list: [] }).done(function (res) {
                if (res.errorCode !== '000000') { gwAlert(res.errorMsg || '操作失败'); return; }
                renderFreePriority(res.body);
                gwAlert('已恢复自动顺序');
            }).fail(function () { gwAlert('操作失败'); });
        });

        $('#gwAdminKeyTable').on('change', '.gw-admin-ov', function () {
            postJSON('gateway/admin/keys/switch', { id: Number($(this).data('id')), override: $(this).val() }).done(function (res) {
                if (res.errorCode !== '000000') {
                    gwAlert(res.errorMsg || '设置失败');
                    loadAdminKeys();
                    return;
                }
                gwAlert('已设置');
                loadAdminKeys();
            });
        });

        /* 管理端页签 */
        $('.mm-tabs').on('click', '.mm-tab', function () {
            showAdminTab($(this).attr('data-tab'));
        });
        $('#gwAdminCard').on('click', '[data-goto]', function () {
            showAdminTab($(this).attr('data-goto'));
        });

        /* 明细详情弹框 */
        function openUsageDetail(id) {
            $.getJSON('gateway/admin/usage/detail', { id: id }).done(function (res) {
                if (res.errorCode !== '000000' || !res.body) {
                    gwAlert(res.errorMsg || '详情获取失败');
                    return;
                }
                var b = res.body;
                $('#gwUsageDetailMeta').text((b.ownerName || '') + ' · ' + (b.createTime || '') + ' · ' + (b.usedModel || '?'));
                var fmt = function (s) {
                    if (!s) return '(无)';
                    try { return JSON.stringify(JSON.parse(s), null, 2); } catch (e) { return s; }
                };
                $('#gwUsageDetailReq').text(fmt(b.requestData));
                $('#gwUsageDetailResp').text(fmt(b.responseData));
                $('#gwUsageDetailModal').show();
            }).fail(function (xhr) {
                var r = xhr.responseJSON || {};
                gwAlert(r.errorMsg || '详情获取失败');
            });
        }
        $('#gwUsageDetailClose').on('click', function () { $('#gwUsageDetailModal').hide(); });
        $('#gwUsageLogBody').on('click', '.gw-usage-detail', function () {
            openUsageDetail(Number($(this).data('id')));
        });

        $('#gwUsagePrev').on('click', function () {
            if (state.usagePage > 1) loadUsage(state.usagePage - 1);
        });
        $('#gwUsageNext').on('click', function () {
            loadUsage(state.usagePage + 1);
        });

        $('#gwUsageUser').on('change', function () {
            loadUsage(1);
        });
        $('#gwUsageDate').on('change', function () {
            loadUsage(1);
        });

        $('#gwAutoEnabled').on('change', function () {
            var on = $(this).prop('checked');
            postJSON('gateway/admin/automation/enabled', { enabled: on }).done(function (res) {
                if (res.errorCode === '000000') {
                    $('#gwAutoEnabledLabel').text(on ? '已开启（按配置小时自动执行）' : '已关闭');
                    alterModal(on ? '积分自动化已开启' : '积分自动化已关闭');
                } else alterModal(res.errorMsg || '保存失败');
            }).fail(function () { alterModal('保存失败'); });
        });

        $('#gwAutoRunAll').on('click', function () {
            var $btn = $(this);
            $btn.text('执行中…（约 1 分钟）').prop('disabled', true);
            Object.keys(AUTO_KIND_LABEL).forEach(function (k) { state.autoRunning[k] = true; });
            renderAuto();
            postJSON('gateway/admin/automation/runAll', {}).done(function (res) {
                $btn.text('▶ 一键执行全部').prop('disabled', false);
                state.autoRunning = {};
                if (res.errorCode === '000000') {
                    var rows = res.body || [];
                    alterModal(rows.map(function (s) { return (AUTO_KIND_LABEL[s.kind] || s.kind) + '：' + (s.message || ''); }).join('\n'));
                    loadAutomation();
                } else { alterModal(res.errorMsg || '执行失败'); renderAuto(); }
            }).fail(function () {
                $btn.text('▶ 一键执行全部').prop('disabled', false);
                state.autoRunning = {};
                renderAuto();
                alterModal('执行失败');
            });
        });

        $('#gwAutoBody').on('click', '.gw-auto-run', function () {
            var kind = $(this).data('kind');
            state.autoRunning[kind] = true;
            renderAuto();
            postJSON('gateway/admin/automation/run', { kind: kind }).done(function (res) {
                delete state.autoRunning[kind];
                if (res.errorCode === '000000') {
                    var b = res.body || {};
                    alterModal('执行完成：' + (b.message || ''));
                    loadAutomation();
                } else { alterModal(res.errorMsg || '执行失败'); renderAuto(); }
            }).fail(function () {
                delete state.autoRunning[kind];
                renderAuto();
                alterModal('执行失败');
            });
        });

        $('#gwSwitch').on('change', function () {
            var on = $(this).prop('checked');
            postJSON('gateway/admin/switch', { on: on }).done(function (res) {
                if (res.errorCode !== '000000') {
                    gwAlert(res.errorMsg || '切换失败');
                    $('#gwSwitch').prop('checked', !on);
                    return;
                }
                $('#gwSwitchLabel').text(on ? '开（用户自选模型生效）' : '关（静默映射免费模型）');
            });
        });

        /* 浏览器扫码登录：按钮在管理员加载完成后显示 */
        $('#gwBrowserLoginBtn').show();
        $('#gwBrowserLoginBtn').on('click', function () {
            if (loginTimer) cancelBrowserLogin();
            else startBrowserLogin();
        });
    }

    /* ================= 入口 ================= */

    window.Gateway = {
        init: function () {
            fillUrls();
            if (initialized) {
                /* 再次进入（gateway/modelManage 两个菜单共用本模块）：完整重拉，
                   保证开关等管理配置与其它页面改过的状态同步 */
                refreshStatus();
                loadKeys();
                loadModels().done(loadKeys);
                if (state.admin) loadAdmin();
                return;
            }
            initialized = true;
            bindEvents();
            /* 调用明细日期默认当日（本地时区） */
            if (!$('#gwUsageDate').val()) {
                var d = new Date();
                function p2(n) { return n < 10 ? '0' + n : n; }
                $('#gwUsageDate').val(d.getFullYear() + '-' + p2(d.getMonth() + 1) + '-' + p2(d.getDate()));
            }
            refreshStatus();
            loadModels().done(loadKeys);
            loadAdmin();
        }
    };

})();
