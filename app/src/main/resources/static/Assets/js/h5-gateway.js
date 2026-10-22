/**
 * 模型网关（H5）：接入地址 + 我的 Key 卡片 + 新建（底部抽屉）。
 */
(function () {

    var initialized = false;
    var state = { models: [], defaultModel: '' };
    var keys = [];

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function postJSON(url, body) {
        return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify(body == null ? {} : { body: body }) });
    }

    function copyText(text, okMsg) {
        var done = function () { toast(okMsg || '已复制'); };
        var fail = function () {
            var ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.opacity = '0';
            document.body.appendChild(ta);
            ta.select();
            try { document.execCommand('copy'); done(); } catch (e) { toast('复制失败，请长按手动复制'); }
            document.body.removeChild(ta);
        };
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(done, fail);
        } else {
            fail();
        }
    }

    var CARD = 'background:var(--glass-2);border:1px solid var(--glass-5);border-radius:14px;padding:14px;margin-bottom:12px;';
    var LAB = 'font-size:11px;color:var(--text-5);margin-bottom:6px;';
    var CODE = 'flex:1;min-width:0;font-size:12px;color:var(--accent);word-break:break-all;';

    function fillUrls() {
        var base = location.origin + '/api';
        $('#h5GwUrlAnthropic').text(base + '/anthropic');
        $('#h5GwUrlOpenai').text(base + '/openai/v1');
        $('#h5GwCopyA').off('click').on('click', function () { copyText($('#h5GwUrlAnthropic').text(), 'Anthropic 地址已复制'); });
        $('#h5GwCopyO').off('click').on('click', function () { copyText($('#h5GwUrlOpenai').text(), 'OpenAI 地址已复制'); });
    }

    function renderStats() {
        var active = 0;
        keys.forEach(function (k) { if (k.enabled) active++; });
        $('#h5GwKeyCount').text(keys.length);
        $('#h5GwKeyActive').text(active);
    }

    function renderKeys(list) {
        keys = list || [];
        var $l = $('#h5GwKeys').html('');
        $('#h5GwKeysEmpty').toggle(!keys.length);
        renderStats();
        keys.forEach(function (k) {
            var status = k.enabled ? (k.adminExempt ? '长期有效' : '今日已启用') : (k.activeDate ? '已过期' : '已停用');
            var color = k.enabled ? '#4ade80' : '#e0a800';
            var $card = $('<div/>').attr('style', CARD);
            $card.append('<div style="display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:8px;">' +
                '<span style="font-weight:600;color:var(--text-1);font-size:14px;">' + esc(k.name || '(未命名)') + '</span>' +
                '<span style="font-size:11px;padding:2px 8px;border-radius:20px;border:1px solid ' + color + '33;color:' + color + ';background:' + color + '14;">' + status + '</span></div>');
            $card.append('<div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">' +
                '<code style="' + CODE + '">' + esc(k.maskedKey) + '</code>' +
                '<span class="h5-btn-ghost h5-btn-sm h5-gw-copy" data-id="' + k.id + '">复制</span></div>');
            $card.append('<div style="font-size:11px;color:var(--text-5);margin-bottom:10px;">' +
                '<span style="padding:1px 8px;border-radius:20px;background:var(--glass-3);color:var(--text-2);margin-right:6px;">' + esc(k.model || '跟随系统') + '</span>' +
                '最近使用：' + esc(k.lastUsedTime || '从未') + '</div>');
            var $ops = $('<div style="display:flex;gap:8px;"></div>');
            $ops.append('<span class="h5-btn-primary h5-btn-sm h5-gw-toggle" data-id="' + k.id + '" data-enabled="' + (k.enabled ? '1' : '0') + '">' + (k.enabled ? '停用' : '启用') + '</span>');
            $ops.append('<span class="h5-btn-ghost h5-btn-sm h5-gw-del" data-id="' + k.id + '">删除</span>');
            $card.append($ops);
            $l.append($card);
        });
    }

    function loadKeys() {
        $.ajax({ url: 'gateway/keys', type: 'GET', dataType: 'json', cache: false }).done(function (res) {
            if (res.errorCode !== '000000') return;
            renderKeys((res.body && res.body.list) || []);
        });
    }

    function loadModels() {
        return $.ajax({ url: 'gateway/models', type: 'GET', dataType: 'json', cache: false }).done(function (res) {
            if (res.errorCode !== '000000') return;
            var b = res.body || {};
            state.models = b.models || [];
            state.defaultModel = b.default || '';
        });
    }

    function modelOptions() {
        var html = '<option value="auto" selected>跟随系统（自动选择模型）</option>';
        (state.models || []).forEach(function (m) {
            var label = (m === state.defaultModel && state.defaultModel) ? m + '（默认）' : m;
            html += '<option value="' + esc(m) + '">' + esc(label) + '</option>';
        });
        return html;
    }

    function openCreate() {
        openSheet('<div style="font-weight:600;color:var(--text-1);margin-bottom:12px;">新建 Key</div>' +
            '<input id="h5GwKeyName" placeholder="备注名（如：我的手机）" style="width:100%;box-sizing:border-box;padding:11px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);margin-bottom:10px;font-size:13px;">' +
            '<select id="h5GwKeyModel" style="width:100%;box-sizing:border-box;padding:11px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);margin-bottom:6px;font-size:13px;">' + modelOptions() + '</select>' +
            '<div style="font-size:11px;color:var(--text-5);line-height:1.7;margin-bottom:12px;">不选模型 = 跟随系统，由网关自动选择可用模型。</div>' +
            '<div class="h5-btn-primary h5-btn-block" id="h5GwKeyCreate">创建</div>');
        $('#h5GwKeyCreate').off('click').on('click', function () {
            var name = $.trim($('#h5GwKeyName').val() || '');
            var model = $('#h5GwKeyModel').val() || 'auto';
            postJSON('gateway/keys/add', { name: name, model: model }).done(function (res) {
                if (res.errorCode !== '000000') { toast(res.errorMsg || '创建失败'); return; }
                closeSheet();
                openDialog('<h3>创建成功</h3>' +
                    '<div style="font-size:12px;color:var(--text-5);margin-bottom:6px;">请立即复制保存，关闭后仍可在列表点「复制」取回：</div>' +
                    '<div style="word-break:break-all;color:var(--accent);font-size:13px;margin-bottom:12px;">' + esc(res.body.apiKey) + '</div>' +
                    '<div class="h5-dialog-actions">' +
                    '<button class="h5-btn" onclick="closeDialog()">关闭</button>' +
                    '<button class="h5-btn-primary" id="h5GwCreatedCopy">📋 复制</button></div>');
                $('#h5GwCreatedCopy').one('click', function () { copyText(res.body.apiKey, 'Key 已复制，请妥善保存'); });
                loadKeys();
            }).fail(function (xhr) {
                var r = xhr.responseJSON || {};
                toast(r.errorMsg || '创建失败，请重试');
            });
        });
    }

    function renderGateway($page) {
        var base = location.origin + '/api';
        $page.html(
            '<div class="h5-section-title">模型网关</div>' +
            '<div style="display:flex;gap:10px;margin-bottom:12px;">' +
            '<div style="flex:1;background:var(--glass-2);border:1px solid var(--glass-5);border-radius:12px;padding:10px;text-align:center;">' +
            '<div style="font-size:20px;font-weight:700;color:var(--text-1);" id="h5GwKeyCount">0</div>' +
            '<div style="font-size:11px;color:var(--text-5);">我的 Key</div></div>' +
            '<div style="flex:1;background:var(--glass-2);border:1px solid var(--glass-5);border-radius:12px;padding:10px;text-align:center;">' +
            '<div style="font-size:20px;font-weight:700;color:#4ade80;" id="h5GwKeyActive">0</div>' +
            '<div style="font-size:11px;color:var(--text-5);">今日可用</div></div></div>' +

            '<div style="' + CARD + '">' +
            '<div style="font-weight:600;color:var(--text-1);font-size:13px;margin-bottom:10px;">🔌 接入地址</div>' +
            '<div style="' + LAB + '">Anthropic 协议 · Claude Code</div>' +
            '<div style="display:flex;align-items:center;gap:8px;margin-bottom:10px;">' +
            '<code style="' + CODE + '" id="h5GwUrlAnthropic"></code>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5GwCopyA">复制</span></div>' +
            '<div style="' + LAB + '">OpenAI 协议 · SDK</div>' +
            '<div style="display:flex;align-items:center;gap:8px;">' +
            '<code style="' + CODE + '" id="h5GwUrlOpenai"></code>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5GwCopyO">复制</span></div>' +
            '<div style="font-size:11px;color:var(--text-5);line-height:1.7;margin-top:10px;">Claude Code：ANTHROPIC_BASE_URL 填上面的地址，ANTHROPIC_AUTH_TOKEN 填你的 Key。</div>' +
            '</div>' +

            '<div class="h5-btn-primary h5-btn-block" id="h5GwAddBtn" style="margin-bottom:12px;">＋ 新建 Key</div>' +

            '<div id="h5GwKeys"></div>' +
            '<div class="h5-empty" id="h5GwKeysEmpty" style="display:none;">还没有 Key，点上方「＋ 新建 Key」创建第一把</div>' +

            '<div style="font-size:11px;color:var(--text-5);line-height:1.8;margin-top:6px;">' +
            '⚠️ Key 仅在启用当天有效，次日自动停用，需每天点一次「启用」；管理员账号的 Key 长期有效。</div>'
        );

        if (!initialized) {
            initialized = true;
            $page.on('click', '#h5GwAddBtn', openCreate);
            $page.on('click', '.h5-gw-copy', function () {
                var id = Number($(this).data('id'));
                postJSON('gateway/keys/reveal', { id: id }).done(function (res) {
                    if (res.errorCode !== '000000' || !res.body) { toast(res.errorMsg || '获取 Key 失败'); return; }
                    copyText(res.body, 'Key 已复制');
                }).fail(function () { toast('获取 Key 失败'); });
            });
            $page.on('click', '.h5-gw-toggle', function () {
                var enabled = $(this).attr('data-enabled') === '1';
                postJSON('gateway/keys/toggle', { id: Number($(this).data('id')), enabled: !enabled }).done(function (res) {
                    if (res.errorCode === '000000') { loadKeys(); toast(enabled ? '已停用' : '已启用，今天可用'); }
                    else toast(res.errorMsg || '操作失败');
                });
            });
            $page.on('click', '.h5-gw-del', function () {
                var id = Number($(this).data('id'));
                confirm('确定删除这把 Key？使用它的 AI 工具将立即失效。', function () {
                    postJSON('gateway/keys/delete', { id: id }).done(function (res) {
                        if (res.errorCode === '000000') { loadKeys(); toast('已删除'); }
                        else toast(res.errorMsg || '删除失败');
                    });
                });
            });
        }

        fillUrls();
        loadModels().done(loadKeys);
    }

    registerH5Module('gateway', renderGateway);

})();
