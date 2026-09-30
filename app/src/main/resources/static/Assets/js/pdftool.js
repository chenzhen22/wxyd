/* ============================================================
   证据材料整理（桌面版）— 上传 PDF/图片，按序合并为 A4 竖向带页码 PDF
   后端：/api/pdf/merge（multipart: files + meta JSON）
   依赖：jQuery（与全站一致）
   ============================================================ */
(function () {
    var state = []; // { file: File, label: string, type: 'pdf'|'image' }
    var mounted = false;

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function tip(msg) {
        var $m = $("#pdfMergeMsg");
        if ($m.length) $m.text(msg || '');
    }

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
        for (var i = 0; i < fileList.length; i++) {
            var f = fileList[i];
            var t = fileType(f);
            if (t === 'unknown') {
                tip('已忽略不支持的文件：' + f.name + '（仅限 PDF/PNG/JPG）');
                continue;
            }
            state.push({ file: f, label: parseLabel(f.name), type: t });
        }
        render();
    }

    function render() {
        var $tbl = $("#pdfFileTable");
        var $body = $("#pdfFileTableBody");
        var $empty = $("#pdfEmptyHint");
        var $count = $("#pdfFileCount");
        var $btn = $("#pdfMergeBtn");
        if (!state.length) {
            $tbl.hide();
            $empty.show();
            $count.text('');
            $btn.prop('disabled', true);
            return;
        }
        $empty.hide();
        $tbl.show();
        $count.text('共 ' + state.length + ' 份');
        $btn.prop('disabled', false);
        var html = '';
        for (var i = 0; i < state.length; i++) {
            (function (idx) {
                var it = state[idx];
                var pages = it.type === 'image' ? '1' : 'PDF';
                html += '<tr>' +
                    '<td>' + (idx + 1) + '</td>' +
                    '<td style="word-break:break-all">' + esc(it.file.name) + '</td>' +
                    '<td><input type="text" class="pdf-label-input" data-idx="' + idx +
                        '" value="' + esc(it.label) + '" maxlength="24" style="width:90px;" placeholder="如 101"></td>' +
                    '<td>' + pages + '</td>' +
                    '<td>' +
                    '<button class="btn-api pdf-up" data-idx="' + idx + '"' + (idx === 0 ? ' disabled' : '') + '>↑</button> ' +
                    '<button class="btn-api pdf-down" data-idx="' + idx + '"' + (idx === state.length - 1 ? ' disabled' : '') + '>↓</button> ' +
                    '<button class="btn-del pdf-del" data-idx="' + idx + '">删除</button>' +
                    '</td></tr>';
            })(i);
        }
        $body.html(html);
    }

    function doMerge() {
        if (!state.length) {
            tip('请先添加文件');
            return;
        }
        var mode = $('input[name="pdfMode"]:checked').val() || 'label';
        var fd = new FormData();
        var labels = [];
        for (var i = 0; i < state.length; i++) {
            fd.append('files', state[i].file);
            labels.push(state[i].label || '');
        }
        fd.append('meta', JSON.stringify({ mode: mode, labels: labels }));

        var $btn = $("#pdfMergeBtn");
        $btn.prop('disabled', true);
        tip('正在合并生成，请稍候…');

        var xhr = new XMLHttpRequest();
        xhr.open('POST', 'pdf/merge', true);
        xhr.responseType = 'blob';
        xhr.onload = function () {
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
                tip('已生成并开始下载（' + (blob.size / 1024).toFixed(0) + ' KB，' + state.length + ' 份）');
                $btn.prop('disabled', false);
            } else {
                var reader = new FileReader();
                reader.onload = function () {
                    var msg = '生成失败';
                    try {
                        var r = JSON.parse(reader.result);
                        if (r && r.errorMsg) msg = r.errorMsg;
                    } catch (e) {}
                    tip('错误：' + msg);
                    $btn.prop('disabled', false);
                };
                reader.readAsText(xhr.response);
            }
        };
        xhr.onerror = function () {
            tip('网络错误，请重试');
            $btn.prop('disabled', false);
        };
        xhr.send(fd);
    }

    function init() {
        if (mounted) return;
        mounted = true;

        var $dz = $("#pdfDropZone");
        var $input = $("#pdfFileInput");

        $dz.on("click", function (e) {
            if (e.target.id === "pdfFileInput") return;
            $input.click();
        });
        $input.on("change", function () {
            if (this.files && this.files.length) addFiles(this.files);
            this.value = '';
        });
        $dz.on("dragover dragenter", function (e) {
            e.preventDefault(); e.stopPropagation();
            $dz.addClass("dragover");
        }).on("dragleave", function (e) {
            e.preventDefault(); e.stopPropagation();
            $dz.removeClass("dragover");
        }).on("drop", function (e) {
            e.preventDefault(); e.stopPropagation();
            $dz.removeClass("dragover");
            var dt = e.originalEvent && e.originalEvent.dataTransfer;
            if (dt && dt.files && dt.files.length) addFiles(dt.files);
        });

        // 页码标注输入
        $("#pdfFileTableBody").on("input", ".pdf-label-input", function () {
            var idx = Number($(this).data("idx"));
            if (state[idx]) state[idx].label = $(this).val();
        });
        // 上移
        $("#pdfFileTableBody").on("click", ".pdf-up", function () {
            var idx = Number($(this).data("idx"));
            if (idx > 0) {
                var t = state[idx]; state[idx] = state[idx - 1]; state[idx - 1] = t;
                render();
            }
        });
        // 下移
        $("#pdfFileTableBody").on("click", ".pdf-down", function () {
            var idx = Number($(this).data("idx"));
            if (idx < state.length - 1) {
                var t = state[idx]; state[idx] = state[idx + 1]; state[idx + 1] = t;
                render();
            }
        });
        // 删除
        $("#pdfFileTableBody").on("click", ".pdf-del", function () {
            var idx = Number($(this).data("idx"));
            state.splice(idx, 1);
            render();
        });

        $("#pdfMergeBtn").on("click", doMerge);
    }

    window.PdfTool = { init: init };
})();
