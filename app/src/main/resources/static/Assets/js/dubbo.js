/**
 * Dubbo 调用测试工具（移植自 do_dubbo，适配 wxyd 暗色玻璃风格 + jQuery）。
 * 请求配置存于 localStorage（键 dubbo-call-tool.requests），支持新建/保存/重命名/删除/搜索/导入/导出。
 */
var DubboTool = (function ($) {
    var STORAGE_KEY = 'dubbo-call-tool.requests';
    var currentId = null;
    var requests = [];
    var searchTerm = '';

    function gid(id) { return document.getElementById(id); }

    function loadFromStorage() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            var arr = raw ? JSON.parse(raw) : null;
            return Array.isArray(arr) ? arr : [];
        } catch (e) {
            return [];
        }
    }

    function saveToStorage() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(requests));
        } catch (e) { /* 存储不可用则忽略 */ }
    }

    function genId() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    }

    function isNameTaken(name, excludeId) {
        var n = (name || '').trim();
        if (!n) { return false; }
        return requests.some(function (c) { return c.name === n && c.id !== excludeId; });
    }

    function readForm() {
        return {
            id: gid('dubboId').value || null,
            name: gid('dubboName').value.trim(),
            registryAddress: gid('dubboRegistryAddress').value.trim(),
            serviceName: gid('dubboServiceName').value.trim(),
            version: gid('dubboVersion').value.trim(),
            timeout: parseInt(gid('dubboTimeout').value, 10) || 30000,
            interfaceName: gid('dubboInterfaceName').value.trim(),
            methodName: gid('dubboMethodName').value.trim(),
            paramJson: gid('dubboParamJson').value.trim()
        };
    }

    function fillForm(cfg) {
        gid('dubboId').value = cfg.id || '';
        gid('dubboName').value = cfg.name || '';
        gid('dubboRegistryAddress').value = cfg.registryAddress || 'zookeeper://127.0.0.1:2181';
        gid('dubboServiceName').value = cfg.serviceName || '';
        gid('dubboVersion').value = cfg.version || '1.0.0';
        gid('dubboTimeout').value = cfg.timeout || 30000;
        gid('dubboInterfaceName').value = cfg.interfaceName || 'com.ifp.core.flow.service.FlowService';
        gid('dubboMethodName').value = cfg.methodName || 'execute';
        gid('dubboParamJson').value = cfg.paramJson || '';
        currentId = cfg.id || null;
    }

    function clearResult() {
        gid('dubboResult').textContent = '';
        gid('dubboError').textContent = '';
        gid('dubboCost').textContent = '';
    }

    function showError(msg) {
        gid('dubboError').textContent = msg || '未知错误';
    }

    function showResult(cr) {
        gid('dubboCost').textContent = '(' + cr.costMs + ' ms)';
        gid('dubboResult').textContent = cr.success ? JSON.stringify(cr.response, null, 2) : '';
        gid('dubboError').textContent = cr.success ? '' : (cr.error || '');
    }

    function refreshList() {
        var ul = gid('dubboConfigList');
        ul.innerHTML = '';
        var q = searchTerm.trim().toLowerCase();
        requests.forEach(function (cfg) {
            var label = (cfg.name || cfg.id || '');
            if (q && label.toLowerCase().indexOf(q) === -1) { return; }
            var li = document.createElement('li');
            var a = document.createElement('a');
            a.textContent = label;
            a.href = 'javascript:void(0)';
            a.addEventListener('click', function () { loadConfig(cfg.id); });
            var rn = document.createElement('button');
            rn.textContent = '改名';
            rn.className = 'btn-api dubbo-mini';
            rn.addEventListener('click', function (e) {
                e.stopPropagation();
                renameConfig(cfg.id);
            });
            var del = document.createElement('button');
            del.textContent = '删';
            del.className = 'btn-del dubbo-mini';
            del.addEventListener('click', function (e) {
                e.stopPropagation();
                deleteConfig(cfg.id);
            });
            li.appendChild(a);
            li.appendChild(rn);
            li.appendChild(del);
            ul.appendChild(li);
        });
    }

    function loadConfig(id) {
        var cfg = requests.filter(function (c) { return c.id === id; })[0];
        if (cfg) { fillForm(cfg); clearResult(); }
        else { showError('列表中没有找到该请求'); }
    }

    function newRequest() {
        fillForm({});
        clearResult();
    }

    function saveConfig() {
        var cfg = readForm();
        if (!cfg.name) { showError('请填写名称'); return; }
        if (isNameTaken(cfg.name, cfg.id)) {
            var newName = prompt('名称已存在：' + cfg.name + '\n请输入新名称：', cfg.name + '-副本');
            if (!newName || !newName.trim()) { showError('已取消保存'); return; }
            gid('dubboName').value = cfg.name = newName.trim();
            saveConfig();
            return;
        }
        var now = new Date().toISOString();
        if (cfg.id) {
            var idx = -1;
            for (var i = 0; i < requests.length; i++) {
                if (requests[i].id === cfg.id) { idx = i; break; }
            }
            if (idx >= 0) {
                var old = requests[idx];
                cfg.createTime = old.createTime || now;
                cfg.updateTime = now;
                requests[idx] = cfg;
            } else {
                cfg.id = genId();
                cfg.createTime = now;
                cfg.updateTime = now;
                currentId = cfg.id;
                requests.push(cfg);
            }
        } else {
            cfg.id = genId();
            cfg.createTime = now;
            cfg.updateTime = now;
            currentId = cfg.id;
            requests.push(cfg);
        }
        saveToStorage();
        refreshList();
        clearResult();
        gid('dubboId').value = cfg.id;
    }

    function renameConfig(id) {
        var cfg = requests.filter(function (c) { return c.id === id; })[0];
        var newName = prompt('输入新名称：', (cfg && cfg.name) || '');
        if (!newName || !newName.trim()) { return; }
        if (isNameTaken(newName.trim(), id)) { alert('名称已存在'); return; }
        cfg.name = newName.trim();
        cfg.updateTime = new Date().toISOString();
        saveToStorage();
        if (currentId === id) { fillForm(cfg); }
        refreshList();
        clearResult();
    }

    function deleteConfig(id) {
        if (!confirm('确定删除该请求？')) { return; }
        requests = requests.filter(function (c) { return c.id !== id; });
        saveToStorage();
        if (currentId === id) { fillForm({}); }
        refreshList();
        clearResult();
    }

    function exportFile() {
        var data = { version: 1, requests: requests };
        var blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = 'dubbo-configs.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    function importFile(file) {
        if (!file) { return; }
        var reader = new FileReader();
        reader.onload = function (ev) {
            try {
                var obj = JSON.parse(ev.target.result);
                var arr = Array.isArray(obj)
                    ? obj
                    : (obj && Array.isArray(obj.requests)) ? obj.requests : null;
                if (!arr) { throw new Error('文件中缺少请求数组（应为数组或 { version, requests }）'); }
                requests = arr.map(function (c) {
                    if (!c || typeof c !== 'object') { return null; }
                    if (!c.id) { c.id = genId(); }
                    if (!c.name) { c.name = c.id; }
                    return c;
                }).filter(function (c) { return c !== null; });
                saveToStorage();
                fillForm({});
                refreshList();
                clearResult();
            } catch (err) {
                showError('加载文件失败：' + err.message + '（当前列表保持不变）');
            }
        };
        reader.onerror = function () { showError('读取文件失败'); };
        reader.readAsText(file);
        gid('dubboFileInput').value = '';
    }

    function invoke() {
        var cfg = readForm();
        if (!cfg.registryAddress || !cfg.serviceName) { showError('请填写注册中心与服务名'); return; }
        clearResult();
        $.ajax({
            url: 'dubbo/invoke',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(cfg),
            success: function (cr) { showResult(cr); },
            error: function (jqXHR) {
                if (jqXHR.status === 401) return; // 全局 ajaxError 处理并跳转
                showError('调用失败：HTTP ' + jqXHR.status);
            }
        });
    }

    function formatJson() {
        var raw = gid('dubboParamJson').value.trim();
        if (!raw) { return; }
        try {
            gid('dubboParamJson').value = JSON.stringify(JSON.parse(raw), null, 2);
        } catch (err) {
            showError('JSON 格式有误：' + err.message);
        }
    }

    function init() {
        gid('dubboNew').addEventListener('click', newRequest);
        gid('dubboSave').addEventListener('click', saveConfig);
        gid('dubboInvokeTop').addEventListener('click', invoke);
        gid('dubboInvoke').addEventListener('click', invoke);
        gid('dubboRename').addEventListener('click', function () {
            if (currentId) { renameConfig(currentId); } else { showError('请先保存或从左侧列表加载请求'); }
        });
        gid('dubboDelete').addEventListener('click', function () {
            if (currentId) { deleteConfig(currentId); } else { showError('请先保存或从左侧列表加载请求'); }
        });
        gid('dubboFormat').addEventListener('click', formatJson);
        gid('dubboSearch').addEventListener('input', function (e) {
            searchTerm = e.target.value || '';
            refreshList();
        });
        gid('dubboLoadBtn').addEventListener('click', function () { gid('dubboFileInput').click(); });
        gid('dubboFileInput').addEventListener('change', function (e) { importFile(e.target.files[0]); });
        gid('dubboSaveFileBtn').addEventListener('click', exportFile);

        requests = loadFromStorage();
        refreshList();
    }

    return { init: init };
})(jQuery);

$(function () { DubboTool.init(); });
