/**
 * 报文归档下载前端逻辑
 *
 * 流程：访问目标网址 → 后台解析 HTML → 取 id="xzz" 元素文本 → Base64 解码 → 下载 7z。
 * 成功时接口直接返回二进制附件，解析明细放在 X-Archive-* 响应头；
 * 失败时返回 JSON，因此这里按 Content-Type 分流。
 *
 * 用原生 fetch 而非 jQuery：jQuery 1.8.3 处理 blob 下载不便，
 * 且 fetch 不会触发 index.js 里的全局 401 跳转，便于本模块自行提示。
 */
var Archive = (function () {
    var DEFAULT_URL = 'https://kstest1.kshbank.cn:9091/extService/index.html';
    var DEFAULT_ELEMENT_ID = 'xzz';
    var loaded = false;
    var busy = false;

    function esc(s) {
        if (s === null || s === undefined) return '';
        return String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function fmtBytes(n) {
        n = Number(n);
        if (!isFinite(n) || n < 0) return '-';
        if (n < 1024) return n + ' B';
        if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB';
        return (n / 1024 / 1024).toFixed(2) + ' MB';
    }

    /** 响应头里的中文/特殊字符是 URL 编码的，这里还原 */
    function decodeHeader(v) {
        if (!v) return '';
        try { return decodeURIComponent(v); } catch (e) { return v; }
    }

    function clearResult() {
        $('#arcSteps').empty();
        $('#arcErrTitle').hide();
        $('#arcError').hide().text('');
        $('#arcCost').text('');
    }

    function pushStep(icon, cls, label, detail) {
        $('#arcSteps').append(
            '<div class="archive-step ' + (cls || '') + '">'
            + '<span class="archive-step-icon">' + esc(icon) + '</span>'
            + '<span class="archive-step-label">' + esc(label) + '</span>'
            + (detail ? '<span class="archive-step-detail">' + esc(detail) + '</span>' : '')
            + '</div>'
        );
    }

    function setBusy(b) {
        busy = b;
        $('#arcFetch').prop('disabled', b).text(b ? '处理中...' : '获取并下载');
    }

    function readInfo(resp) {
        var h = function (n) { return resp.headers.get(n) || ''; };
        return {
            name: decodeHeader(h('X-Archive-Name')),
            size: h('X-Archive-Size'),
            b64: h('X-Archive-B64-Len'),
            htmlLen: h('X-Archive-Html-Len'),
            ms: h('X-Archive-Elapsed-Ms'),
            is7z: h('X-Archive-Is7z') === 'true',
            consistent: h('X-Archive-7z-Consistent') === 'true',
            sevenZVersion: h('X-Archive-7z-Version'),
            sha256: h('X-Archive-Sha256'),
            elementId: decodeHeader(h('X-Archive-Element-Id')),
            elementTag: h('X-Archive-Element-Tag')
        };
    }

    function triggerDownload(blob, name) {
        var a = document.createElement('a');
        var objUrl = URL.createObjectURL(blob);
        a.href = objUrl;
        a.download = name || 'archive.7z';
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        // 交给浏览器读完再释放，避免下载被中断
        setTimeout(function () { URL.revokeObjectURL(objUrl); }, 10000);
    }

    function doFetch() {
        if (busy) return;

        var url = $.trim($('#arcUrl').val() || '');
        var elementId = $.trim($('#arcElementId').val() || '') || DEFAULT_ELEMENT_ID;
        var fileName = $.trim($('#arcFileName').val() || '');
        var insecure = $('#arcInsecure').is(':checked');
        var timeoutMs = parseInt($.trim($('#arcTimeout').val() || ''), 10);
        if (isNaN(timeoutMs) || timeoutMs <= 0) timeoutMs = 60000;

        clearResult();

        if (!url) {
            pushStep('✗', 'err', '参数校验失败', '目标网址不能为空');
            return;
        }

        pushStep('→', 'run', '① 目标网址', url);
        pushStep('→', 'run', '② 待提取元素', '#' + elementId);
        setBusy(true);
        var t0 = Date.now();

        fetch('archive/fetch', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json;charset=UTF-8',
                'Accept': 'application/json, application/octet-stream',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify({
                url: url,
                elementId: elementId,
                fileName: fileName || null,
                timeoutMs: timeoutMs,
                insecureTls: insecure
            })
        }).then(function (resp) {
            var ct = (resp.headers.get('Content-Type') || '').toLowerCase();
            if (ct.indexOf('application/json') !== -1) {
                // 失败分支（含未登录 401）
                return resp.text().then(function (txt) {
                    var j = {};
                    try { j = JSON.parse(txt || '{}'); } catch (e) { j = { errorMsg: txt }; }
                    var err = new Error(j.errorMsg || j.message || ('HTTP ' + resp.status));
                    err.json = j;
                    err.status = resp.status;
                    throw err;
                });
            }
            var info = readInfo(resp);
            return resp.blob().then(function (blob) {
                return { blob: blob, info: info };
            });
        }).then(function (r) {
            var info = r.info;
            var elapsed = info.ms ? info.ms + ' ms' : (Date.now() - t0) + ' ms';
            var b64Len = info.b64 ? Number(info.b64).toLocaleString() : '-';

            pushStep('✓', 'ok', '① 访问并解析页面', 'HTML ' + fmtBytes(info.htmlLen));
            pushStep('✓', 'ok', '② 提取 #' + (info.elementId || elementId) + ' 元素',
                '<' + (info.elementTag || '?') + '>，Base64 ' + b64Len + ' 字符');
            pushStep('✓', 'ok', '③ Base64 解码', fmtBytes(info.size));

            if (info.is7z) {
                pushStep('✓', 'ok', '④ 7z 魔数校验通过',
                    '格式版本 ' + (info.sevenZVersion || '-')
                    + (info.consistent ? '，头部长度自洽' : '，但头部长度不自洽，文件可能不完整'));
            } else {
                pushStep('!', 'warn', '④ 7z 魔数校验未通过',
                    '解码结果不是 7z，已按实际内容命名下载，请自行确认');
            }

            pushStep('✓', 'ok', '⑤ 触发下载', (info.name || 'archive.7z') + '（' + fmtBytes(info.size) + '）');
            pushStep('i', 'info', 'SHA-256', info.sha256 || '-');
            $('#arcCost').text('后台耗时 ' + elapsed);

            triggerDownload(r.blob, info.name);

        }).catch(function (err) {
            if (err.status === 401) {
                pushStep('✗', 'err', '会话已过期', '请重新登录后再试');
                setTimeout(function () { location.href = 'login.html'; }, 1500);
                return;
            }
            var j = err.json || {};
            pushStep('✗', 'err', j.stage || '请求失败', j.errorMsg || err.message || '未知错误');
            if (j.httpStatus) {
                pushStep('i', 'info', '上游状态码', 'HTTP ' + j.httpStatus);
            }
            $('#arcErrTitle').show();
            $('#arcError').show().text(j.errorMsg || err.message || '未知错误');
        }).then(function () {
            setBusy(false);
        });
    }

    function init() {
        if (loaded) return;
        loaded = true;

        if (!$('#arcUrl').val()) $('#arcUrl').val(DEFAULT_URL);
        if (!$('#arcElementId').val()) $('#arcElementId').val(DEFAULT_ELEMENT_ID);

        $('#arcFetch').on('click', doFetch);

        $('#arcReset').on('click', function () {
            $('#arcUrl').val(DEFAULT_URL);
            $('#arcElementId').val(DEFAULT_ELEMENT_ID);
            $('#arcFileName').val('');
            $('#arcTimeout').val('60000');
            $('#arcInsecure').prop('checked', false);
            clearResult();
        });

        // 输入框回车直接触发
        $('#arcUrl,#arcElementId,#arcFileName,#arcTimeout').on('keydown', function (e) {
            if (e.keyCode === 13) {
                e.preventDefault();
                doFetch();
            }
        });
    }

    return { init: init };
})();
