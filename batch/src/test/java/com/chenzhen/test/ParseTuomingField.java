package com.chenzhen.test;

import com.chenzhen.util.CommUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParseTuomingField {
    private static List<TuominField> list = new ArrayList<>();
    private static List<String> clist = new ArrayList<>();
    private static String[] serviceNames = {
            "transBank",
            "soa_corporService",
            "soa_TBPAuthenticationService",
            "TBPinnerService",
            "TBPinnerManagement",
            "soa_copService"
    };

    public static void main(String[] args) throws Exception {
        List<String> mlist = TuoMinComm.readFile(serviceNames[5]);

        //tuominAccount(mlist);
        //tuominName(mlist);
        //tuominCert(mlist);
        //tuominMobile(mlist);
        //tuominAddress(mlist);
        tuominEmail(mlist);

        TuoMinComm.print(list);
    }

    public static void tuominAccount(List<String> mlist) {
        mlist.forEach((msg -> {
            if (msg.contains("dataField") && (msg.contains("账号") || msg.contains("卡号") || msg.contains("账户"))
                    && !msg.contains("名") && !msg.contains("地址")
                    && !msg.contains("行号") && !msg.contains("机构")
                    && !msg.contains("类") && !msg.contains("币种")
                    && !msg.contains("开户行") && !msg.contains("权限")
                    && !msg.contains("级") && !msg.contains("状态")
                    && !msg.contains("情况") && !msg.contains("标志")
                    && !msg.contains("方式") && !msg.contains("标记")
                    && !msg.contains("率") && !msg.contains("用途")
                    && !msg.contains("组") && !msg.contains("期")
                    && !msg.contains("符") && !msg.contains("手机")
                    && !msg.contains("属性") && !msg.contains("性质")
                    && !msg.contains("额") && !msg.contains("是否")
                    && !msg.contains("规则") && !msg.contains("数")
                    && !msg.contains("标识") && !msg.contains("编号")) {
                Map<String, String> map = CommUtils.extractAllAttributes(msg);
                if (!clist.contains(map.get("name"))) {
                    TuominField tuominField = new TuominField(map.get("name"), map.get("desc"));
                    list.add(tuominField);
                }
                clist.add(map.get("name"));
            }
        }));
    }

    public static void tuominName(List<String> mlist) {
        mlist.forEach((msg -> {
            if (msg.contains("dataField") && (msg.contains("名字") || msg.contains("号名") ||
                    msg.contains("户名") || msg.contains("姓名") || msg.contains("人名") || msg.contains("文名")
                    || msg.contains("人名") || msg.contains("付息名称") || msg.contains("方名") || msg.contains("团名")
                    || msg.contains("商名") || msg.contains("钱包名") || msg.contains("曾用名") || msg.contains("企业名")
                    || msg.contains("车队名称") || msg.contains("位名") || msg.contains("经办名")
                    || msg.contains("工商登记处名称") || msg.contains("司名") || msg.contains("台名") || msg.contains("提交人")
                    || msg.contains("供应链名称") || msg.contains("局名") || msg.contains("商名") || msg.contains("别名")
                    || msg.contains("担保人") || msg.contains("申请人") || msg.contains("付款人") || msg.contains("受益人")
                    || msg.contains("联系人") || msg.contains("当事人") || msg.contains("汇款人") || msg.contains("收款人")
                    || msg.contains("对手名称") || msg.contains("负责人") || msg.contains("代表人") || msg.contains("登记人")
                    || msg.contains("质权人") || msg.contains("出票人") || msg.contains("简称") || msg.contains("昵称"))
                    && !msg.contains("地址") && !msg.contains("号码")
                    && !msg.contains("手机号") && !msg.contains("系统简称")
                    && !msg.contains("文件") && !msg.contains("邮箱")
                    && !msg.contains("账号")
                    && !msg.contains("机构") && !msg.contains("类")
                    && !msg.contains("部门") && !msg.contains("期")
                    && !msg.contains("序号") && !msg.contains("网点")
                    && !msg.contains("国别") && !msg.contains("客户号")
                    && !msg.contains("币种") && !msg.contains("职务")
                    && !msg.contains("委托") && !msg.contains("手续费")
                    && !msg.contains("电话") && !msg.contains("传真")
                    && !msg.contains("开户行") && !msg.contains("余额")
                    && !msg.contains("证件") && !msg.contains("等级")
                    && !msg.contains("状态") && !msg.contains("标识")) {
                Map<String, String> map = CommUtils.extractAllAttributes(msg);
                if (!clist.contains(map.get("name"))) {
                    TuominField tuominField = new TuominField(map.get("name"), map.get("desc"));
                    list.add(tuominField);
                }
                clist.add(map.get("name"));
            }
        }));

    }

    public static void tuominCert(List<String> mlist) {
        mlist.forEach((msg -> {
            if (msg.contains("dataField") && (msg.contains("证件"))
                    && !msg.contains("类型") && !msg.contains("日") && !msg.contains("电话")
                    && !msg.contains("期") && !msg.contains("地址") && !msg.contains("名")
                    && !msg.contains("标志") && !msg.contains("机关") && !msg.contains("是否")
                    && !msg.contains("种类")) {
                Map<String, String> map = CommUtils.extractAllAttributes(msg);
                if (!clist.contains(map.get("name"))) {
                    TuominField tuominField = new TuominField(map.get("name"), map.get("desc"));
                    list.add(tuominField);
                }
                clist.add(map.get("name"));
            }
        }));
    }


    public static void tuominMobile(List<String> mlist) {
        mlist.forEach((msg -> {
            if (msg.contains("dataField") && (msg.contains("电话") || msg.contains("手机"))
                    && !msg.contains("类型") && !msg.contains("日")
                    && !msg.contains("期") && !msg.contains("地址") && !msg.contains("名")
                    && !msg.contains("种类") && !msg.contains("是否") && !msg.contains("短信")
                    && !msg.contains("选项") && !msg.contains("银行") && !msg.contains("标志")
                    && !msg.contains("状态") && !msg.contains("版本") && !msg.contains("设置")
                    && !msg.contains("密码") && !msg.contains("验证码")) {
                Map<String, String> map = CommUtils.extractAllAttributes(msg);
                if (!clist.contains(map.get("name"))) {
                    TuominField tuominField = new TuominField(map.get("name"), map.get("desc"));
                    list.add(tuominField);
                }
                clist.add(map.get("name"));
            }
        }));
    }

    public static void tuominAddress(List<String> mlist) {
        mlist.forEach((msg -> {
            if (msg.contains("dataField") && (msg.contains("地址"))
                    && !msg.contains("类型") && !msg.contains("日") && !msg.contains("电话")
                    && !msg.contains("期") && !msg.contains("名")
                    && !msg.contains("种类") && !msg.contains("是否") && !msg.contains("短信")
                    && !msg.contains("选项") && !msg.contains("银行") && !msg.contains("标志")
                    && !msg.contains("状态") && !msg.contains("版本") && !msg.contains("设置")
                    && !msg.contains("密码")) {
                Map<String, String> map = CommUtils.extractAllAttributes(msg);
                if (!clist.contains(map.get("name"))) {
                    TuominField tuominField = new TuominField(map.get("name"), map.get("desc"));
                    list.add(tuominField);
                }
                clist.add(map.get("name"));
            }
        }));
    }

    public static void tuominEmail(List<String> mlist) {
        mlist.forEach((msg -> {
            if (msg.contains("dataField") && (msg.contains("邮箱"))
                    /*&& !msg.contains("类型") && !msg.contains("日") && !msg.contains("电话")
                    && !msg.contains("期") && !msg.contains("名")
                    && !msg.contains("种类") && !msg.contains("是否") && !msg.contains("短信")
                    && !msg.contains("选项") && !msg.contains("银行") && !msg.contains("标志")
                    && !msg.contains("状态") && !msg.contains("版本") && !msg.contains("设置")
                    && !msg.contains("密码")*/
            ) {
                Map<String, String> map = CommUtils.extractAllAttributes(msg);
                if (!clist.contains(map.get("name"))) {
                    TuominField tuominField = new TuominField(map.get("name"), map.get("desc"));
                    list.add(tuominField);
                }
                clist.add(map.get("name"));
            }
        }));
    }

}
