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

    // ===================== 用户管理 - 调用公共分页 =====================
    let userPage = 1;
    const userSize = 10;
    queryWhiteInfo('');
    $("#openAddUser").click(() => $("#userModal").show());
    $("#confirmAddUser").click(function () {
        let n = $("#addUserName").val().trim();
        if (!n) {
            alter("用户名不能为空！");
            return;
        }
        $("#userModal").hide();
        $("#addUserName").val("");
        addWhite(n);
    });
    $("#userSearch").click(function () {
        let n = $("#userSearchInp").val().trim();
        queryWhiteInfo(n);
    });
    $("#userTableBody").on("click", ".btn-del", function () {
        let username = $(this).context.dataset.i;
        deleteWhiteInfo(username);
    });
    $("#userTableBody").on("click", ".btn-update", function () {
        let username = $(this).context.dataset.i;
        updateWhite(username);
    });
    $("#userPag").on("click", ".pag-btn", function () {
        userPage = $(this).data("p");
        renderUser(userPage, userSize);
    });

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

function addWhite(whitename) {
    var message = {};
    message.transData = '{"action":"addWhite","whitename":"' + whitename + '"}';
    message.transData = sbtoa(message.transData);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            var errorCode = data.errorCode;
            if (errorCode == "000000") {
                queryWhiteInfo('');
                alter("提交成功，请联系管理员审批")
            } else {
                alter(data.errorMsg);
            }

        },
        error: function (data) {
            alter(data.responseText);
        }
    })
}

function updateWhite(whitename) {
    var message = {};
    message.transData = '{"action":"updateWhite","whitename":"' + whitename + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            var errorCode = data.errorCode;
            if (errorCode == "000000") {
                alterModal("成功");
                queryWhiteInfo('');
            } else {
                alert(data.errorMsg);
            }

        },
        error: function (data) {
            loading(false);
            alterModal(data.responseText);
        }
    })
}

function queryWhiteInfo(userName) {
    var message = {};
    message.transData = '{"action":"queryWhiteInfo","userName":"' + userName + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            let list = data.body || [];
            localStorage.setItem(USER_KEY, JSON.stringify(list));
            renderUser(1, 10);
        },
        error: function (data) {
            loading(false);
            alterModal(data.responseText);
        }
    })
}

function deleteWhiteInfo(whitename) {
    var message = {};
    message.transData = '{"action":"deleteWhite","whitename":"' + whitename + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            var errorCode = data.errorCode;
            if (errorCode == "000000") {
                queryWhiteInfo('');
                alterModal("成功");
            } else {
                alterModal(data.errorMsg);
            }

        },
        error: function (data) {
            loading(false);
            alterModal(data.responseText);
        }
    })
}

function renderUser(userPage, userSize) {
    let userList = JSON.parse(localStorage.getItem(USER_KEY)) || [];
    commonPagination(userList, userPage, userSize, "#userTableBody", "#userPag", function (start, idx, item) {
        item.status = item.status == "0" ? "正常" : "待审批";
        return '<tr><td>' + (start + idx + 1) + '</td><td>' + item.userName + '</td><td>' + item.ip + '</td><td>' + item.status + '</td><td><button class="btn-update" data-i="' + item.userName + '">通过</button><button style="margin-left: 20px;" class="btn-del" data-i="' + item.userName + '">删除</button></tr>';
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