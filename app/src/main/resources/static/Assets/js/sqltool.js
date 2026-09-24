/**
 * SQL 查询工具（仅管理员）— 移植自 assistantMgt 的 sql-query 功能。
 * 依赖 jQuery（项目已引入）。后端接口：sql/tables、sql/query、sql/export。
 */
(function () {
    'use strict';

    var currentSql = '';
    var currentPage = 1;
    var currentSize = 20;
    var totalPages = 1;
    var totalRows = 0;
    var hasResult = false;
    var fullTextData = {};
    var initialized = false;

    var $sqlInput, $executeBtn, $csvBtn, $xlsxBtn, $errorBox, $tableHead, $tableBody, $pag, $resultInfo, $tableSelect;

    function esc(s) {
        if (s === null || s === undefined) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function showError(msg) { $errorBox.text(msg).show(); }
    function clearError() { $errorBox.hide(); }

    // 供 index.js 在确认管理员身份后调用
    window.initSqlTool = function () {
        if (initialized) return;
        initialized = true;

        $sqlInput = $('#sqlInput');
        $executeBtn = $('#sqlExecute');
        $csvBtn = $('#sqlCsv');
        $xlsxBtn = $('#sqlXlsx');
        $errorBox = $('#sqlError');
        $tableHead = $('#sqlTableHead');
        $tableBody = $('#sqlTableBody');
        $pag = $('#sqlPag');
        $resultInfo = $('#sqlResultInfo');
        $tableSelect = $('#sqlTableSelect');

        // 加载表列表
        $.get('sql/tables', function (res) {
            if (res && res.errorCode === '000000' && res.body) {
                $.each(res.body, function (i, name) {
                    $tableSelect.append('<option value="' + esc(name) + '">' + esc(name) + '</option>');
                });
            }
        }, 'json');

        // 选择表 -> 自动生成 SELECT
        $tableSelect.on('change.sql', function () {
            var name = $(this).val();
            if (name) $sqlInput.val('SELECT * FROM ' + name);
        });

        // 清空
        $('#sqlClear').on('click.sql', function () { $sqlInput.val('').focus(); });

        // 执行
        $executeBtn.on('click.sql', executeQuery);

        // Ctrl + Enter 执行
        $sqlInput.on('keydown.sql', function (e) {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault(); executeQuery();
            }
        });

        // 导出
        $csvBtn.on('click.sql', function () { exportData('csv'); });
        $xlsxBtn.on('click.sql', function () { exportData('xlsx'); });

        // 双击查看长文本
        $('#sqlTableWrap').on('dblclick.sql', function (e) {
            var td = $(e.target).closest('td.dblclick-cell');
            if (td.length) {
                var idx = td.attr('data-cell');
                if (idx !== undefined && fullTextData[idx]) {
                    $('#sqlPreviewContent').text(fullTextData[idx]);
                    $('#sqlPreviewMask').addClass('show');
                }
            }
        });
        $('#sqlPreviewMask').on('click.sql', function () { $(this).removeClass('show'); });
        $('#sqlPreviewClose').on('click.sql', function () { $('#sqlPreviewMask').removeClass('show'); });

        // 分页点击（事件委托）
        $pag.on('click.sql', '.pag-btn', function () {
            var p = parseInt($(this).attr('data-p'), 10);
            if (isNaN(p) || p < 1 || p > totalPages || p === currentPage) return;
            executeQueryWithParams(currentSql, p, currentSize);
        });
        $pag.on('change.sql', '.sql-pagesize', function () {
            var s = parseInt($(this).val(), 10);
            if (s !== currentSize) executeQueryWithParams(currentSql, 1, s);
        });
    };

    function executeQuery() {
        var sql = $.trim($sqlInput.val());
        if (!sql) { showError('请输入 SQL 语句'); return; }
        executeQueryWithParams(sql, 1, currentSize);
    }

    function executeQueryWithParams(sql, page, size) {
        currentSql = sql; currentPage = page; currentSize = size;
        $executeBtn.prop('disabled', true).text('⏳ 执行中...');
        clearError();
        $tableBody.html('<tr><td>查询中...</td></tr>');
        $tableHead.html('');
        $pag.hide();
        $resultInfo.text('');

        $.ajax({
            url: 'sql/query', type: 'post', contentType: 'application/json',
            data: JSON.stringify({ body: { sql: sql, page: page, size: size } }),
            dataType: 'json',
            success: function (res) {
                $executeBtn.prop('disabled', false).text('▶ 执行');
                if (!res || res.errorCode !== '000000') {
                    showError((res && res.errorMsg) ? res.errorMsg : '查询失败');
                    $tableBody.html('<tr><td>查询出错</td></tr>');
                    $csvBtn.prop('disabled', true);
                    $xlsxBtn.prop('disabled', true);
                    hasResult = false;
                    return;
                }
                renderResult(res.body || {});
            },
            error: function (xhr) {
                $executeBtn.prop('disabled', false).text('▶ 执行');
                var msg = '请求失败';
                try {
                    var r = JSON.parse(xhr.responseText || '{}');
                    if (r && r.errorMsg) msg = r.errorMsg;
                } catch (e) {}
                showError(msg);
                $tableBody.html('<tr><td>请求出错</td></tr>');
            }
        });
    }

    function renderResult(data) {
        var columns = data.columns || [];
        var rows = data.rows || [];
        totalRows = data.total || 0;
        totalPages = data.totalPages || 0;
        currentPage = data.page || 1;

        $resultInfo.html('共 <strong>' + totalRows + '</strong> 条 · 耗时 <strong>' + (data.elapsedMs || 0) + '</strong> ms');
        $csvBtn.prop('disabled', false);
        $xlsxBtn.prop('disabled', false);
        hasResult = true;

        var thead = '<tr><th>#</th>';
        $.each(columns, function (i, c) { thead += '<th>' + esc(c) + '</th>'; });
        thead += '</tr>';
        $tableHead.html(thead);

        if (rows.length === 0) {
            $tableBody.html('<tr><td colspan="' + (columns.length + 1) + '">暂无数据</td></tr>');
            $pag.hide();
            return;
        }

        var html = '';
        var start = (currentPage - 1) * currentSize + 1;
        fullTextData = {};
        var cellIdx = 0;
        $.each(rows, function (i, row) {
            html += '<tr><td style="color:var(--text-10);font-size:12px;">' + (start + i) + '</td>';
            $.each(columns, function (j, col) {
                var val = row[col];
                var str = (val === null || val === undefined) ? '' : String(val);
                var isLong = str.length > 60;
                fullTextData[cellIdx] = isLong ? str : null;
                var cls = isLong ? ' class="dblclick-cell"' : '';
                html += '<td' + cls + ' data-cell="' + cellIdx + '" title="双击查看完整内容">' + esc(str) + '</td>';
                cellIdx++;
            });
            html += '</tr>';
        });
        $tableBody.html(html);
        renderPagination();
    }

    function renderPagination() {
        if (totalPages <= 1) { $pag.hide(); return; }
        var maxVisible = 7;
        var startP, endP;
        if (totalPages <= maxVisible) {
            startP = 1; endP = totalPages;
        } else {
            var half = Math.floor(maxVisible / 2);
            if (currentPage <= half + 1) { startP = 1; endP = maxVisible; }
            else if (currentPage >= totalPages - half) { startP = totalPages - maxVisible + 1; endP = totalPages; }
            else { startP = currentPage - half; endP = currentPage + half; }
        }

        var h = '';
        if (startP > 1) {
            h += '<button class="pag-btn" data-p="1">1</button>';
            if (startP > 2) h += '<span style="color:var(--text-10);padding:0 2px;">…</span>';
        }
        for (var p = startP; p <= endP; p++) {
            var active = p === currentPage ? ' active' : '';
            h += '<button class="pag-btn' + active + '" data-p="' + p + '">' + p + '</button>';
        }
        if (endP < totalPages) {
            if (endP < totalPages - 1) h += '<span style="color:var(--text-10);padding:0 2px;">…</span>';
            h += '<button class="pag-btn" data-p="' + totalPages + '">' + totalPages + '</button>';
        }

        var prev = '<button class="pag-btn" data-p="' + (currentPage - 1) + '"' + (currentPage <= 1 ? ' disabled' : '') + '>‹</button>';
        var next = '<button class="pag-btn" data-p="' + (currentPage + 1) + '"' + (currentPage >= totalPages ? ' disabled' : '') + '>›</button>';
        var sel = '<select class="sql-pagesize"><option value="10">10/页</option><option value="20" selected>20/页</option><option value="50">50/页</option><option value="100">100/页</option></select>';

        $pag.html(prev + h + next + sel).show();
    }

    function exportData(type) {
        if (!hasResult || !currentSql) { showError('请先执行查询'); return; }
        var url = 'sql/export?type=' + type + '&sql=' + encodeURIComponent(currentSql);
        var a = document.createElement('a');
        a.href = url; a.download = 'query_result.' + type;
        document.body.appendChild(a); a.click(); document.body.removeChild(a);
    }
})();
