package com.chenzhen.test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class resolveDatadict {

    // <dataField name="" desc="" />
    private final static String dataDictStr1 = "<dataField name=\"";
    private final static String dataDictStr2 = "\" desc=\"";
    private final static String dataDictStr3 = "\" />";


    // <ISS_DATE>$!ISS_DATE</ISS_DATE>
    private final static String esbTempStr1 = "<";
    private final static String esbTempStr2 = ">$!";
    private final static String esbTempStr3 = "</";
    private final static String esbTempStr4 = ">";

    //<gw:field name="CLIENT_NAME" refName="CLIENT_NAME" need="false"  desc="核心客户名称" />
    private final static String msgDefineStr1 = "<gw:field name=\"";
    private final static String msgDefineStr2 = "\" refName=\"";
    private final static String msgDefineStr3 = "\" need=\"false\"  desc=\"";
    private final static String msgDefineStr4 = "\" />";

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        String str = "WALET_ID\t钱包ID\n" +
                "CCY\t币种\n" +
                "CTRL_BAL\t控制余额\n" +
                "FRZ_BAL\t冻结余额\n" +
                "WALET_CURR_USAB_BAL\t钱包当前可用余额\n" +
                "WALET_BAL_LMT\t钱包余额限额\n" +
                "WALET_SINGL_TXN_LMT\t钱包单笔交易限额\n" +
                "WALET_INTR_TXN_LMT\t钱包当日交易限额\n" +
                "WALET_YEAR_TXN_LMT\t钱包年交易限额\n" +
                "AGGR_FRZ_AMT\t累计冻结金额\n" +
                "WALET_DAY_DEBIT_AMT_SUM\t钱包日借方发生额汇总\n" +
                "WALET_YEAR_DEBIT_AMT_SUM\t钱包年借方发生额汇总\n" +
                "WALET_DAY_TXN_REMA_LMT\t钱包单日交易剩余限额\n" +
                "WALET_SINGL_OUT_LMT\t钱包单笔兑出限额\n" +
                "WALET_INTR_OUT_LMT\t钱包当日兑出限额\n" +
                "WALET_YEAR_OUT_LMT\t钱包年兑出限额\n" +
                "WALET_SINGL_TURN_LMT\t钱包单笔兑回限额\n" +
                "WALET_INTR_TURN_LMT\t钱包当日兑回限额\n" +
                "WALET_YEAR_TURN_LMT\t钱包年兑回限额\n" +
                "WALET_REMA_DAY_TXN_LMT\t钱包剩余日交易限额\n" +
                "WALET_REMA_YEAR_TXN_LMT\t钱包剩余年交易限额\n" +
                "WALET_REMA_DAY_OUT_LMT\t钱包剩余日兑出限额\n" +
                "WALET_REMA_YEAR_OUT_LMT\t钱包剩余年兑出限额\n" +
                "WALET_REMA_DAY_TURN_LMT\t钱包剩余日兑回限额\n" +
                "WALET_REMA_YEAR_TURN_LMT\t钱包剩余年兑回限额\n" +
                "WALET_DAY_AGGR_USE_LMT\t钱包日累计使用限额\n" +
                "WALET_YEAR_AGGR_USE_LMT\t钱包年累计使用限额\n" +
                "WALET_CAN_ENTER_BAL\t钱包可充值余额\n" +
                "WALET_SINGL_TFR_LMT_SYS\t钱包单笔转钱限额（系统）\n" +
                "WALET_DAY_TFR_LMT_SYS\t钱包日转钱限额（系统）\n" +
                "WALET_YEAR_TFR_LMT_SYS\t钱包年转钱限额（系统）\n" +
                "WALET_REMA_DAY_TFR_LMT_SYS\t钱包剩余日转钱限额（系统）\n" +
                "WALET_REMA_YEAR_TFR_LMT_SYS\t钱包剩余年转钱限额（系统）\n" +
                "WALET_SINGL_OUT_LMT_SYS\t钱包单笔兑出限额（系统）\n" +
                "WALET_DAY_OUT_LMT_SYS\t钱包日兑出限额（系统）\n" +
                "WALET_YEAR_OUT_LMT_SYS\t钱包年兑出限额（系统）\n" +
                "WALET_REMA_DAY_OUT_LMT_SYS\t钱包剩余日兑出限额（系统）\n" +
                "WALET_REMA_YEAR_OUT_LMT_SYS\t钱包剩余年兑出限额（系统）\n" +
                "SINGL_ONLN_TXN_LMT\t单笔线上交易限额\n" +
                "DAY_AGGR_ONLN_TXN_LMT\t日累计线上交易限额\n" +
                "YEAR_AGGR_ONLN_TXN_LMT\t年累计线上交易限额\n" +
                "SINGL_ONLN_OUT_LMT\t单笔线上兑出限额\n" +
                "DAY_AGGR_ONLN_OUT_LMT\t日累计线上兑出限额\n" +
                "YEAR_AGGR_ONLN_OUT_LMT\t年累计线上兑出限额\n" +
                "SINGL_ONLN_TURN_LMT\t单笔线上兑回限额\n" +
                "DAY_AGGR_ONLN_TURN_LMT\t日累计线上兑回限额\n" +
                "YEAR_AGGR_ONLN_TURN_LMT\t年累计线上兑回限额\n" +
                "SINGL_PUB_TFR_PRV_LMT\t单笔公转私限额\n" +
                "DAY_AGGR_PUB_TFR_PRV_LMT\t日累计公转私限额\n" +
                "YEAR_AGGR_PUB_TFR_PRV_LMT\t年累计公转私限额\n" +
                "DAY_AGGR_PUB_TFR_PRV_CNT\t日累计公转私笔数\n" +
                "REMA_DAY_AGGR_ONLN_TXN_LMT\t剩余日累计线上交易限额\n" +
                "REMA_YEAR_AGGR_ONLN_TXN_LMT\t剩余年累计线上交易限额\n" +
                "REMA_DAY_AGGR_ONLN_OUT_LMT\t剩余日累计线上兑出限额\n" +
                "REMA_YEAR_AGGR_ONLN_OUT_LMT\t剩余年累计线上兑出限额\n" +
                "REMA_DAY_AGGR_ONLN_TURN_LMT\t剩余日累计线上兑回限额\n" +
                "REMA_YEAR_AGGR_ONLN_TURN_LMT\t剩余年累计线上兑回限额\n" +
                "REMA_DAY_AGGR_PUB_TFR_PRV_LMT\t剩余日累计公转私限额\n" +
                "REMA_YEAR_AGGR_PUB_TFR_PRV_LMT\t剩余年累计公转私限额\n" +
                "REMA_DAY_AGGR_PUB_TFR_PRV_CNT\t剩余日累计公转私笔数\n";
//		Arrays.asList(str).stream().forEach(s -> System.out.println("截取前:{" + s + "}\r\n截取后:{"s.substring(0, s.indexOf(9)) + "} {" + s.substring(s.indexOf(9) + 1, s.length()) + "}\n"));


        //转成字典
        transformStrToDatadict(str);

        //转成请求报文
        //transformStrToEsbTemp(str);


        //转成返回报文
        //transformStrToMsgDefine(str);
    }


    /*
     *
             将excel表复制的字段转换成数据字典格式，

        ☆☆☆☆☆☆☆☆☆暂时不支持转换list

        直接拖选excel表复制粘贴当做入参，如
   String str = "VOUCHER_NO	业务凭证号码\r\n" +
                "ACCT_STOP_PAY	账户余额止付标志\r\n" +
                "ACCT_CLASS	账户基本类别\r\n" +
                "ACCT_STATUS_UPD_DATE	账户状态变更日期\r\n" +
                "ACCT_LICENSE_NO	账户许可证号\r\n" +
                "ACCT_LICENSE_DATE	账户许可证签发日期\r\n" +
                "LAST_CHANGE_USER_ID	最后修改柜员号\r\n" +
                "APPR_USER_ID	账户审批柜员\r\n" +
                "HOME_BRANCH	客户管理行\r\n" +
                "APPLY_BRANCH	业务申请机构\r\n" +
                "ACCOUNTING_STATUS_UPD_DATE	核算状态变更日期\r\n" +
                "LAST_CHANGE_DATE	最后修改日\r\n" +
                "OPEN_TRAN_DATE	开户后首次交易日期\r\n" +
                "MATURITY_DATE	账户/凭证/结算卡/贷款到期日期\r\n" +
                "";

     */
    public static List<String> transformStrToDatadict(String sourceStr) {

        String[] str = sourceStr.split("\n");
        List list = Arrays.asList(str).stream().map(s->{

            StringBuffer sb = new StringBuffer();
            sb.append(dataDictStr1);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(dataDictStr2);
            sb.append(s.substring(s.indexOf(9) + 1, s.length()));
            sb.append(dataDictStr3);
            String r = sb.toString();
            System.out.println(" "+r);
            return r;
        }).collect(Collectors.toList());

        return list;
    }


    /*
     *
     *  请求报文
             将excel表复制的字段转换成  请求报文格式 ，

        ☆☆☆☆☆☆☆☆☆暂时不支持转换list
        ☆☆☆☆☆☆☆☆☆输出的请求报文 字段值 和 字段名 同名。

        直接拖选excel表复制粘贴当做入参，如
   String str = "VOUCHER_NO	业务凭证号码\r\n" +
                "ACCT_STOP_PAY	账户余额止付标志\r\n" +
                "ACCT_CLASS	账户基本类别\r\n" +
                "ACCT_STATUS_UPD_DATE	账户状态变更日期\r\n" +
                "ACCT_LICENSE_NO	账户许可证号\r\n" +
                "ACCT_LICENSE_DATE	账户许可证签发日期\r\n" +
                "LAST_CHANGE_USER_ID	最后修改柜员号\r\n" +
                "APPR_USER_ID	账户审批柜员\r\n" +
                "HOME_BRANCH	客户管理行\r\n" +
                "APPLY_BRANCH	业务申请机构\r\n" +
                "ACCOUNTING_STATUS_UPD_DATE	核算状态变更日期\r\n" +
                "LAST_CHANGE_DATE	最后修改日\r\n" +
                "OPEN_TRAN_DATE	开户后首次交易日期\r\n" +
                "MATURITY_DATE	账户/凭证/结算卡/贷款到期日期\r\n" +
                "";

     */
    public static List<String>  transformStrToEsbTemp(String sourceStr){
        String[] str = sourceStr.split("\n");
        List list = Arrays.asList(str).stream().map(s->{

            StringBuffer sb = new StringBuffer();
            sb.append(esbTempStr1);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(esbTempStr2);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(esbTempStr3);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(esbTempStr4);
            String r = sb.toString();
            System.out.println(r);
            return r;
        }).collect(Collectors.toList());

        return list;
    }



    /*
     *
     *  返回报文
             将excel表复制的字段转换成  请求报文格式 ，

        ☆☆☆☆☆☆☆☆☆暂时不支持转换list

        直接拖选excel表复制粘贴当做入参，如
   String str = "VOUCHER_NO	业务凭证号码\r\n" +
                "ACCT_STOP_PAY	账户余额止付标志\r\n" +
                "ACCT_CLASS	账户基本类别\r\n" +
                "ACCT_STATUS_UPD_DATE	账户状态变更日期\r\n" +
                "ACCT_LICENSE_NO	账户许可证号\r\n" +
                "ACCT_LICENSE_DATE	账户许可证签发日期\r\n" +
                "LAST_CHANGE_USER_ID	最后修改柜员号\r\n" +
                "APPR_USER_ID	账户审批柜员\r\n" +
                "HOME_BRANCH	客户管理行\r\n" +
                "APPLY_BRANCH	业务申请机构\r\n" +
                "ACCOUNTING_STATUS_UPD_DATE	核算状态变更日期\r\n" +
                "LAST_CHANGE_DATE	最后修改日\r\n" +
                "OPEN_TRAN_DATE	开户后首次交易日期\r\n" +
                "MATURITY_DATE	账户/凭证/结算卡/贷款到期日期\r\n" +
                "";

     */
    public static List<String>  transformStrToMsgDefine(String sourceStr){
        String[] str = sourceStr.split("\n");
        List list = Arrays.asList(str).stream().map(s->{

            StringBuffer sb = new StringBuffer();
            sb.append(msgDefineStr1);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(msgDefineStr2);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(msgDefineStr3);
            sb.append(s.substring(s.indexOf(9) + 1, s.length()));
            sb.append(msgDefineStr4);
            String r = sb.toString();
            System.out.println(" "+r);
            return r;
        }).collect(Collectors.toList());

        return list;
    }

}
