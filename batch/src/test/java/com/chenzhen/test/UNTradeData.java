package com.chenzhen.test;

import com.chenzhen.util.CommUtils;
import com.chenzhen.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UNTradeData {

    private final static String FLOWNO = "E250911122631256";
    private final static String TYPE = "TP";

    public static void main(String[] args) throws Exception {
        Connection conn = JdbcUtil.getConnection();
        String sql = "SELECT CTD_DATA FROM CPR_TRADE_DATA where CTD_TRADE_FLOWNO =? AND CTD_DATATYPE =? ";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, FLOWNO);
        ps.setString(2, TYPE);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String dataString = rs.getString(1);
            dataString = CommUtils.unzip(dataString);
            System.out.println(dataString);
        }


    }
}
