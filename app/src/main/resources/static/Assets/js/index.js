const MSG_KEY = "pcAdminMsgList";
const USER_KEY = "pcAdminUserList";

$(function () {
    // 动态时钟
    function setTime() {
        let d = new Date();
        let fmt = d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, '0') + "-" + String(d.getDate()).padStart(2, '0') + " " + String(d.getHours()).padStart(2, '0') + ":" + String(d.getMinutes()).padStart(2, '0') + ":" + String(d.getSeconds()).padStart(2, '0');
        $("#nowTime").text(fmt);
    }

    setTime();
    setInterval(setTime, 1000);

    // 菜单切换
    $(".menu-item").click(function () {
        $(".menu-item").removeClass("active");
        $(this).addClass("active");
        $(".module-box").removeClass("active");
        $("#" + $(this).data("target")).addClass("active");
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
        $.ajax({
            url, type: "post", contentType: "application/json", data: JSON.stringify({body}),
            success: function (res) {
                alterModal(res.errorCode == "000000" ? "保存成功" : res.errorMsg);
                loadRobots();
                $("#robotForm").hide();
            }
        });
    });
    $(document).on("click", "#robotTableBody .btn-del", function () {
        let id = $(this).data("id");
        if (!confirm("确定删除？")) return;
        $.ajax({
            url: "robot/delete", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: parseInt(id)}}),
            success: function (res) {
                alterModal(res.errorCode == "000000" ? "删除成功" : res.errorMsg);
                loadRobots();
            }
        });
    });
    $("#sendType").change(function () {
        $("#textInput").toggle($(this).val() == "text");
        $("#fileInput").toggle($(this).val() == "file");
    });
    $("#btnSend").click(function () {
        let fd = new FormData();
        fd.append("robotId", $("#sendRobotSel").val());
        fd.append("type", $("#sendType").val());
        if ($("#sendType").val() == "text") fd.append("text", $("#sendText").val());
        else fd.append("file", $("#sendFile")[0].files[0]);
        $.ajax({
            url: "robot/send", type: "post", processData: false, contentType: false, data: fd,
            success: function (res) {
                if (res.errorCode == "000000") {
                    let b = res.body || {};
                    $("#sendResult").text(b.type == "file" ? `文件已发：共${b.totalChunks}块` : "文本已发");
                } else {
                    $("#sendResult").text(res.errorMsg);
                }
            },
            error: function () {
                $("#sendResult").text("发送异常");
            }
        });
    });
    loadRobots();

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
        let msgId = $(this).context.dataset.i;
        delMsg(msgId);
    });
    $("#msgPag").on("click", ".pag-btn", function () {
        msgPage = $(this).data("p");
        renderMsg(msgPage, msgSize);
    });
});

// ===================== 钉钉机器人 =====================
function loadRobots() {
    $.get("robot/list", function (res) {
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
    }, "json");
}

// ===================== 用户管理 =====================
function loadUsers() {
    $.get("user/list", function (res) {
        let list = res.body || [];
        let html = "";
        for (let i = 0; i < list.length; i++) {
            let u = list[i];
            let role = u.role == 0 ? "超管" : "普通";
            let st = u.status == 0 ? "已通过" : u.status == 1 ? "待审批" : "已拒绝";
            let op = u.status == 1 ? `<button class="btn-main" onclick="approveUser(${u.id})">通过</button> <button class="btn-del" onclick="rejectUser(${u.id})">拒绝</button>` : "";
            html += `<tr><td>${i + 1}</td><td>${u.username}</td><td>${u.displayName || ''}</td><td>${role}</td><td>${st}</td><td>${u.createTime || ''}</td><td>${op}</td></tr>`;
        }
        $("#userTableBody").html(html);
    }, "json");
}

function approveUser(id) {
    $.ajax({
        url: "approveUser", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: id}}),
        success: function () {
            loadUsers();
        }
    });
}

function rejectUser(id) {
    $.ajax({
        url: "rejectUser", type: "post", contentType: "application/json", data: JSON.stringify({body: {id: id}}),
        success: function () {
            loadUsers();
        }
    });
}

function loginWy() {
    $("#loginWy").attr("disabled", true);
    var userName = $("#userName1").val();
    var Ostype = $("#Ostype1").val();
    if (Ostype == "") {
        $("#loginWy").attr("disabled", false);
        alterModal("请选择环境!");
        return;
    }
    if (Ostype == "") {
        $("#loginWy").attr("disabled", false);
        alterModal("请输入用户名!");
        return;
    }

    var message = {};
    message.transData = '{"action":"mmLogin","userName":"' + userName + '","Ostype":"' + Ostype + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            $("#loginWy").attr("disabled", false);
            var resultHtml = data.body.resultHtml;
            if (resultHtml && resultHtml != "") {
                window.open(resultHtml);
            } else {
                alterModal("系统繁忙，请稍后再试")
            }
        },
        error: function (data) {
            loading(false);
            $("#loginWy").attr("disabled", false);
            alterModal(JSON.stringify(data));
        }
    })
}

function NGLoginChange() {
    var type = $("#Ostype2").val();
    if (type == "5") {
        $("#nglocalType").show();
    } else {
        $("#nglocalType").hide();
    }
}

function NGLogin() {
    $("#ngLogin").attr("disabled", true);
    var userName = $("#userName2").val();
    var Ostype = $("#Ostype2").val();
    var lstype = $("#lstype").val();
    if (Ostype == "") {
        $("#ngLogin").attr("disabled", false);
        alterModal("请选择环境!");
        return;
    }
    if (userName == "") {
        $("#ngLogin").attr("disabled", false);
        alterModal("请输入员工编号!");
        return;
    }
    var message = {};
    message.transData = '{"action":"ngLogin","userName":"' + userName + '","Ostype":"' + Ostype + '","lstype":"' + lstype + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            $("#ngLogin").attr("disabled", false);
            var body = data.body;
            var errorCode = data.errorCode;
            if ("000000" == errorCode) {
                var resultHtml = body.returnUrl;
                if (resultHtml && resultHtml != "") {
                    window.open(resultHtml);
                } else {
                    alterModal(JSON.stringify(data))
                }
            } else {
                alterModal(JSON.stringify(data))
            }

        },
        error: function (data) {
            loading(false);
            $("#ngLogin").attr("disabled", false);
            alterModal(JSON.stringify(data));
        }
    })
}

function loadMsg(flag) {
    var message = {};
    message.transData = '{"action":"queryMessage","flag":"' + flag + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            let msgList = data.body.msgList || [];
            let userName = data.body.userName;
            $("#userName").html(userName);
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
    var message = {};
    message.transData = '{"action":"addMessage","message":"' + msg + '"}';
    message.transData = sbtoa(message.transData);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loadMsg('');
        },
        error: function (data) {
            alterModal(JSON.stringify(data));
        }
    })
}

function delMsg(msgId) {
    var message = {};
    message.transData = '{"action":"delMessage","msgId":"' + msgId + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            if (data.errorCode == '000000') {
                loadMsg('');
            } else {
                alterModal(JSON.stringify(data))
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

function sbtoa(transData) {
    var message = {};
    transData = btoa(encodeURIComponent(transData));
    message.transData = transData;
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        async: false,
        data: message,
        success: function (data) {
            transData = data.body.transData;
        },
        error: function (data) {
        }
    });
    return transData;
}