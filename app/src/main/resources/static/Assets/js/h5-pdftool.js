/* ============================================================
   证据材料整理（H5 移动端）— 上传 PDF/图片，按序合并为 A4 竖向带页码 PDF
   后端：/api/pdf/merge（multipart: files + meta JSON）
   注册：registerH5Module('pdfTool', renderPdfTool)
   依赖：h5.js 提供的 toast / loading / setFab / esc / fmtSize / showLogin
   ============================================================ */
(function () {
    /* state 在模块闭包内，每次进入模块会重置（renderPdfTool 内 state = []） */
    var state = []; // { file: File, label: string, type: 'pdf'|'image' }

    /* 从文件名提取页码标注：优先 "页码101" / "（101）" / "(101)"，否则取首个数字串 */
    function parseLabel(name) {
        if (!name) return '';
        var m = name.match(/页码[:：]?\s*(\d+)/i)
            || name.match(/[（(]\s*(\d+)\s*[）)]/)
            || name.match(/(\d+)/);
        return m ? m[1] : '';
    }

    function fileType(f) {
        var n = (f.name || '').toLowerCase();
        if (n.endsWith('.pdf') || f.type === 'application/pdf') return 'pdf';
        if (n.endsWith('.png') || n.endsWith('.jpg') || n.endsWith('.jpeg')
            || f.type === 'image/png' || f.type === 'image/jpeg') return 'image';
        return 'unknown';
    }

    function addFiles(fileList) {
        if (!fileList || !fileList.length) return;
        var ignored = [];
        for (var i = 0; i < fileList.length; i++) {
            var f = fileList[i];
            var t = fileType(f);
            if (t === 'unknown') { ignored.push(f.name); continue; }
            state.push({ file: f, label: parseLabel(f.name), type: t });
        }
        if (ignored.length) toast('已忽略：' + ignored.join('、') + '（仅 PDF/PNG/JPG）');
        renderFiles();
    }

    function renderFiles() {
        var $list = $('#pdfFileList');
        var $empty = $('#pdfEmpty');
        var $mergeCard = $('#pdfMergeCard');
        var $count = $('#pdfFileCount');
        if (!state.length) {
            $list.empty();
            $empty.show();
            $mergeCard.hide();
            $count.text('');
            return;
        }
        $empty.hide();
        $mergeCard.show();
        $count.text('共 ' + state.length + ' 份');
        var html = '';
        for (var i = 0; i < state.length; i++) {
            var it = state[i];
            var pages = it.type === 'image' ? '图片 · 1 页' : 'PDF · 多页';
            html += '<div class="h5-card">' +
                '<div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + (i + 1) + '. ' + esc(it.file.name) + '</div>' +
                '<div class="h5-card-sub">' + pages + ' · ' + fmtSize(it.file.size) + '</div>' +
                '</div></div>' +
                '<div class="h5-field" style="margin:8px 0 0"><label>页码标注</label>' +
                '<input class="h5-input pdf-label-input" data-idx="' + i + '" value="' + esc(it.label) + '" maxlength="24" placeholder="如 101 或留空"></div>' +
                '<div class="h5-card-actions">' +
                '<button class="h5-btn h5-btn-sm pdf-up" data-idx="' + i + '"' + (i === 0 ? ' disabled' : '') + '>↑上移</button> ' +
                '<button class="h5-btn h5-btn-sm pdf-down" data-idx="' + i + '"' + (i === state.length - 1 ? ' disabled' : '') + '>↓下移</button> ' +
                '<button class="h5-btn-danger h5-btn-sm pdf-del" data-idx="' + i + '">删除</button>' +
                '</div></div>';
        }
        $list.html(html);
    }

    function doMerge() {
        if (!state.length) { toast('请先添加文件'); return; }
        var mode = $('input[name="pdfMode"]:checked').val() || 'label';
        var fd = new FormData();
        var labels = [];
        for (var i = 0; i < state.length; i++) {
            fd.append('files', state[i].file);
            labels.push(state[i].label || '');
        }
        fd.append('meta', JSON.stringify({ mode: mode, labels: labels }));

        var $btn = $('#pdfMergeBtn');
        var $msg = $('#pdfMergeMsg');
        $btn.prop('disabled', true);
        $msg.text('正在合并生成，请稍候…');
        loading(true);

        var xhr = new XMLHttpRequest();
        xhr.open('POST', 'pdf/merge', true);
        xhr.responseType = 'blob';
        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
        xhr.onload = function () {
            loading(false);
            if (xhr.status === 401) {
                $btn.prop('disabled', false);
                $msg.text('');
                showLogin();
                return;
            }
            var ct = xhr.getResponseHeader('Content-Type') || '';
            if (xhr.status === 200 && ct.indexOf('application/pdf') === 0) {
                var blob = xhr.response;
                var url = URL.createObjectURL(blob);
                var a = document.createElement('a');
                a.href = url;
                a.download = '证据材料（A4竖版·带页码）.pdf';
                document.body.appendChild(a);
                a.click();
                a.remove();
                setTimeout(function () { URL.revokeObjectURL(url); }, 8000);
                $msg.text('已生成并开始下载（' + (blob.size / 1024).toFixed(0) + ' KB，' + state.length + ' 份）');
                $btn.prop('disabled', false);
            } else {
                var reader = new FileReader();
                reader.onload = function () {
                    var m = '生成失败';
                    try { var r = JSON.parse(reader.result); if (r && r.errorMsg) m = r.errorMsg; } catch (e) { }
                    $msg.text('错误：' + m);
                    $btn.prop('disabled', false);
                };
                reader.readAsText(xhr.response);
            }
        };
        xhr.onerror = function () {
            loading(false);
            $msg.text('网络错误，请重试');
            $btn.prop('disabled', false);
        };
        xhr.send(fd);
    }

    function renderPdfTool($page) {
        state = []; /* 每次进入重置 */
        $page.html(
            '<div class="h5-section-title">证据材料整理</div>' +
            '<div class="h5-alert h5-alert-info">上传 PDF / 图片，按序合并为 A4 竖向带页码 PDF（横向内容自动旋转、矢量保真）</div>' +
            '<div class="h5-card">' +
            '<div class="h5-card-title">页码模式</div>' +
            '<div class="h5-field"><label style="display:flex;align-items:center;gap:8px;font-size:13.5px;color:var(--text-2)"><input type="radio" name="pdfMode" value="label" checked> 保留原标注（按下方填写的页码）</label></div>' +
            '<div class="h5-field" style="margin:0"><label style="display:flex;align-items:center;gap:8px;font-size:13.5px;color:var(--text-2)"><input type="radio" name="pdfMode" value="auto"> 顺序重新编号（1、2、3…）</label></div>' +
            '</div>' +
            '<div class="h5-section-title" id="pdfFileCount"></div>' +
            '<div id="pdfFileList"></div>' +
            '<div class="h5-empty" id="pdfEmpty"><button class="h5-btn-primary h5-btn-sm" id="pdfAddEmptyBtn">＋ 添加文件</button></div>' +
            '<div class="h5-card" id="pdfMergeCard" style="display:none">' +
            '<button class="h5-btn-primary h5-btn-block" id="pdfMergeBtn">📄 合并生成 PDF</button>' +
            '<div id="pdfMergeMsg" style="text-align:center;margin-top:8px;font-size:13px;color:var(--text-2);min-height:18px"></div>' +
            '</div>' +
            '<input type="file" id="pdfHiddenInput" multiple accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg" style="display:none">'
        );
        /* 浮动按钮：添加文件 */
        setFab(true, function () { $('#pdfHiddenInput').click(); });
        renderFiles();

        /* 事件委托：进入时先清空 .pdftool 命名空间旧绑定，再统一重绑，避免重复 */
        $page.off('.pdftool');
        $page.on('click.pdftool', '#pdfAddEmptyBtn', function () { $('#pdfHiddenInput').click(); });
        $page.on('change.pdftool', '#pdfHiddenInput', function () {
            if (this.files && this.files.length) addFiles(this.files);
            this.value = '';
        });
        $page.on('input.pdftool', '.pdf-label-input', function () {
            var idx = Number($(this).data('idx'));
            if (state[idx]) state[idx].label = $(this).val();
        });
        $page.on('click.pdftool', '.pdf-up', function () {
            var idx = Number($(this).data('idx'));
            if (idx > 0) { var t = state[idx]; state[idx] = state[idx - 1]; state[idx - 1] = t; renderFiles(); }
        });
        $page.on('click.pdftool', '.pdf-down', function () {
            var idx = Number($(this).data('idx'));
            if (idx < state.length - 1) { var t = state[idx]; state[idx] = state[idx + 1]; state[idx + 1] = t; renderFiles(); }
        });
        $page.on('click.pdftool', '.pdf-del', function () {
            var idx = Number($(this).data('idx'));
            state.splice(idx, 1);
            renderFiles();
        });
        $page.on('click.pdftool', '#pdfMergeBtn', doMerge);
    }

    window.registerH5Module('pdfTool', renderPdfTool);
})();