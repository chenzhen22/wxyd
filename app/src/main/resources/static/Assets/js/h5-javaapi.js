/* ============================================================
   H5 · Java8 API 交互式学习模块
   端点（raw JSON，非 Result 包裹）：
   GET  javaapi/classes               → List<ApiClass>
   GET  javaapi/classes/{name}        → ApiClass
   POST javaapi/classes/{name}/test   → CodeResult {success,passed,output,error,executionTime}
   ============================================================ */

registerH5Module('javaapi', function ($page) {

    var classes = null;

    $page.html(
        '<div class="h5-section-title">Java API 交互式学习</div>' +
        '<input class="h5-input" id="jaSearch" placeholder="🔍 搜索 API..." style="margin-bottom:12px">' +
        '<div id="jaListWrap"><div class="h5-empty">加载中…</div></div>' +
        '<div id="jaDetailWrap" style="display:none"></div>');

    function loadList() {
        loading(true);
        api({ url: 'javaapi/classes', type: 'GET' }).done(function (list) {
            loading(false);
            classes = list || [];
            renderList($('#jaSearch').val());
        }).fail(function () {
            loading(false);
            $('#jaListWrap').html('<div class="h5-empty">加载失败</div>');
        });
    }

    function renderList(kw) {
        kw = (kw || '').toLowerCase();
        var html = '';
        (classes || []).forEach(function (c) {
            if (kw && (c.name || '').toLowerCase().indexOf(kw) < 0 &&
                (c.packageName || '').toLowerCase().indexOf(kw) < 0) return;
            html += '<div class="h5-card" data-name="' + esc(c.name) + '" style="cursor:pointer">' +
                '<div class="h5-card-title">' + esc(c.name) + '</div>' +
                '<div class="h5-card-sub">' + esc(c.packageName || '') + '</div></div>';
        });
        $('#jaListWrap').html(html || '<div class="h5-empty">无匹配结果</div>');
    }

    $page.on('input', '#jaSearch', function () { renderList($(this).val()); });

    $page.off('click', '.h5-card[data-name]').on('click', '.h5-card[data-name]', function () {
        openClass($(this).data('name'));
    });

    function openClass(name) {
        loading(true);
        api({ url: 'javaapi/classes/' + encodeURIComponent(name), type: 'GET' }).done(function (c) {
            loading(false);
            var html = '<div class="h5-section-title"><span class="h5-btn-ghost h5-btn-sm" id="jaBack">← 返回列表</span></div>' +
                '<div class="h5-card"><div class="h5-card-title" style="font-size:18px">' + esc(c.name) + '</div>' +
                '<div class="h5-card-tags"><span class="h5-badge h5-badge-role">' + esc(c.packageName || '') + '</span></div>' +
                '<div style="margin-top:10px;color:var(--text-2);font-size:13.5px;white-space:pre-wrap;word-break:break-word">' + esc(c.intro || '') + '</div></div>';
            if ((c.methods || []).length) {
                html += '<div class="h5-section-title">方法列表</div><div class="h5-card">';
                c.methods.forEach(function (m) {
                    html += '<div style="padding:9px 0;border-bottom:1px solid var(--border-1)">' +
                        '<div class="h5-li-title h5-code">' + esc(m.signature || m.name) + '</div>' +
                        (m.description ? '<div class="h5-li-sub">' + esc(m.description) + '</div>' : '') + '</div>';
                });
                html += '</div>';
            }
            if ((c.testCases || []).length) {
                html += '<div class="h5-section-title">在线运行测试</div><div class="h5-card">' +
                    '<div class="h5-field"><label>测试用例</label><select class="h5-select" id="jaCase">' +
                    c.testCases.map(function (t, i) {
                        return '<option value="' + i + '">' + esc(t.name) + (t.description ? ' · ' + esc(t.description) : '') + '</option>';
                    }).join('') +
                    '</select></div>' +
                    '<div class="h5-field"><label>代码</label><textarea class="h5-textarea h5-code" id="jaCode" style="min-height:200px" spellcheck="false"></textarea></div>' +
                    '<button class="h5-btn-primary h5-btn-block" id="jaRun">▶ 运行测试</button>' +
                    '<div class="h5-pre" id="jaOut" style="display:none;margin-top:12px"></div></div>';
            }
            $('#jaListWrap').hide();
            $('#jaDetailWrap').show().html(html);
            $('#jaBack').one('click', function () { $('#jaDetailWrap').hide(); $('#jaListWrap').show(); });

            if ((c.testCases || []).length) {
                var tc = c.testCases;
                /* 代码框自动增高到内容高度，避免内部滚动/被键盘遮挡 */
                function fitCode() {
                    var el = $('#jaCode')[0];
                    if (!el) return;
                    el.style.height = 'auto';
                    el.style.height = Math.max(200, el.scrollHeight + 4) + 'px';
                }
                function fill(i) {
                    $('#jaCode').val(tc[i].code || '');
                    fitCode();
                }
                fill(0);
                $('#jaCode').on('input', fitCode);
                $('#jaCase').on('change', function () { fill(this.value); });
                $('#jaRun').off('click').on('click', function () {
                    var idx = Number($('#jaCase').val()) || 0;
                    var code = $('#jaCode').val();
                    if (!code.trim()) { toast('代码不能为空'); return; }
                    loading(true);
                    api({
                        url: 'javaapi/classes/' + encodeURIComponent(c.name) + '/test',
                        contentType: 'application/json',
                        data: JSON.stringify({ testName: tc[idx].name, code: code })
                    }).done(function (r) {
                        loading(false);
                        var txt = (r.passed ? '✅ 测试通过' : '❌ 测试未通过') +
                            (r.executionTime != null ? ' · ' + r.executionTime + 'ms' : '') + '\n';
                        if (r.output) txt += '---- 输出 ----\n' + r.output + '\n';
                        if (r.error) txt += '---- 错误 ----\n' + r.error;
                        $('#jaOut').show().text(txt || '（无输出）');
                    }).fail(function () { loading(false); toast('运行失败'); });
                });
            }
        }).fail(function () { loading(false); toast('加载失败'); });
    }

    loadList();
});
