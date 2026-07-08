package com.chenzhen.test;

import com.chenzhen.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ShuangseqiuTest {

    public static void main(String[] args) {
        int pageIndex = 1;
        int pageSize = 200;
        while (true) {
            int start = (pageIndex -1) * pageSize;
            int end = pageSize;
            boolean flag = queryNum(start, end);
            if(flag) {
                pageIndex ++;
            }
        }
    }

    public static boolean queryNum(int start, int end) {
        boolean flag = false;
        Connection conn = null;
        Statement st = null;
        try {

            conn = JdbcUtil.getConnection();
            String querySql= "select num,numId from shuangseqiu_m order by numid limit "+start+","+end;
            System.out.println(querySql);
            st = conn.createStatement();
            ResultSet rs = st.executeQuery(querySql);
            String num = "";
            while(rs.next()) {
                flag = true;
                num = rs.getString(1);;
                updateNum(num);
            }
            JdbcUtil.close(conn, st);
            return flag;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtil.close(conn, st);
            return flag;
        }
    }

    public static void updateNum(String num) {
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = JdbcUtil.getConnection();
            String insertSql = "insert into ssq values (?, 1)";
            st = conn.prepareStatement(insertSql);
            st.setString(1, num);
            st.executeUpdate();
        } catch (Exception e) {
            try {
                System.out.println(e.getMessage());
                String updateSql = "update `ssq` set `total`=`total`+1 where `num` =?";
                st = conn.prepareStatement(updateSql);
                st.setString(1, num);
                st.executeUpdate();
            } catch (Exception e1) {
                System.out.println(e1.getMessage());
            }

        } finally {
            JdbcUtil.close(conn, st);
        }
    }
}
