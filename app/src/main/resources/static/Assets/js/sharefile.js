/**
 * 文件共享模块：列表 / 上传（拖拽，仅管理员）/ 下载 / 删除（仅管理员）。
 * 通过 window.ShareFile.init() 懒加载，由 index.js 在点击菜单时调用。
 */
window.ShareFile = (function () {
    let initialized = false;
    let isAdmin = false;
    let pendingFiles = []; // 已选择待上传的文件

    function fmtSize(bytes) {
        if (bytes == null) return "-";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + " MB";
        return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
    }

    function fmtTime(ts) {
        if (!ts) return "-";
        let d = new Date(ts);
        let p = function (n) { return String(n).padStart(2, "0"); };
        return d.getFullYear() + "-" + p(d.getMonth() + 1) + "-" + p(d.getDate())
            + " " + p(d.getHours()) + ":" + p(d.getMinutes()) + ":" + p(d.getSeconds());
    }

    function escapeHtml(s) {
        return String(s == null ? "" : s)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    function loadList() {
        $.ajax({
            url: "share/list", type: "get", cache: false, dataType: "json",
            success: function (res) {
                let list = (res && res.body) || [];
                $("#shareCount").text("共 " + list.length + " 个文件");
                if (list.length === 0) {
                    $("#shareTableBody").html('<tr><td colspan="99" style="text-align:center;color:#999;">暂无文件</td></tr>');
                    return;
                }
                let html = "";
                for (let i = 0; i < list.length; i++) {
                    let f = list[i];
                    let op = '<a class="btn-api" href="share/download?name='
                        + encodeURIComponent(f.name) + '" target="_blank" rel="noopener">下载</a>';
                    if (isAdmin) {
                        op += ' <button class="btn-del" data-name="' + escapeHtml(f.name) + '">删除</button>';
                    }
                    html += '<tr>'
                        + '<td>' + (i + 1) + '</td>'
                        + '<td>' + escapeHtml(f.name) + '</td>'
                        + '<td>' + fmtSize(f.size) + '</td>'
                        + '<td>' + fmtTime(f.lastModified) + '</td>'
                        + '<td>' + op + '</td>'
                        + '</tr>';
                }
                $("#shareTableBody").html(html);
            },
            error: function () {
                $("#shareTableBody").html('<tr><td colspan="99" style="text-align:center;color:#999;">加载失败</td></tr>');
            }
        });
    }

    function showSelectedFiles() {
        if (!pendingFiles || pendingFiles.length === 0) {
            $("#shareDropZoneHint").show();
            $("#shareDropZoneFile").hide();
            return;
        }
        $("#shareDropZoneHint").hide();
        let names = pendingFiles.map(function (f) {
            return "✅ " + f.name + " （" + fmtSize(f.size) + "）";
        }).join("<br>");
        $("#shareDropZoneFile").html(names).show();
    }

    function bindDropZone() {
        let $dz = $("#shareDropZone");
        // 点击拖拽区 → 触发原生多选文件选择
        $dz.on("click", function (e) {
            if (e.target.id === "shareFileInput") return;
            $("#shareFileInput").click();
        });
        // 原生选择（追加，不覆盖）
        $("#shareFileInput").on("change", function () {
            if (this.files && this.files.length) {
                for (let i = 0; i < this.files.length; i++) pendingFiles.push(this.files[i]);
            }
            this.value = ""; // 允许重复选择同名文件
            showSelectedFiles();
        });
        // 拖拽事件
        $dz.on("dragover dragenter", function (e) {
            e.preventDefault(); e.stopPropagation();
            $dz.addClass("dragover");
        }).on("dragleave", function (e) {
            e.preventDefault(); e.stopPropagation();
            $dz.removeClass("dragover");
        }).on("drop", function (e) {
            e.preventDefault(); e.stopPropagation();
            $dz.removeClass("dragover");
            let ev = e.originalEvent;
            let files = ev && ev.dataTransfer ? ev.dataTransfer.files : null;
            if (!files || files.length === 0) return;
            for (let i = 0; i < files.length; i++) pendingFiles.push(files[i]);
            showSelectedFiles();
        });
    }

    // 分片大小：50MB，单分片远低于 Cloudflare 免费版 100MB 边缘上限，可稳定走隧道
    const CHUNK_SIZE = 50 * 1024 * 1024;

    function genUploadId() {
        if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
        return Date.now().toString(36) + Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
    }

    function uploadOneFile(file, onProgress) {
        let total = Math.max(1, Math.ceil(file.size / CHUNK_SIZE));
        let uploadId = genUploadId();
        // 断点续传：先查询已接收分片，跳过已传部分
        return $.ajax({
            url: "share/chunkStatus", type: "get", data: {uploadId: uploadId, total: total}
        }).then(function (st) {
            let received = (st && st.body && st.body.received) || [];
            let receivedSet = {};
            received.forEach(function (i) { receivedSet[i] = true; });
            let seq = 0;
            let chain = $.Deferred().resolve().promise();
            for (let index = 0; index < total; index++) {
                (function (idx) {
                    chain = chain.then(function () {
                        if (receivedSet[idx]) {
                            seq++;
                            if (onProgress) onProgress(seq, total, file.name);
                            return;
                        }
                        let start = idx * CHUNK_SIZE;
                        let end = Math.min(start + CHUNK_SIZE, file.size);
                        let blob = file.slice(start, end);
                        let fd = new FormData();
                        fd.append("uploadId", uploadId);
                        fd.append("fileName", file.name);
                        fd.append("index", idx);
                        fd.append("total", total);
                        fd.append("chunk", blob, file.name);
                        return $.ajax({
                            url: "share/uploadChunk", type: "post",
                            processData: false, contentType: false, data: fd
                        }).then(function () {
                            seq++;
                            if (onProgress) onProgress(seq, total, file.name);
                        });
                    });
                })(index);
            }
            return chain.then(function () {
                return $.ajax({
                    url: "share/merge", type: "post", contentType: "application/json",
                    data: JSON.stringify({body: {uploadId: uploadId, fileName: file.name, total: total}})
                });
            });
        });
    }

    // 上传轮询句柄，避免重复上传时叠加定时器
    let uploadTimer = null;

    function doUpload() {
        if (!isAdmin) {
            alterModal("仅管理员可上传文件");
            return;
        }
        if (!pendingFiles || pendingFiles.length === 0) {
            alterModal("请先选择或拖入文件");
            return;
        }
        let files = pendingFiles.slice();
        let names = files.map(function (f) { return f.name; });
        $("#shareUploadBtn").prop("disabled", true);
        // 弹出全屏浮层「加载中」
        loading(true);
        $("#loadingMsg").text("准备上传 " + files.length + " 个文件…");
        let done = 0;
        let failed = false;
        let failMsg = "";
        // 分片上传链路：仅用于推进进度、并捕获「分片上传本身」的失败；
        // 合并是否成功以文件列表轮询为准（反向代理/Cloudflare 可能吞掉 merge 响应，
        // 导致 merge 的 Promise 永远不返回，浮层卡死——故不依赖它来关闭浮层）。
        let p = $.Deferred().resolve().promise();
        files.forEach(function (file) {
            p = p.then(function () {
                if (failed) return;
                return uploadOneFile(file, function (c, t, name) {
                    $("#loadingMsg").text("上传中（" + (done + 1) + "/" + files.length + "）：" + name + " 分片 " + c + "/" + t);
                    $("#shareUploadMsg").text("上传中：" + name + " 分片 " + c + "/" + t + "（已完成文件 " + done + "/" + files.length + "）");
                }).then(function () {
                    done++;
                }).fail(function (jqXHR) {
                    failed = true;
                    if (jqXHR && jqXHR.status === 401) return; // 全局处理跳转
                    failMsg = (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.errorMsg) || "上传失败";
                    $("#shareUploadMsg").text("失败：" + failMsg);
                });
            });
        });
        let chainSettled = false;
        p.always(function () { chainSettled = true; });

        // 以列表轮询确认实际上传结果，避免依赖可能被代理吞掉的 merge 响应
        if (uploadTimer) clearInterval(uploadTimer);
        let waited = 0;
        let finished = false;
        uploadTimer = setInterval(function () {
            if (finished) return;
            waited++;
            if (failed) { // 分片上传本身失败（非合并响应丢失）
                finished = true; clearInterval(uploadTimer);
                $("#shareUploadBtn").prop("disabled", false);
                loading(false);
                alterModal("上传失败：" + (failMsg || "请稍后重试"));
                return;
            }
            if (chainSettled) {
                $("#loadingMsg").text("文件已上传，正在确认结果…（" + waited + "s）");
            }
            $.ajax({
                url: "share/list", type: "get", cache: false, dataType: "json",
                success: function (res) {
                    if (finished) return;
                    let list = (res && res.body) || [];
                    let have = {};
                    for (let i = 0; i < list.length; i++) have[list[i].name] = true;
                    if (names.every(function (n) { return have[n]; })) {
                        finished = true; clearInterval(uploadTimer);
                        $("#shareUploadBtn").prop("disabled", false);
                        loading(false);
                        $("#shareUploadMsg").text("上传成功：" + files.length + " 个文件");
                        alterModal("上传成功：" + files.length + " 个文件");
                        pendingFiles = [];
                        showSelectedFiles();
                        loadList();
                    } else if (chainSettled && waited >= 30) {
                        // 合并已发起但列表 30s 仍未出现目标文件
                        finished = true; clearInterval(uploadTimer);
                        $("#shareUploadBtn").prop("disabled", false);
                        loading(false);
                        alterModal("上传已完成，但列表未及时刷新，请手动刷新查看");
                    } else if (waited >= 600) {
                        // 绝对兜底：10 分钟内任何情况都关闭浮层，绝不卡死
                        finished = true; clearInterval(uploadTimer);
                        $("#shareUploadBtn").prop("disabled", false);
                        loading(false);
                        alterModal("上传状态未知，请刷新页面查看文件列表");
                    }
                },
                error: function () {
                    if (finished) return;
                    if (chainSettled && waited >= 30) {
                        finished = true; clearInterval(uploadTimer);
                        $("#shareUploadBtn").prop("disabled", false);
                        loading(false);
                        alterModal("上传已完成，但获取列表失败，请刷新查看");
                    } else if (waited >= 600) {
                        finished = true; clearInterval(uploadTimer);
                        $("#shareUploadBtn").prop("disabled", false);
                        loading(false);
                        alterModal("上传状态未知，请刷新页面查看文件列表");
                    }
                }
            });
        }, 1000);
    }

    function bindDelete() {
        $("#shareTableBody").on("click", ".btn-del", function () {
            let $btn = $(this);
            let name = $btn.data("name");
            confirmModal("确定删除文件「" + name + "」？", function () {
                // 按钮进入加载态，防止重复点击
                $btn.prop("disabled", true).text("删除中…");
                $.ajax({
                    url: "share/delete", type: "post", contentType: "application/json",
                    data: JSON.stringify({body: {name: name}}),
                    success: function (res) {
                        if (res.errorCode === "000000") {
                            alterModal("已删除：" + name);
                            loadList(); // 重建列表，行随表格刷新
                        } else {
                            alterModal(res.errorMsg || "删除失败");
                            $btn.prop("disabled", false).text("删除");
                        }
                    },
                    error: function (jqXHR) {
                        if (jqXHR.status === 401) return;
                        alterModal("删除异常");
                        $btn.prop("disabled", false).text("删除");
                    }
                });
            });
        });
    }

    function init() {
        if (initialized) {
            loadList(); // 再次进入仅刷新列表
            return;
        }
        initialized = true;
        // 获取当前用户角色，决定是否展示上传/删除
        $.get("user/info", function (res) {
            let info = (res && res.body) || {};
            isAdmin = !!info.isAdmin;
            if (isAdmin) {
                $("#shareUploadCard").show();
                $("#shareAdminBadge").show();
                bindDropZone();
                $("#shareUploadBtn").off("click").on("click", doUpload);
            }
        }, "json");
        bindDelete();
        loadList();
    }

    return { init: init, isAdmin: function () { return isAdmin; } };
})();
