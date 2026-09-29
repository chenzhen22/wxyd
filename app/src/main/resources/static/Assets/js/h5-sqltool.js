/* ============================================================
   H5 · SQL 查询工具（仅管理员）
   端点（Result 包裹，export 为附件下载）：
   GET  sql/tables                          → List<String>
   POST sql/query  {body:{sql,page,size}}   → SqlQueryResultVO {columns,rows,total,page,size,totalPages,elapsedMs}
   GET  sql/export?type=csv|xlsx&sql=...    → 附件（session cookie 鉴权）
   ============================================================ */

registerH5Module('sql', function ($page) {

    if (!H5State.isAdmin) {
        $page.html('<div class="h5-empty">权限不足，仅超级管理员可使用 SQL 查询</div>');
        return;
    }

    $page.html(
        '<div class="h5-section-title">SQL 查询<span class="h5-btn-ghost h5-btn-sm" id="sqlTablesBtn">选表</span></div>' +
        '<div class="h5-card">' +
        '<div class="h5-field"><textarea class="h5-textarea h5-code" id="sqlInput" style="min-height:120px" spellcheck="false">SELECT * FROM users LIMIT 20</textarea></div>' +
        '<div style="display:flex;gap:8px">' +
        '<button class="h5-btn-primary" id="sqlRun" style="flex:1">▶ 执行</button>' +
        '<button class="h5-btn" id="sqlCsv">CSV</button>' +
        '<button class="h5-btn" id="sqlXlsx">Excel</button>' +
        '</div></div>' +
        '<div id="sqlResult"></div>');

    var page = 1, size = 20;

    $('#sqlTablesBtn').on('click', function () {
        loading(true);
        api({ url: 'sql/tables', type: 'GET' }).done(function (res) {
            loading(false);
            var list = (res.errorCode === '000000' && res.body) || [];
            openSheet('<div class="h5-sheet-head"><h3>选择表</h3></div><div class="h5-theme-list">' +
                (list.map(function (t) { return '<div class="h5-theme-opt" data-t="' + esc(t) + '">' + esc(t) + '</div>'; }).join('') ||
                    '<div class="h5-empty">无表</div>') + '</div>');
            $('#h5Sheet .h5-theme-opt').one('click', function () {
                $('#sqlInput').val('SELECT * FROM ' + $(this).data('t') + ' LIMIT 20');
                closeSheet();
            });
        }).fail(function () { loading(false); toast('获取表列表失败'); });
    });

    function run() {
        var sql = $('#sqlInput').val().trim();
        if (!sql) { toast('SQL 不能为空'); return; }
        loading(true);
        postJSON('sql/query', { sql: sql, page: page, size: size }).done(function (res) {
            loading(false);
            if (res.errorCode !== '000000') {
                $('#sqlResult').html('<div class="h5-alert h5-alert-err">' + esc(res.errorMsg || '查询失败') + '</div>');
                return;
            }
            var vo = res.body || {};
            var cols = vo.columns || [], rows = vo.rows || [];
            var html = '<div class="h5-alert h5-alert-info">共 ' + vo.total + ' 条 · 第 ' + vo.page + '/' + vo.totalPages +
                ' 页 · ' + vo.elapsedMs + 'ms</div>';
            if (!rows.length) {
                html += '<div class="h5-empty">无数据</div>';
            } else {
                html += '<div class="h5-table-wrap"><table class="h5-table"><thead><tr>' +
                    cols.map(function (c) { return '<th>' + esc(c) + '</th>'; }).join('') +
                    '</tr></thead><tbody>' +
                    rows.map(function (r) {
                        return '<tr>' + cols.map(function (c) {
                            var v = r[c];
                            return '<td>' + esc(v == null ? 'NULL' : (typeof v === 'object' ? JSON.stringify(v) : String(v))) + '</td>';
                        }).join('') + '</tr>';
                    }).join('') + '</tbody></table></div>';
                if (vo.totalPages > 1) {
                    html += '<div style="display:flex;gap:8px;margin-top:12px">' +
                        '<button class="h5-btn" id="sqlPrev" style="flex:1"' + (vo.page <= 1 ? ' disabled' : '') + '>上一页</button>' +
                        '<button class="h5-btn" id="sqlNext" style="flex:1"' + (vo.page >= vo.totalPages ? ' disabled' : '') + '>下一页</button>' +
                        '</div>';
                }
            }
            $('#sqlResult').html(html);
            $('#sqlPrev').on('click', function () { if (page > 1) { page--; run(); } });
            $('#sqlNext').on('click', function () { if (page < vo.totalPages) { page++; run(); } });
        }).fail(function () {
            loading(false);
            $('#sqlResult').html('<div class="h5-alert h5-alert-err">请求失败</div>');
        });
    }

    $('#sqlRun').on('click', function () { page = 1; run(); });

    function doExport(type) {
        var sql = $('#sqlInput').val().trim();
        if (!sql) { toast('SQL 不能为空'); return; }
        var a = document.createElement('a');
        a.href = 'sql/export?type=' + type + '&sql=' + encodeURIComponent(sql);
        document.body.appendChild(a);
        a.click();
        a.remove();
    }
    $('#sqlCsv').on('click', function () { doExport('csv'); });
    $('#sqlXlsx').on('click', function () { doExport('xlsx'); });
});
