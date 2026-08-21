const MSG_KEY = "pcAdminMsgList";
const USER_KEY = "pcAdminUserList";
const CERT_KEY = "pcAdminCertList";

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
        $(".menu-list").children().eq(9).addClass("active");
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

    $("#taskFlash").click(function () {
        let val = $("#taskHost").val();
        taskSrc(val)
    });

    $(".taskStyle").attr("src", "http://10.8.23.5:7724/TBPtaskMan/taskList.html");
    $("#taskHost").on('change', function () {
        let val = $(this).val();
        taskSrc(val)
    });

    // ===================== 文档管理 =====================
    const DOC_KEY = "docList";
    let docList = [];
    let docPage = 1;
    const docSize = 10;

    var $drop = $("#dropArea");
    // 阻止默认拖放事件
    $drop.on("dragenter dragover dragleave drop", function (e) {
        e.preventDefault();
        e.stopPropagation();
    });
    // 拖拽进入高亮
    $drop.on("dragenter dragover", function () {
        $drop.addClass("active");
    });
    // 拖拽离开/松开取消高亮
    $drop.on("dragleave drop", function () {
        $drop.removeClass("active");
    });

    //拖放完成上传
    $drop.on("drop", function (e) {
        var file = e.originalEvent.dataTransfer.files[0];
        fileUpload(file);
    });

    //点击拖拽区选择文件
    $drop.on("click", function () {
        $("#fileUpload").click();
    });

    // 查询文件列表
    function queryDocList(type) {
        let fileName = $("#docFileName").val().trim();
        $.get("doc/list", {fileName: fileName, type: type}, function (res) {
            docList = res.body || [];
            localStorage.setItem(DOC_KEY, JSON.stringify(docList));
            docPage = 1;
            renderDocTable();
        });
    }

    // 渲染表格
    function renderDocTable() {
        commonPagination(docList, docPage, docSize, "#docTableBody", "#docPag", function (realIdx, i, item) {
            return `
            <tr>
                <td>${realIdx + i + 1}</td>
                <td>${item.fileName}</td>
                <td>${item.clientIp}</td>
                <td>${item.userName}</td>
                <td>${item.createTime}</td>
                <td>
                    <!--<button class="btn-preview" data-id="${item.fileUUID}">预览</button>-->
                    <button class="btn-download" data-id="${item.fileUUID}">下载</button>
                    <button class="btn-del" data-id="${item.fileUUID}">删除</button>
                </td>
            </tr>`;
        });
    }

    // 上传文件
    $("#fileUpload").change(function (e) {
        let file = e.target.files[0];
        fileUpload(file);
        $(this).val("");
    });

    function fileUpload(file) {
        if (!file) return;
        let formData = new FormData();
        formData.append("file", file);
        $.ajax({
            url: "doc/upload",
            type: "POST",
            data: formData,
            processData: false,
            contentType: false,
            success: function (res) {
                alterModal("上传成功");
                queryDocList("");
            },
            error: function () {
                alterModal("上传接口异常");
            }
        });
    }

    // 删除
    $("#docTableBody").on("click", ".btn-del", function () {
        let id = $(this).data("id");
        if (!confirm("确定删除？")) return;
        $.post("doc/delete", {'fileUUID': id}, function (res) {
            if (res.errorCode == '000000') {
                alterModal("删除成功");
                queryDocList("");
            } else {
                alterModal(JSON.stringify(res))
            }

        });
    });

    // 下载
    $("#docTableBody").on("click", ".btn-download", function () {
        let id = $(this).data("id");
        window.open("doc/download?fileUUID=" + id);
    });

    // 预览
    $("#docTableBody").on("click", ".btn-preview", function () {
        let id = $(this).data("id");
        $(".preview-img,.preview-text").hide();
        let url = baseUrl + "/preview?id=" + id;
        // 简单判断图片
        let fileName = $(this).closest("tr").find("td:eq(1)").text();
        if (/\.(jpg|jpeg|png|gif|bmp)$/i.test(fileName)) {
            $(".preview-img").attr("src", url).show();
        } else {
            $(".preview-text").load(url).show();
        }
        $("#previewModal").show();
    });

    // 进入页面默认加载
    queryDocList("");

    // 搜索
    $("#searchDocBtn").click(function () {
        queryDocList("");
    });

    // 搜索
    $("#searchDocBtnAll").click(function () {
        queryDocList("");
    });

    // 搜索
    $("#searchDocBtnByMe").click(function () {
        queryDocList("1");
    });


    // 分页
    $("#docPag").on("click", ".pag-btn", function () {
        docPage = $(this).data("p");
        renderDocTable();
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

    queryUdInfo();
    // 证书管理交互
    $("#btnRefreshCert").click(() => queryUdInfo());
    $("#btnChangeUDn").click(function () {
        let o = $("#operNo").val().trim(), u = $("#uDnNo").val().trim(), e = $("#envSelect").val();
        if (!o || !u || e == "请选择环境") {
            alterModal("请完整填写操作员、U盾编号并选择环境！");
            return;
        }
        updateUdInfo(o, u, e);
    });
    $("#btnSubmitCertStatus").click(function () {
        let s = $("#certStatusSelect").val(), e = $("#envStatusSelect").val();
        if (s == "请选择状态" || e == "请选择环境") {
            alterModal("请选择校验状态与环境！");
            return;
        }
        updateCertStatus(s, e);
    });
    $("#btnUnbindCert").click(function () {
        let c = $("#certNo").val().trim();
        if (!c) {
            alterModal("请输入需要解绑的证书编号！");
            return;
        }
        unbindCert(c);
    });
    $("#btnCfcaCert").click(() =>
        queryCFCAUdInfo()
    );

    let certPage = 1;
    const certSize = 10;
    queryCertOper(certPage, certSize);
    $("#certPag").on("click", ".pag-btn", function () {
        certPage = $(this).data("p");
        rendercert(certPage, certSize);
    });

    // 接口文档交互
    $("#btnApiQuery").click(function () {
        let no = $("#apiNo").val().trim();
        if (!no) {
            alterModal("请输入编号查询！");
            return;
        }
        alterModal("接口查询成功");
    });
    $("#btnWebDownload").click(() => alert("web全量下载任务已发起"));
    $("#btnDubboDownload").click(() => alert("dubbo全量下载任务已发起"));
    $("#btnApiSubmit").click(function () {
        if ($("#apiProject").val() == "请选择") {
            alterModal("请选择工程！");
            return;
        }
        alterModal("接口查询提交成功");
    });
});

function queryCertOper() {
    var message = {};
    message.transData = '{"action":"queryUdOper"}';
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
            localStorage.setItem(CERT_KEY, JSON.stringify(list));
            rendercert(1, 10);
        },
        error: function (data) {
            loading(false);
            alterModal("系统繁忙，请稍后再试");
        }
    })
}

function unbindCert(zsNumber) {
    var message = {};
    message.transData = '{"action":"unBindUkey","zsNumber":"' + zsNumber + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
            queryCertOper();
        },
        error: function (data) {
            loading(false);
            alterModal(data.responseText);
        }
    })
}

function queryCFCAUdInfo(zsNumber) {
    var zsNumber = $("#certNo").val();
    if (zsNumber == "") {
        alterModal("请输入证书编号!");
        return;
    }
    var message = {};
    message.transData = '{"action":"cfcaInfoQry","zsNumber":"' + zsNumber + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            alterModal(JSON.stringify(data))
        },
        error: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
        }
    })
}

function updateCertStatus(udStatue, udhost) {
    var message = {};
    message.transData = '{"action":"udOper","udStatue":"' + udStatue + '","udhost":"' + udhost + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            alterModal("证书校验状态变更提交成功！");
            queryUdInfo();
            queryCertOper();
        },
        error: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
        }
    })
}

function queryUdInfo() {
    var message = {};
    message.transData = '{"action":"queryUdInfo"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            if (data.errorCode == "000000") {
                var list = data.body;
                if (list != null && list.length > 0) {
                    for (var i = 1; i <= list.length; i++) {
                        var status = list[i - 1].statue;
                        if (status == "1") {
                            $("#udfo" + i).html("关闭");
                        } else {
                            $("#udfo" + i).html("打开");
                        }
                    }
                }

            } else {
                loading(false);
                alterModal(data.errorMsg);
            }

        },
        error: function (data) {
            alterModal(data);
        }
    })
}

function updateUdInfo(userId, usbkey, Ostype) {
    var message = {};
    message.transData = '{"action":"queryCprUser","userId":"' + userId + '","usbkey":"' + usbkey + '","Ostype":"' + Ostype + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
        },
        error: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
        }
    })
}

function rendercert(certPage, certSize) {
    let certList = JSON.parse(localStorage.getItem(CERT_KEY)) || [];
    commonPagination(certList, certPage, certSize, "#certTableBody", "#certPag", function (start, idx, item) {
        return '<tr><td>' + (start + idx + 1) + '</td><td>' + item.name + '</td><td>' + item.ip + '</td><td>' + item.time + '</td><td>' + item.content + '</td></tr>';
    });
}

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

function queryMsgCode() {
    $("#msgCode").html("");
    var mobilePhone = $("#mobilePhone").val();
    var Ostype = $("#Ostype").val();
    if (mobilePhone == "") {
        alterModal("请输入手机号码!");
        return;
    }
    if (Ostype == "") {
        alterModal("请选择环境!");
        return;
    }

    var message = {};
    message.transData = '{"action":"queryMsgCode","mobilePhone":"' + mobilePhone + '","Ostype":"' + Ostype + '"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            var body = data.body;
            if (body != null) {
                $("#msgCode").html(body.msgCode + "&nbsp;&nbsp;&nbsp;&nbsp;创建时间：" + body.createTime);
            }
        },
        error: function (data) {
            loading(false);
            alterModal(JSON.stringify(data));
        }
    })
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

function orderCreate(type) {
    var orderO = $("#orderO").val();
    if (orderO == "") {
        alterModal("请输入清单列表!");
        return;
    }

    var message = {};
    if (type == "1") {
        message.transData = '{"action":"orderCreate","orderO":"' + orderO + '"}';
    }
    if (type == "2") {
        message.transData = '{"action":"datadict","orderO":"' + orderO + '"}';
    }
    if (type == "3") {
        message.transData = '{"action":"requestData","orderO":"' + orderO + '"}';
    }
    if (type == "4") {
        message.transData = '{"action":"responseData","orderO":"' + orderO + '"}';
    }
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            $("#orderN").val("");
            var orderList = data.body;
            var listOrder = "";
            if (orderList != null) {
                for (i = 0; i < orderList.length; i++) {
                    listOrder += orderList[i] + "\n";
                }
            }
            $("#orderN").val(listOrder);
        },
        error: function (data) {
            loading(false);
            alterModal(data.responseText);
        }
    })
}

function nowQuery() {
    $("#rznr").html("");
    $("#startId").attr("disabled", true);
    $("#startId2").attr("disabled", true);
    var searchFiled = $("#searchFiled").val();
    if (searchFiled == "") {
        $("#startId").attr("disabled", false);
        $("#startId2").attr("disabled", false);
        $("#rznr").html("");
        return;
    }
    var host = $("#host").val();
    if (host == "") {
        $("#startId").attr("disabled", false);
        $("#startId2").attr("disabled", false);
        alterModal("请选择环境!");
        return;
    }
    var project = $("#project").val();
    if (project == "") {
        $("#startId").attr("disabled", false);
        $("#startId2").attr("disabled", false);
        alterModal("请选择功能!");
        return;
    }

    var message = {};
    message.transData = '{"action":"start","searchFiled":"' + searchFiled + '","host":"' + host + '","project":"' + project + '","type":""}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            $("#startId").attr("disabled", false);
            $("#startId2").attr("disabled", false);
            if (!data || !data.body || data.body == "") {
                $("#rznr").html("");
                alterModal("查无记录");
                return;
            }
            var aa = data.body;
            var bb = [];
            bb = aa.split(searchFiled);
            var cc = "";
            for (var i = 0; i < bb.length; i++) {
                if (i == bb.length - 1) {
                    cc = cc + bb[i];
                } else {
                    cc = cc + bb[i] + "<span style='font-weight: bold;color: red'>" + searchFiled + "</span>";
                }
            }
            $("#rznr").html(cc);
        },
        error: function (data) {
            loading(false);
            $("#startId").attr("disabled", false);
            $("#startId2").attr("disabled", false);
            alterModal(data);
        }
    })
}

function historyQuery() {
    $("#rznr").html("");
    $("#startId").attr("disabled", true);
    $("#startId2").attr("disabled", true);
    var searchFiled = $("#searchFiled").val();
    if (searchFiled == "") {
        $("#startId").attr("disabled", false);
        $("#startId2").attr("disabled", false);
        $("#rznr").html("");
        return;
    }
    var host = $("#host").val();
    if (host == "") {
        $("#startId").attr("disabled", false);
        $("#startId2").attr("disabled", false);
        alterModal("请选择环境!");
        return;
    }
    var project = $("#project").val();
    if (project == "") {
        $("#startId").attr("disabled", false);
        $("#startId2").attr("disabled", false);
        alterModal("请选择功能!");
        return;
    }
    var message = {};
    message.transData = '{"action":"start","searchFiled":"' + searchFiled + '","host":"' + host + '","project":"' + project + '","type":"1"}';
    message.transData = sbtoa(message.transData);
    loading(true);
    $.ajax({
        url: "tbphx.do",
        dataType: "json",
        type: "post",
        data: message,
        success: function (data) {
            loading(false);
            $("#startId").attr("disabled", false);
            $("#startId2").attr("disabled", false);
            if (!data || !data.result || data.result == "") {
                $("#rznr").html("");
                alert("查无记录");
                return;
            }
            var aa = data.result;
            var bb = [];
            bb = aa.split(searchFiled);
            var cc = "";
            for (var i = 0; i < bb.length; i++) {
                if (i == bb.length - 1) {
                    cc = cc + bb[i];
                } else {
                    cc = cc + bb[i] + "<span style='font-weight: bold;color: red'>" + searchFiled + "</span>";
                }
            }
            $("#rznr").html(cc);
        },
        error: function (data) {
            loading(false);
            $("#startId").attr("disabled", false);
            $("#startId2").attr("disabled", false);
            alterModal(data);
        }
    })
}

function webDown(type) {
    $("#docQryAllType").val(type);
    $("#docQryAll").submit();
}

function docQry() {
    var docname = $("#docname1").val();
    if (docname == "") {
        alterModal("请输入接口编号!");
        return;
    }
    $("#docname").val(docname);
    $("#docQry").submit();
}

function bookmarks() {
    window.open("bookmarks.html");
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

function taskSrc(val) {
    if (val == 'SIT1') {
        $(".taskStyle").attr("src", "http://10.8.23.5:7724/TBPtaskMan/taskList.html");
    }
    if (val == 'SIT2') {
        $(".taskStyle").attr("src", "http://10.8.64.48:7724/TBPtaskMan/taskList.html");
    }
    if (val == 'UAT1') {
        $(".taskStyle").attr("src", "http://10.8.8.113:7724/TBPtaskMan/taskList.html");
    }
    if (val == 'UAT2') {
        $(".taskStyle").attr("src", "http://10.8.32.41:7724/TBPtaskMan/taskList.html");
    }
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