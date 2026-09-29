const MSG_KEY = "pcAdminMsgList";
const USER_KEY = "pcAdminUserList";

$(function () {
    // 全局 AJAX 401 处理：会话过期/未登录 → 弹窗提示并跳转登录页
    $(document).ajaxError(function (event, jqXHR, settings, errorThrown) {
        if (jqXHR.status == 401) {
            let msg = "未登录或会话过期";
            try {
                let r = jqXHR.responseJSON || JSON.parse(jqXHR.responseText || "{}");
                if (r && r.errorMsg) msg = r.errorMsg;
            } catch (e) {}
            alterModal(msg + "，即将跳转登录页…");
            setTimeout(function () { location.href = "login.html"; }, 1500);
        }
    });
    // 动态时钟
    function setTime() {
        let d = new Date();
        let fmt = d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, '0') + "-" + String(d.getDate()).padStart(2, '0') + " " + String(d.getHours()).padStart(2, '0') + ":" + String(d.getMinutes()).padStart(2, '0') + ":" + String(d.getSeconds()).padStart(2, '0');
        $("#nowTime").text(fmt);
    }

    setTime();
    setInterval(setTime, 1000);

    // ===================== 主题切换（持久化到当前用户）=====================
    var THEMES = ["dark", "light", "eyecare", "techblue", "orange", "pink", "system"];
    function systemTheme() {
        return (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? "dark" : "light";
    }
    function applyTheme(theme) {
        if (THEMES.indexOf(theme) < 0) theme = "dark";
        // “跟随系统”：按操作系统的浅色/深色动态套用
        if (theme === "system") {
            document.documentElement.setAttribute("data-theme", systemTheme());
            $("#themeSelect").val("system");
        } else {
            document.documentElement.setAttribute("data-theme", theme);
            $("#themeSelect").val(theme);
        }
        try { localStorage.setItem("wxyd-theme", theme); } catch (e) {}
    }
    // 选中“跟随系统”时，随系统配色切换实时更新
    var mq = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
    function onSystemChange() {
        if ($("#themeSelect").val() === "system") applyTheme("system");
    }
    if (mq) {
        if (mq.addEventListener) mq.addEventListener('change', onSystemChange);
        else if (mq.addListener) mq.addListener(onSystemChange);
    }

    // 进入页面：以服务端保存的主题为准（localStorage 仅用于首屏防闪烁）
    $.get("user/theme", function (res) {
        let t = (res && res.body) || "dark";
        applyTheme(t);
    }, "json");

    $("#themeSelect").change(function () {
        let theme = $(this).val();
        applyTheme(theme);
        // 立即本地生效，再异步落库
        try { localStorage.setItem("wxyd-theme", theme); } catch (e) {}
        $.ajax({
            url: "user/updateTheme", type: "post", contentType: "application/json",
            data: JSON.stringify({body: {theme: theme}}),
            success: function (res) {
                if (res.errorCode !== "000000") alterModal(res.errorMsg || "主题保存失败");
            },
            error: function () { alterModal("主题保存失败，请稍后重试"); }
        });
    });

    // 菜单切换
    $(".menu-item").click(function () {
        $(".menu-item").removeClass("active");
        $(this).addClass("active");
        $(".module-box").removeClass("active");
        $("#" + $(this).data("target")).addClass("active");
        // Java8 API 模块懒加载
        if ($(this).data("target") === "javaApi" && window.JavaApi) {
            JavaApi.init();
        }
        // Shell 脚本学习模块懒加载
        if ($(this).data("target") === "shellScript" && window.ShellScript) {
            ShellScript.init();
        }
        // 记忆笔记模块懒加载
        if ($(this).data("target") === "note" && window.Note) {
            Note.init();
        }
        // 报文归档下载模块懒加载
        if ($(this).data("target") === "archiveTool" && window.Archive) {
            Archive.init();
        }
        // 文件共享模块懒加载
        if ($(this).data("target") === "shareFile" && window.ShareFile) {
            ShareFile.init();
        }
        // 群聊模块懒加载
        if ($(this).data("target") === "groupChat" && window.GroupChat) {
            GroupChat.mount($("#groupChatBody")[0]);
        }
        // 菜单管理模块懒加载（仅管理员）
        if ($(this).data("target") === "menuManage" && window.MenuManage) {
            MenuManage.mount($("#menuManageBody")[0]);
        }
    });

    $("#welcome").click(function () {
        $(".menu-item").removeClass("active");
        $(".module-box").removeClass("active");
        $("#messageBoard").addClass("active");
        $(".menu-list").children().eq(3).addClass("active");
    });

    // 关闭弹窗
    $(".close-modal").click(function () {
        $(".modal").hide();
    });

    $("#close-alterModal").click(function () {
        $("#alterSpan").html('');
        $("#alterModal").hide();
    });

    $("#confirm-modal").click(function () {
        $("#alterSpan").html('');
        $("#alterModal").hide();
    });

    // ===================== 钉钉机器人 =====================
    $("#btnRefreshRobot").click(loadRobots);
    $("#btnAddRobot").click(function () {
        $("#robotForm").show();
        $("#robotId").val("");
    });
    $("#btnSaveRobot").click(function () {
        let body = {name: $("#robotName").val(), accessToken: $("#robotToken").val(), secret: $("#robotSecret").val()};
        let id = $("#robotId").val();
        let url = id ? "robot/update" : "robot/add";
        if (id) body.id = parseInt(id);
        loading(true);
        $.ajax({
            url, type: "post", contentType: "application/json", data: JSON.stringify({body}),
            success: function (res) {
                loading(false);
                alterModal(res.errorCode == "000000" ? "保存成功" : res.errorMsg);
                loadRobots();
                $("#robotForm").hide();
            },
            error: function () {
                loading(false);
            }
        });
    });
    $(document).on("click", "#robotTableBody .btn-del", function () {
        let id = $(this).data("id");
        confirmModal("确定删除该机器人？", function () {
            loading(true);
            $.ajax({
                url: "robot/delete", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: parseInt(id)}}),
                success: function (res) {
                    loading(false);
                    alterModal(res.errorCode == "000000" ? "删除成功" : res.errorMsg);
                    loadRobots();
                },
                error: function () {
                    loading(false);
                }
            });
        });
    });
    $("#sendType").change(function () {
        $("#textInput").toggle($(this).val() == "text");
        $("#fileInput").toggle($(this).val() == "file");
    });
    // 文件拖拽/选择展示
    function showSelectedFile(file) {
        if (!file) return;
        let size = file.size > 1048576 ? (file.size / 1048576).toFixed(2) + " MB" : (file.size / 1024).toFixed(2) + " KB";
        $("#dropZoneHint").hide();
        let $f = $("#dropZoneFile").text("✅ " + file.name + " （" + size + "）").show();
        $f.data("name", file.name);
    }
    // 点击拖拽区 → 触发原生文件选择
    $("#dropZone").on("click", function (e) {
        if (e.target.id === "sendFile") return;
        $("#sendFile").click();
    });
    // 原生选择回调
    $("#sendFile").on("change", function () {
        if (this.files && this.files[0]) showSelectedFile(this.files[0]);
    });
    // 拖拽事件
    let $dz = $("#dropZone");
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
        let file = files[0];
        // 通过 DataTransfer 把拖入的文件写回隐藏 input，保证发送逻辑不变
        let dt = new DataTransfer();
        dt.items.add(file);
        $("#sendFile")[0].files = dt.files;
        if (files.length > 1) $("#dropZoneFile").show().text("⚠️ 仅发送第一个文件：" + file.name);
        showSelectedFile(file);
    });
    $("#btnSend").click(function () {
        let fd = new FormData();
        fd.append("robotId", $("#sendRobotSel").val());
        fd.append("type", $("#sendType").val());
        if ($("#sendType").val() == "text") fd.append("text", $("#sendText").val());
        else fd.append("file", $("#sendFile")[0].files[0]);
        loading(true);
        $.ajax({
            url: "robot/send", type: "post", processData: false, contentType: false, data: fd,
            success: function (res) {
                loading(false);
                if (res.errorCode == "000000") {
                    let b = res.body || {};
                    $("#sendResult").text(b.type == "file" ? `文件已发：共${b.totalChunks}块` : "文本已发");
                } else {
                    $("#sendResult").text(res.errorMsg);
                }
            },
            error: function (jqXHR) {
                if (jqXHR.status == 401) return; // 由全局 ajaxError 处理并跳转
                loading(false);
                $("#sendResult").text("发送异常");
            }
        });
    });
    loadRobots();

    // ===================== 设置昵称 =====================
    $("#btnSaveNick").click(function () {
        let nick = $("#nickInput").val().trim();
        if (!nick) { alterModal("昵称不能为空"); return; }
        loading(true);
        $.ajax({
            url: "user/updateDisplayName", type: "post", contentType: "application/json",
            data: JSON.stringify({body: {displayName: nick}}),
            success: function (res) {
                loading(false);
                if (res.errorCode == "000000") {
                    alterModal("昵称设置成功");
                    $("#nickModal").hide();
                    loadMsg('');
                } else {
                    alterModal(res.errorMsg);
                }
            },
            error: function () {
                loading(false);
            }
        });
    });

    // ===================== 用户管理 =====================
    $("#btnRefreshUser").click(loadUsers);
    loadUsers();

    // ===================== 留言板 - 调用公共分页 =====================
    let msgPage = 1;
    const msgSize = 10;
    loadMsg('');
    $("#openAddMsg").click(function() {
        console.log(111)
        $("#msgModal").show()
    });
    $("#lookMsgByAll").click(() => loadMsg(''));
    $("#lookMsgByMe").click(() => loadMsg('1'));
    $("#confirmAddMsg").click(function () {
        let c = $("#addMsgContent").val().trim();
        if (!c) {
            alter("留言内容不能为空！");
            return;
        }
        addMsg(c);
        $("#msgModal").hide();
        $("#addMsgContent").val("");
    });
    $("#msgTableBody").on("click", ".btn-del", function () {
        let msgId = $(this).data("i");
        delMsg(msgId);
    });
    $("#msgPag").on("click", ".pag-btn", function () {
        msgPage = Number($(this).data("p"));
        renderMsg(msgPage, msgSize);
    });

    // ===================== SQL 查询（仅管理员可见）=====================
    $.get("user/info", function (res) {
        let info = (res && res.body) || {};
        if (info.isAdmin) {
            $("#menuSqlTool").show();
            $("#menuMenuManage").show();
            if (window.initSqlTool) window.initSqlTool();
        }
        applyMenuVisibility(info.isAdmin);
    }, "json");

    // ===================== 菜单可见性过滤（菜单管理配置）=====================
    // 服务端返回当前用户可见的菜单 key；管理员返回全部。首页/我的/菜单管理入口不参与过滤。
    function applyMenuVisibility(isAdmin) {
        $.get("menu/visible", function (res) {
            let keys = (res && res.body) || [];
            $(".menu-item").each(function () {
                let t = $(this).data("target");
                if (t === "welcome" || t === "menuManage") return;
                let visible = keys.indexOf(t) !== -1;
                // SQL 查询入口额外要求管理员（保持原有 admin-only 语义）
                if (t === "sqlTool") $(this).toggle(visible && isAdmin);
                else $(this).toggle(visible);
            });
        }, "json").fail(function () { /* 拉取失败保持现状（默认全可见） */ });
    }
});

// 退出登录：销毁会话并跳转登录页
function doLogout() {
    $.ajax({
        url: "logout", type: "post",
        complete: function () { location.href = "login.html"; }
    });
}

// ===================== 钉钉机器人 =====================
function loadRobots() {
    loading(true);
    $.get("robot/list", function (res) {
        loading(false);
        let list = res.body || [];
        let html = "";
        for (let i = 0; i < list.length; i++) {
            let r = list[i];
            html += `<tr><td>${i + 1}</td><td>${r.name}</td><td>${(r.accessToken || '').slice(-6)}</td>
                <td><button class="btn-del" data-id="${r.id}">删除</button></td></tr>`;
        }
        $("#robotTableBody").html(html);
        let sel = $("#sendRobotSel").html("");
        for (let r of list) {
            sel.append(`<option value="${r.id}">${r.name}</option>`);
        }
    }, "json").fail(function () { loading(false); });
}

// ===================== 用户管理 =====================
function loadUsers() {
    loading(true);
    $.get("user/list", function (res) {
        loading(false);
        let list = res.body || [];
        let html = "";
        for (let i = 0; i < list.length; i++) {
            let u = list[i];
            let role = u.role == 0 ? "超管" : "普通";
            let st = u.status == 0 ? "已通过" : u.status == 1 ? "待审批" : u.status == 2 ? "已拒绝" : u.status == 3 ? "已暂停" : "未知";
            let op = "";
            if (u.role == 0) {
                op = '<span style="color:#999;">—</span>';
            } else if (u.status == 0) {
                op = `<button class="btn-main" onclick="pauseUser(${u.id})">暂停</button> <button class="btn-del" onclick="deleteUser(${u.id})">删除</button>`;
            } else if (u.status == 1) {
                op = `<button class="btn-main" onclick="approveUser(${u.id})">通过</button> <button class="btn-del" onclick="rejectUser(${u.id})">拒绝</button>`;
            } else if (u.status == 2) {
                op = `<button class="btn-del" onclick="deleteUser(${u.id})">删除</button>`;
            } else if (u.status == 3) {
                op = `<button class="btn-main" onclick="resumeUser(${u.id})">恢复</button> <button class="btn-del" onclick="deleteUser(${u.id})">删除</button>`;
            }
            html += `<tr><td>${i + 1}</td><td>${u.username}</td><td>${u.displayName || ''}</td><td>${role}</td><td>${st}</td><td>${u.createTime || ''}</td><td>${op}</td></tr>`;
        }
        $("#userTableBody").html(html);
    }, "json").fail(function () { loading(false); });
}

function approveUser(id) {
    loading(true);
    $.ajax({
        url: "approveUser", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: id}}),
        success: function () {
            loading(false);
            loadUsers();
        },
        error: function () {
            loading(false);
        }
    });
}

function rejectUser(id) {
    loading(true);
    $.ajax({
        url: "rejectUser", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: id}}),
        success: function () {
            loading(false);
            loadUsers();
        },
        error: function () {
            loading(false);
        }
    });
}

function pauseUser(id) {
    confirmModal("确定暂停该用户？暂停后该用户将无法登录。", function () {
        loading(true);
        $.ajax({
            url: "pauseUser", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: id}}),
            success: function (res) {
                loading(false);
                if (res.errorCode == "000000") loadUsers();
                else alterModal(res.errorMsg || "操作失败");
            },
            error: function () { loading(false); }
        });
    });
}

function resumeUser(id) {
    loading(true);
    $.ajax({
        url: "resumeUser", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: id}}),
        success: function (res) {
            loading(false);
            if (res.errorCode == "000000") loadUsers();
            else alterModal(res.errorMsg || "操作失败");
        },
        error: function () { loading(false); }
    });
}

function deleteUser(id) {
    confirmModal("确定删除该用户？其关联的钉钉机器人也会一并删除，且不可恢复。", function () {
        loading(true);
        $.ajax({
            url: "deleteUser", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: id}}),
            success: function (res) {
                loading(false);
                if (res.errorCode == "000000") loadUsers();
                else alterModal(res.errorMsg || "操作失败");
            },
            error: function () { loading(false); }
        });
    });
}

function loadMsg(flag) {
    loading(true);
    $.ajax({
        url: "queryMessage", type: "post", contentType: "application/json",
        data: JSON.stringify({body: {flag: flag}}),
        success: function (data) {
            loading(false);
            let msgList = data.body.msgList || [];
            let displayName = data.body.displayName;
            let username = data.body.username;
            // 昵称优先显示 display_name，没有则显示"设置昵称"链接
            if (displayName) {
                $("#userName").html(displayName).css("color", "var(--text-1)").off("click");
            } else {
                $("#userName").html('<span style="color:var(--accent);">设置昵称</span>').off("click").on("click", function () {
                    $("#nickInput").val(username || "");
                    $("#nickModal").show();
                });
            }
            localStorage.setItem(MSG_KEY, JSON.stringify(msgList));
            renderMsg(1, 10);
        },
        error: function (data) {
            loading(false);
        }
    })
}

function renderMsg(msgPage, msgSize) {
    let msgList = JSON.parse(localStorage.getItem(MSG_KEY)) || [];
    commonPagination(msgList, msgPage, msgSize, "#msgTableBody", "#msgPag", function (start, idx, item) {
        return '<tr><td>' + (start + idx + 1) + '</td><td>' + item.name + '</td><td>' + item.content + '</td><td>' + item.time + '</td><td><button class="btn-del" data-i="' + item.id + '">删除</button></tr>';
    });
}

function addMsg(msg) {
    loading(true);
    $.ajax({
        url: "addMessage", type: "post", contentType: "application/json",
        data: JSON.stringify({body: {message: msg}}),
        success: function (data) {
            loadMsg('');
        },
        error: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
        }
    })
}

function delMsg(msgId) {
    loading(true);
    $.ajax({
        url: "delMessage", type: "post", contentType: "application/json",
        data: JSON.stringify({body: {msgId: msgId}}),
        success: function (data) {
            loading(false);
            if (data.errorCode == '000000') {
                loadMsg('');
            } else {
                alterModal(JSON.stringify(data));
            }
        },
        error: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
        }
    })
}

// ===================== 【公共通用分页方法抽离】 =====================
// list:数据源 page:当前页 size:每页条数 tableDom:表格容器 pagDom:分页容器 renderCell:自定义渲染行回调
function commonPagination(list, page, size, tableDom, pagDom, renderCell) {
    let start = (page - 1) * size;
    let pageData = list.slice(start, start + size);
    let html = "";
    if (pageData.length === 0) {
        html = '<tr><td colspan="99" style="text-align:center;color:#999;">暂无数据</td></tr>';
    } else {
        $.each(pageData, function (idx, item) {
            html += renderCell(start, idx, item);
        });
    }
    $(tableDom).html(html);
    // 生成分页按钮
    let totalPage = Math.ceil(list.length / size);
    let pagHtml = "";
    for (let i = 1; i <= totalPage; i++) {
        pagHtml += '<button class="pag-btn ' + (i === page ? 'active' : '') + '" data-p="' + i + '">' + i + '</button>';
    }
    $(pagDom).html(pagHtml);
}

function loading(flag) {
    if (flag == true) {
        $("#loading").show()
    } else {
        $("#loading").hide()
    }
}

function alterModal(msg) {
    loading(false);
    $("#alterSpan").html(msg);
    $("#alterModal").show();
}

// 公共确认弹框（居中，复用 alterModal 的视觉样式）：点击“确认”时执行 onOk 回调
let _confirmOk = null;
function confirmModal(msg, onOk) {
    loading(false);
    $("#confirmModalMsg").html(msg);
    _confirmOk = onOk;
    $("#confirmModal").show();
}

$(function () {
    $("#confirmModalOk").click(function () {
        $("#confirmModal").hide();
        if (typeof _confirmOk === "function") {
            let cb = _confirmOk;
            _confirmOk = null;
            cb();
        }
    });
    $("#confirmModalCancel, #close-confirmModal").click(function () {
        $("#confirmModal").hide();
        _confirmOk = null;
    });
});
