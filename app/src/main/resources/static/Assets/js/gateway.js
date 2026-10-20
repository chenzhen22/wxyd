(function () {

    var initialized = false;
    var state = { models: [], defaultModel: '', admin: false, usagePage: 1 };

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
            $t.append('<tr><td colspan="5" style="color:var(--text-5);">池内没有账号：在桌面 App 登录后点「刷新」，或检查 wxyd.wbgateway.workbuddy-exe 配置</td></tr>');
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
            $tr.append('<td>' + esc(r.source === 'live' ? '桌面登录' : '历史快照') + ' <span style="color:var(--text-5);">' + esc(r.domain || '') + '</span></td>');
            $tr.append('<td>' + credits + '</td>');
            $tr.append('<td><select class="gw-pool-model" style="width:100%;padding:3px 6px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);font-size:12px;">' + opts + '</select></td>');
            $tr.append('<td>' + fmtMs(r.expiresAtMs) + '</td>');
            $t.append($tr);
        });
    }

    /* ================= 接入地址 ================= */

    function fillUrls() {
        var base = location.origin + '/api'; /* context-path 固定为 /api */
        $('#gwUrlAnthropic').text(base + '/anthropic');
        $('#gwUrlOpenai').text(base + '/openai/v1');
    }

    /* ================= Key 列表（模型只读展示，不可修改） ================= */

    function renderKeys(list, max) {
        var $tb = $('#gwKeyTableBody').html('');
        $('#gwKeyEmpty').toggle(!list.length);
        list.forEach(function (k) {
            var $tr = $('<tr/>');
            $tr.append('<td>' + esc(k.name || '(未命名)') + '</td>');
            var keyTd = $('<td/>');
            keyTd.append('<code style="color:var(--accent);">' + esc(k.maskedKey) + '</code> ');
            keyTd.append('<button class="btn-main gw-copy" data-id="' + k.id + '" style="padding:2px 8px;font-size:12px;">复制</button>');
            $tr.append(keyTd);
            $tr.append('<td><input type="text" readonly value="' + esc(k.model || '跟随请求 / 默认') + '" ' +
                'style="width:100%;padding:4px 6px;border-radius:6px;background:var(--glass-2);color:var(--text-2);border:1px solid var(--glass-5);font-size:12px;"></td>');
            $tr.append('<td>' + (k.enabled ? '🟢 启用' : '⏸️ 停用') + '</td>');
            $tr.append('<td>' + esc(k.lastUsedTime || '从未') + '</td>');
            var ops = $('<td/>');
            /* data-enabled 用 attr 读取，避免 jQuery .data() 把 "1" 自动转成数字导致判断失效 */
            ops.append('<button class="btn-main gw-toggle" data-id="' + k.id + '" data-enabled="' + (k.enabled ? '1' : '0') + '" style="padding:3px 8px;font-size:12px;margin-right:4px;">' + (k.enabled ? '停用' : '启用') + '</button>');
            ops.append('<button class="btn-main gw-del" data-id="' + k.id + '" style="padding:3px 8px;font-size:12px;">删除</button>');
            $tr.append(ops);
            $tb.append($tr);
        });
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
            /* 创建 Key：模型必选，首项为「请选择模型」占位；目录默认模型在文字里标注 */
            var $sel = $('#gwKeyModel').html('<option value="" selected>请选择模型</option>');
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
        $.getJSON('gateway/admin/config').done(function (res) {
            if (res.errorCode !== '000000') return; /* 非管理员：状态/刷新按钮与整个管理区块都保持隐藏 */
            state.admin = true;
            /* 状态徽标与刷新按钮仅管理员可见（gateway/status 接口也仅管理员可调） */
            $('#gwStatusBadge, #gwRefreshBtn, #mmRefreshBtn').show();
            refreshStatus();
            $('#gwAdminCard').show();
            var on = res.body && res.body.modelSwitch;
            $('#gwSwitch').prop('checked', !!on);
            $('#gwSwitchLabel').text(on ? '开（用户自选模型生效）' : '关（静默映射免费模型）');
            loadAdminKeys();
            loadUsage(1);
            $.getJSON('gateway/admin/models').done(function (r2) {
                if (r2.errorCode !== '000000') return;
                var b = r2.body || {};
                $('#gwModelsSource').text(b.source === 'live' ? '实时目录' : '内置兜底目录');
                var $tb = $('#gwAdminModelBody').html('');
                (b.list || []).forEach(function (m) {
                    $tb.append('<tr><td>' + esc(m.id) + '</td><td>' + esc(m.name) + '</td><td>' +
                        (m.free ? '<span style="color:#4ade80;">免费</span>' : '积分计费') + '</td></tr>');
                });
            });
        }).fail(function () { /* 非管理员，保持隐藏 */ });
    }

    /* ================= 管理员：全部用户 Key ================= */

    function loadAdminKeys() {
        $.getJSON('gateway/admin/keys').done(function (res) {
            if (res.errorCode !== '000000') return;
            var list = (res.body && res.body.list) || [];
            var $tb = $('#gwAdminKeyBody').html('');
            $('#gwAdminKeysEmpty').toggle(!list.length);
            list.forEach(function (k) {
                var ov = k.override || 'auto';
                var ovLabel = ov === 'on' ? '开（覆盖）' : ov === 'off' ? '关（覆盖）' : '跟随全局';
                var $tr = $('<tr/>');
                $tr.append('<td>' + esc(k.ownerName || k.username || k.userId) + '</td>');
                $tr.append('<td>' + esc(k.name || '(未命名)') + '</td>');
                $tr.append('<td><code style="color:var(--accent);">' + esc(k.maskedKey) + '</code></td>');
                $tr.append('<td>' + esc(k.model || '跟随请求 / 默认') + '</td>');
                $tr.append('<td>' + (k.enabled ? '🟢' : '⏸️') + '</td>');
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
                var model = esc(x.usedModel || '?') + (x.requestedModel && x.requestedModel !== x.usedModel ? ' <span style="color:var(--text-5);">(' + esc(x.requestedModel) + ')</span>' : '');
                var $tr = $('<tr/>');
                $tr.append('<td>' + esc(x.createTime) + '</td>');
                $tr.append('<td>' + esc(x.ownerName || x.userId) + '</td>');
                $tr.append('<td>' + model + '</td>');
                $tr.append('<td>' + esc(x.protocol) + '</td>');
                $tr.append('<td>' + (x.stream ? '是' : '否') + '</td>');
                $tr.append('<td>' + fmtNum(x.inputTokens) + '</td>');
                $tr.append('<td>' + fmtNum(x.outputTokens) + '</td>');
                $tr.append('<td>' + (x.durationMs != null ? Math.round(x.durationMs / 100) / 10 + 's' : '-') + '</td>');
                $tr.append('<td>' + (x.status === 'ok' ? '<span style="color:#4ade80;">成功</span>' : '<span style="color:#f87171;">失败</span>' + (x.errorKind ? ' <span style="font-size:11px;color:var(--text-5);">' + esc(x.errorKind) + '</span>' : '')) + '</td>');
                $tr.append('<td><button class="btn-main gw-usage-detail" data-id="' + x.id + '" style="padding:2px 8px;font-size:12px;">详情</button></td>');
                $tb.append($tr);
            });
            if (!(b.list || []).length) $tb.append('<tr><td colspan="9" style="color:var(--text-5);">暂无调用记录</td></tr>');
        });
    }

    /* ================= 事件绑定 ================= */

    function bindEvents() {
        $('#gwRefreshBtn').on('click', function () {
            refreshStatus();
            loadKeys();
            loadModels();
            if (state.admin) loadAdmin();
        });
        $('#mmRefreshBtn').on('click', function () {
            refreshStatus();
            if (state.admin) loadAdmin();
        });

        $('#gwKeyAdd').on('click', function () {
            var name = $.trim($('#gwKeyName').val() || '');
            var model = $('#gwKeyModel').val() || '';
            if (!model) {
                gwAlert('请选择模型（模型为必选项）');
                return;
            }
            postJSON('gateway/keys/add', { name: name, model: model }).done(function (res) {
                if (res.errorCode !== '000000') {
                    gwAlert(res.errorMsg || '创建失败');
                    return;
                }
                $('#gwCreatedKey').text(res.body.apiKey);
                $('#gwCreatedModal').show();
                $('#gwKeyName').val('');
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

        $('#gwKeyTable').on('click', '.gw-copy', function () {
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

        $('#gwKeyTable').on('click', '.gw-toggle', function () {
            var enabled = $(this).attr('data-enabled') === '1'; /* 用 attr，避免 .data() 类型转换 */
            postJSON('gateway/keys/toggle', { id: Number($(this).data('id')), enabled: !enabled }).done(function (res) {
                if (res.errorCode === '000000') loadKeys();
                else gwAlert(res.errorMsg || '操作失败');
            });
        });

        $('#gwKeyTable').on('click', '.gw-del', function () {
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

        /* 折叠区块：点击头部切换，默认全部收起 */
        $('.gw-collapse-head').on('click', function () {
            var sec = $('#' + $(this).data('sec'));
            var open = sec.is(':visible');
            sec.toggle(!open);
            $(this).find('.gw-arrow').text(open ? '▸' : '▾');
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
        $('#gwUsageRefresh').on('click', function () {
            loadUsage(state.usagePage);
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
    }

    /* ================= 入口 ================= */

    window.Gateway = {
        init: function () {
            fillUrls();
            if (initialized) {
                refreshStatus();
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
