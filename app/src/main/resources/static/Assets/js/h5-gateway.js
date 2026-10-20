(function () {

    var initialized = false;
    var state = { models: [], defaultModel: '' };

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function postJSON(url, body) {
        return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify(body == null ? {} : { body: body }) });
    }

    /* 复制文本：clipboard API 优先，退回 execCommand */
    function copyText(text, okMsg) {
        var done = function () { toast(okMsg || '已复制'); };
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
                openDialog('<h3>请手动复制</h3><div style="word-break:break-all;color:var(--accent)">' + esc(text) + '</div>' +
                    '<div class="h5-dialog-actions"><button class="h5-btn-primary" onclick="closeDialog()">好的</button></div>');
            }
            document.body.removeChild(ta);
        };
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(done, fail);
        } else {
            fail();
        }
    }

    function fmtNum(n) {
        n = Number(n) || 0;
        return n >= 10000 ? (n / 1000).toFixed(1) + 'k' : String(n);
    }

    function fillUrls() {
        var base = location.origin + '/api'; /* context-path 固定为 /api */
        $('#h5GwUrlAnthropic').text(base + '/anthropic');
        $('#h5GwUrlOpenai').text(base + '/openai/v1');
    }

    /* ================= Key 列表（模型只读展示） ================= */

    function renderKeys(list) {
        var $l = $('#h5GwKeys').html('');
        $('#h5GwKeysEmpty').toggle(!list.length);
        list.forEach(function (k) {
            var $card = $('<div/>').css({ background: 'var(--glass-2)', border: '1px solid var(--glass-5)', borderRadius: '12px', padding: '12px', marginBottom: '10px' });
            $card.append('<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">' +
                '<span style="font-weight:600;color:var(--text-1);">' + esc(k.name || '(未命名)') + '</span>' +
                '<span style="font-size:12px;color:var(--text-5);">' + (k.enabled ? '🟢 启用' : '⏸️ 停用') + '</span></div>');
            var keyRow = $('<div style="display:flex;gap:8px;align-items:center;margin-bottom:6px;"></div>');
            keyRow.append('<code style="flex:1;color:var(--accent);font-size:12px;">' + esc(k.maskedKey) + '</code>');
            keyRow.append('<span class="h5-btn-ghost h5-btn-sm h5-gw-copy" data-id="' + k.id + '">复制</span>');
            $card.append(keyRow);
            $card.append('<div style="font-size:12px;color:var(--text-5);margin-bottom:2px;">模型：<span style="color:var(--text-2);">' + esc(k.model || '跟随请求 / 默认') + '</span></div>');
            $card.append('<div style="font-size:12px;color:var(--text-5);margin-bottom:8px;">最近使用：' + esc(k.lastUsedTime || '从未') + '</div>');
            var ops = $('<div style="display:flex;gap:8px;"></div>');
            ops.append('<span class="h5-btn-ghost h5-btn-sm h5-gw-toggle" data-id="' + k.id + '" data-enabled="' + (k.enabled ? '1' : '0') + '">' + (k.enabled ? '停用' : '启用') + '</span>');
            ops.append('<span class="h5-btn-danger h5-btn-sm h5-gw-del" data-id="' + k.id + '">删除</span>');
            $card.append(ops);
            $l.append($card);
        });
    }

    function loadKeys() {
        $.getJSON('gateway/keys').done(function (res) {
            if (res.errorCode !== '000000') return;
            renderKeys((res.body && res.body.list) || []);
        });
    }

    function loadModels() {
        return $.getJSON('gateway/models').done(function (res) {
            if (res.errorCode !== '000000') return;
            var b = res.body || {};
            state.models = b.models || [];
            state.defaultModel = b.default || '';
            var $sel = $('#h5GwKeyModel').html('<option value="" selected>请选择模型</option>');
            state.models.forEach(function (m) {
                var label = (m === state.defaultModel && state.defaultModel) ? m + '（默认）' : m;
                $sel.append('<option value="' + esc(m) + '">' + esc(label) + '</option>');
            });
        });
    }


    function renderGateway($page) {
        var base = location.origin + '/api';
        $page.html(
            '<div class="h5-section-title">模型网关 </div>' +
            '<div style="margin-bottom:12px;padding:12px;border-radius:12px;background:var(--glass-2);border:1px solid var(--glass-5);font-size:12px;line-height:1.9;color:var(--text-2);">' +
            '<div>[Anthropic] <span style="color:var(--accent);">' + esc(base) + '/anthropic</span></div>' +
            '<div>[OpenAI]&nbsp;&nbsp; <span style="color:var(--accent);">' + esc(base) + '/openai/v1</span></div></div>' +

            '<div style="margin-bottom:12px;padding:12px;border-radius:12px;background:var(--glass-2);border:1px solid var(--glass-5);">' +
            '<div style="font-weight:600;color:var(--text-1);margin-bottom:8px;">🔑 新建 Key <span style="font-weight:400;font-size:12px;color:var(--text-5);">最多 5 把，模型必选</span></div>' +
            '<input id="h5GwKeyName" placeholder="备注名（如：我的手机）" style="width:100%;box-sizing:border-box;padding:10px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);margin-bottom:8px;">' +
            '<div style="display:flex;gap:8px;">' +
            '<select id="h5GwKeyModel" style="flex:1;padding:10px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);"><option value="" selected>请选择模型</option></select>' +
            '<span class="h5-btn-primary" id="h5GwKeyAdd" style="display:flex;align-items:center;">创建</span></div></div>' +

            '<div id="h5GwKeys"></div>' +
            '<div class="h5-empty" id="h5GwKeysEmpty" style="display:none;">还没有 Key，上方创建一把即可接入 AI 工具</div>' +

                        '</div>');

        if (!initialized) {
            initialized = true;
            $('#h5GwKeyAdd').off('click').on('click', function () {
                var name = $.trim($('#h5GwKeyName').val() || '');
                var model = $('#h5GwKeyModel').val() || '';
                if (!model) { toast('请选择模型'); return; }
                postJSON('gateway/keys/add', { name: name, model: model }).done(function (res) {
                    if (res.errorCode !== '000000') { toast(res.errorMsg || '创建失败'); return; }
                    openDialog('<h3>创建成功</h3>' +
                        '<div style="font-size:12px;color:var(--text-5);margin-bottom:6px;">请立即复制保存：</div>' +
                        '<div style="word-break:break-all;color:var(--accent);font-size:13px;margin-bottom:12px;">' + esc(res.body.apiKey) + '</div>' +
                        '<div class="h5-dialog-actions">' +
                        '<button class="h5-btn" onclick="closeDialog()">关闭</button>' +
                        '<button class="h5-btn-primary" id="h5GwCreatedCopy">📋 复制</button></div>');
                    $('#h5GwCreatedCopy').one('click', function () { copyText(res.body.apiKey, 'Key 已复制，请妥善保存'); });
                    $('#h5GwKeyName').val('');
                    loadKeys();
                }).fail(function (xhr) {
                    var r = xhr.responseJSON || {};
                    toast(r.errorMsg || '创建失败，请重试');
                });
            });
            $('#h5GwKeys').on('click', '.h5-gw-copy', function () {
                var id = Number($(this).data('id'));
                postJSON('gateway/keys/reveal', { id: id }).done(function (res) {
                    if (res.errorCode !== '000000' || !res.body) { toast(res.errorMsg || '获取 Key 失败'); return; }
                    copyText(res.body, 'Key 已复制');
                }).fail(function () { toast('获取 Key 失败'); });
            });
            $('#h5GwKeys').on('click', '.h5-gw-toggle', function () {
                var enabled = $(this).attr('data-enabled') === '1'; /* attr 读取，避免 .data() 类型转换 */
                postJSON('gateway/keys/toggle', { id: Number($(this).data('id')), enabled: !enabled }).done(function (res) {
                    if (res.errorCode === '000000') loadKeys();
                    else toast(res.errorMsg || '操作失败');
                });
            });
            $('#h5GwKeys').on('click', '.h5-gw-del', function () {
                var id = Number($(this).data('id'));
                confirm('确定删除这把 Key？使用它的 AI 工具将立即失效。', function () {
                    postJSON('gateway/keys/delete', { id: id }).done(function (res) {
                        if (res.errorCode === '000000') { loadKeys(); toast('已删除'); }
                        else toast(res.errorMsg || '删除失败');
                    });
                });
            });

        }

        loadModels().done(loadKeys);
    }

    registerH5Module('gateway', renderGateway);

})();
