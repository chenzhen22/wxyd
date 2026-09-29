/* ============================================================
   H5 · Dubbo 泛化调用测试工具
   端点（raw JSON，非 Result 包裹）：
   POST dubbo/invoke  body=DubboConfig  → CallResult {success,costMs,response,error}
   已保存配置存 localStorage（key: wxyd-h5-dubbo-configs）。
   ============================================================ */

registerH5Module('dubbo', function ($page) {

    var KEY = 'wxyd-h5-dubbo-configs';
    var currentId = null;

    function loadConfigs() {
        try { return JSON.parse(localStorage.getItem(KEY)) || []; } catch (e) { return []; }
    }
    function saveConfigs(list) {
        try { localStorage.setItem(KEY, JSON.stringify(list)); } catch (e) { }
    }
    function refreshCnt() { $('#dbCnt').text(loadConfigs().length); }

    $page.html(
        '<div class="h5-section-title">Dubbo 调用<span class="h5-btn-ghost h5-btn-sm" id="dbSavedBtn">已保存(<span id="dbCnt">0</span>)</span></div>' +
        '<div class="h5-card">' +
        '<div class="h5-field"><label>名称</label><input class="h5-input" id="dbName" placeholder="如：CPR020181-省别查询"></div>' +
        '<div class="h5-field"><label>注册中心</label><input class="h5-input h5-code" id="dbRegistry"></div>' +
        '<div class="h5-field"><label>服务名（Dubbo group）</label><input class="h5-input h5-code" id="dbService" placeholder="如：dubbov_CPR020181Flow"></div>' +
        '<div class="h5-field"><label>版本</label><input class="h5-input h5-code" id="dbVersion" value="1.0.0"></div>' +
        '<div class="h5-field"><label>接口</label><input class="h5-input h5-code" id="dbIface"></div>' +
        '<div class="h5-field"><label>方法</label><input class="h5-input h5-code" id="dbMethod"></div>' +
        '<div class="h5-field"><label>超时(ms)</label><input class="h5-input" id="dbTimeout" type="number" value="30000"></div>' +
        '<div class="h5-field"><label>请求参数 JSON（顶层 reqId + dataMap）</label><textarea class="h5-textarea h5-code" id="dbParam" style="min-height:150px" spellcheck="false"></textarea></div>' +
        '<div style="display:flex;gap:8px">' +
        '<button class="h5-btn-primary" id="dbInvoke" style="flex:1">调用</button>' +
        '<button class="h5-btn" id="dbSave">保存</button>' +
        '<button class="h5-btn" id="dbClear">清空</button>' +
        '</div></div>' +
        '<div id="dbResult"></div>');

    function collect() {
        return {
            id: currentId,
            name: $('#dbName').val().trim() || '未命名',
            registryAddress: $('#dbRegistry').val().trim(),
            serviceName: $('#dbService').val().trim(),
            version: $('#dbVersion').val().trim(),
            interfaceName: $('#dbIface').val().trim(),
            methodName: $('#dbMethod').val().trim(),
            timeout: parseInt($('#dbTimeout').val(), 10) || 30000,
            paramJson: $('#dbParam').val()
        };
    }

    function apply(c) {
        currentId = c.id || null;
        $('#dbName').val(c.name || '');
        $('#dbRegistry').val(c.registryAddress || '');
        $('#dbService').val(c.serviceName || '');
        $('#dbVersion').val(c.version || '1.0.0');
        $('#dbIface').val(c.interfaceName || '');
        $('#dbMethod').val(c.methodName || '');
        $('#dbTimeout').val(c.timeout || 30000);
        $('#dbParam').val(c.paramJson || '');
        $('#dbResult').empty();
    }

    /* 默认值（与桌面版一致） */
    apply({
        registryAddress: 'zookeeper://127.0.0.1:2181',
        version: '1.0.0',
        interfaceName: 'com.ifp.core.flow.service.FlowService',
        methodName: 'execute',
        timeout: 30000,
        paramJson: '{"reqId":"$reqId","dataMap":{"provinceCode":"44"}}'
    });

    /* 已保存配置 */
    $('#dbSavedBtn').on('click', function () {
        var list = loadConfigs();
        openSheet('<div class="h5-sheet-head"><h3>已保存配置</h3></div>' +
            (list.length ? list.map(function (c, i) {
                return '<div class="h5-card" style="margin-bottom:8px"><div class="h5-card-row">' +
                    '<div style="flex:1;min-width:0"><div class="h5-card-title">' + esc(c.name) + '</div>' +
                    '<div class="h5-card-sub">' + esc(c.serviceName || '') + ' · ' + esc(c.methodName || '') + '</div></div>' +
                    '<div><button class="h5-btn-primary h5-btn-sm" data-i="' + i + '" data-act="load">载入</button> ' +
                    '<button class="h5-btn-danger h5-btn-sm" data-i="' + i + '" data-act="del">删除</button></div>' +
                    '</div></div>';
            }).join('') : '<div class="h5-empty">暂无保存的配置</div>'));
        $('#h5Sheet [data-act]').on('click', function () {
            var i = Number($(this).data('i')), act = $(this).data('act');
            var list = loadConfigs();
            if (act === 'load') { apply(list[i]); closeSheet(); }
            else { list.splice(i, 1); saveConfigs(list); refreshCnt(); closeSheet(); toast('已删除'); }
        });
    });

    $('#dbSave').on('click', function () {
        var c = collect();
        if (!c.registryAddress || !c.interfaceName) { toast('请填写注册中心与接口'); return; }
        var list = loadConfigs();
        if (!c.id) c.id = 'h5-' + Date.now();
        var idx = -1;
        for (var i = 0; i < list.length; i++) { if (list[i].id === c.id) { idx = i; break; } }
        if (idx >= 0) list[idx] = c; else list.push(c);
        saveConfigs(list);
        currentId = c.id;
        refreshCnt();
        toast('已保存');
    });

    $('#dbClear').on('click', function () {
        apply({
            registryAddress: 'zookeeper://127.0.0.1:2181',
            version: '1.0.0',
            interfaceName: 'com.ifp.core.flow.service.FlowService',
            methodName: 'execute',
            timeout: 30000,
            paramJson: '{"reqId":"$reqId","dataMap":{"provinceCode":"44"}}'
        });
    });

    $('#dbInvoke').on('click', function () {
        var c = collect();
        if (!c.registryAddress || !c.interfaceName) { toast('请填写注册中心与接口'); return; }
        try { JSON.parse(c.paramJson || '{}'); } catch (e) { toast('参数 JSON 格式错误'); return; }
        loading(true);
        api({
            url: 'dubbo/invoke', contentType: 'application/json',
            data: JSON.stringify($.extend({}, c, { id: c.id || undefined }))
        }).done(function (r) {
            loading(false);
            var detail = r.success
                ? (typeof r.response === 'string' ? r.response : JSON.stringify(r.response, null, 2))
                : (r.error || '');
            $('#dbResult').html('<div class="h5-card">' +
                '<div class="h5-card-title">' + (r.success ? '✅ 调用成功' : '❌ 调用失败') +
                ' <span class="h5-card-sub" style="display:inline;margin-left:6px">' + (r.costMs != null ? r.costMs + 'ms' : '') + '</span></div>' +
                '<div class="h5-pre" style="margin-top:10px;max-height:420px">' + esc(detail || '（空响应）') + '</div></div>');
        }).fail(function () { loading(false); toast('请求失败'); });
    });
});
