package com.chenzhen.util;

import com.chenzhen.model.ColumnInfo;
import com.chenzhen.model.TableInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataQuery
{
    public List<TableInfo> getAllTables()
            throws SQLException
    {
        List tableList = new ArrayList();
        StringBuffer sqlBuf = new StringBuffer("select t1.TABLE_NAME, t1.COMMENTS,t1.table_type, t2.CONSTRAINT_NAME unique_name, t2.prim_key, t3.ind_cols noUnique_name, t4.f_key, t5.TABLESPACE_NAME, t5.partition_names");
        sqlBuf.append("  from user_tab_comments t1");
        sqlBuf.append(" left join (");
        sqlBuf.append(" select a.TABLE_NAME, a.CONSTRAINT_NAME, listagg(b.column_name,',') as prim_key from USER_CONSTRAINTS a");
        sqlBuf.append(" left join user_cons_columns b ");
        sqlBuf.append(" on a.table_name = b.table_name and a.CONSTRAINT_name = b.CONSTRAINT_name where a.CONSTRAINT_TYPE = 'P'");
        sqlBuf.append(" group by a.CONSTRAINT_NAME, a.TABLE_NAME");
        sqlBuf.append(" ) t2");
        sqlBuf.append(" on t1.TABLE_NAME = t2.table_name");
        sqlBuf.append("  left join (");
        sqlBuf.append(" select to_char(listagg(k.ind_cols),',') ind_cols, TABLE_NAME from (");
        sqlBuf.append(" select index_name || '(' || to_char(listagg(g.column_name,',')) || ')' as ind_cols,TABLE_NAME from (");
        sqlBuf.append(" select t.index_name, t.column_name, t.TABLE_NAME from user_ind_columns t, user_indexes i");
        sqlBuf.append(" where t.index_name = i.index_name and i.uniqueness = 'NONUNIQUE'");
        sqlBuf.append(" order by t.column_position) g");
        sqlBuf.append(" group by index_name, TABLE_NAME) k");
        sqlBuf.append(" group by TABLE_NAME) t3");
        sqlBuf.append(" on t1.TABLE_NAME = t3.table_name");
        sqlBuf.append(" left join (");
        sqlBuf.append(" select a.TABLE_NAME, listagg(a.CONSTRAINT_NAME || '(' || b.column_name || ')',',') f_key");
        sqlBuf.append(" from USER_CONSTRAINTS a");
        sqlBuf.append(" left join user_cons_columns b");
        sqlBuf.append(" on a.table_name = b.table_name");
        sqlBuf.append(" and a.CONSTRAINT_name = b.CONSTRAINT_name");
        sqlBuf.append(" where a.CONSTRAINT_TYPE = 'R'");
        sqlBuf.append(" group by a.TABLE_NAME) t4");
        sqlBuf.append(" on t1.TABLE_NAME = t4.table_name");
        sqlBuf.append(" left join (");
        sqlBuf.append(" SELECT a.TABLE_NAME, a.TABLESPACE_NAME, listagg(b.column_name,',') partition_names");
        sqlBuf.append(" FROM user_tables a");
        sqlBuf.append(" left join user_part_key_columns b");
        sqlBuf.append(" on a.TABLE_NAME = b.name");
        sqlBuf.append(" group by a.TABLESPACE_NAME, a.TABLE_NAME) t5");
        sqlBuf.append(" on t1.TABLE_NAME = t5.table_name");
        sqlBuf.append(" order by t1.table_type");

        JDBCUtil jdbcUtil = new JDBCUtil();
        Connection conn = jdbcUtil.getConnection();
        PreparedStatement pst = conn.prepareStatement(sqlBuf.toString());
        ResultSet rs = pst.executeQuery();
        TableInfo table = null;
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            String comments = rs.getString("COMMENTS");
            String unionName = rs.getString("UNIQUE_NAME");
            String primKeys = rs.getString("PRIM_KEY");
            String noUniqueName = rs.getString("NOUNIQUE_NAME");
            String forKeys = rs.getString("F_KEY");
            String tableType = rs.getString("TABLE_TYPE");
            String tablespaceName = rs.getString("TABLESPACE_NAME");
            String partitionNames = rs.getString("PARTITION_NAMES");
            table = new TableInfo();
            table.setChName(comments);
            table.setEnName(tableName);
            table.setTableType(tableType);
            table.setDesc("");
            table.setDirLink("'" + tableName + "'!A1");
            table.setNoUnique(noUniqueName);
            table.setOrder("");
            table.setPrimKeys(primKeys);
            table.setUnique(unionName);
            table.setForKeys(forKeys);
            table.setTablespaceName(tablespaceName);
            table.setPartitionNames(partitionNames);

            List columnList = getTableColumnList(conn, tableName);
            table.setColumnList(columnList);
            tableList.add(table);
        }

        JDBCUtil.close(pst, rs, null);

        return tableList;
    }

    public List<TableInfo> getAllTables(String inTabNames, String outTabNames) throws SQLException {
        List tableList = new ArrayList();
        StringBuffer sqlBuf = new StringBuffer("select t1.TABLE_NAME, t1.COMMENTS,t1.table_type, t2.CONSTRAINT_NAME unique_name, t2.prim_key, t3.ind_cols noUnique_name, t4.f_key, t5.TABLESPACE_NAME, t5.partition_names");
        sqlBuf.append("  from user_tab_comments t1");
        sqlBuf.append(" left join (");
        sqlBuf.append(" select a.TABLE_NAME, a.CONSTRAINT_NAME, listagg(b.column_name,',') as prim_key from USER_CONSTRAINTS a");
        sqlBuf.append(" left join user_cons_columns b ");
        sqlBuf.append(" on a.table_name = b.table_name and a.CONSTRAINT_name = b.CONSTRAINT_name where a.CONSTRAINT_TYPE = 'P'");
        sqlBuf.append(" group by a.CONSTRAINT_NAME, a.TABLE_NAME");
        sqlBuf.append(" ) t2");
        sqlBuf.append(" on t1.TABLE_NAME = t2.table_name");
        sqlBuf.append("  left join (");
        sqlBuf.append(" select to_char(listagg(k.ind_cols,',')) ind_cols, TABLE_NAME from (");
        sqlBuf.append(" select index_name || '(' || to_char(listagg(g.column_name,',')) || ')' as ind_cols,TABLE_NAME from (");
        sqlBuf.append(" select t.index_name, t.column_name, t.TABLE_NAME from user_ind_columns t, user_indexes i");
        sqlBuf.append(" where t.index_name = i.index_name and i.uniqueness = 'NONUNIQUE'");
        sqlBuf.append(" order by t.column_position) g");
        sqlBuf.append(" group by index_name, TABLE_NAME) k");
        sqlBuf.append(" group by TABLE_NAME) t3");
        sqlBuf.append(" on t1.TABLE_NAME = t3.table_name");
        sqlBuf.append(" left join (");
        sqlBuf.append(" select a.TABLE_NAME, listagg(a.CONSTRAINT_NAME || '(' || b.column_name || ')',',') f_key");
        sqlBuf.append(" from USER_CONSTRAINTS a");
        sqlBuf.append(" left join user_cons_columns b");
        sqlBuf.append(" on a.table_name = b.table_name");
        sqlBuf.append(" and a.CONSTRAINT_name = b.CONSTRAINT_name");
        sqlBuf.append(" where a.CONSTRAINT_TYPE = 'R'");
        sqlBuf.append(" group by a.TABLE_NAME) t4");
        sqlBuf.append(" on t1.TABLE_NAME = t4.table_name");
        sqlBuf.append(" left join (");
        sqlBuf.append(" SELECT a.TABLE_NAME, a.TABLESPACE_NAME, listagg(b.column_name,',') partition_names");
        sqlBuf.append(" FROM user_tables a");
        sqlBuf.append(" left join user_part_key_columns b");
        sqlBuf.append(" on a.TABLE_NAME = b.name");
        sqlBuf.append(" group by a.TABLESPACE_NAME, a.TABLE_NAME) t5");
        sqlBuf.append(" on t1.TABLE_NAME = t5.table_name");
        sqlBuf.append(" where 1=1  ");
        if ((inTabNames != null) && (!(inTabNames.trim().equals(""))))
            sqlBuf.append(" and lower(t1.TABLE_NAME) like '" + inTabNames.toLowerCase() + "%' ");

        if ((outTabNames != null) && (!(outTabNames.trim().equals(""))))
            sqlBuf.append(" and lower(t1.TABLE_NAME) not like '" + outTabNames.toLowerCase() + "%' ");

        sqlBuf.append(" order by t1.table_type");

        JDBCUtil jdbcUtil = new JDBCUtil();
        Connection conn = jdbcUtil.getConnection();
        PreparedStatement pst = conn.prepareStatement(sqlBuf.toString());
        ResultSet rs = pst.executeQuery();
        TableInfo table = null;
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            String comments = rs.getString("COMMENTS");
            String unionName = rs.getString("UNIQUE_NAME");
            String primKeys = rs.getString("PRIM_KEY");
            String noUniqueName = rs.getString("NOUNIQUE_NAME");
            String forKeys = rs.getString("F_KEY");
            String tableType = rs.getString("TABLE_TYPE");
            String tablespaceName = rs.getString("TABLESPACE_NAME");
            String partitionNames = rs.getString("PARTITION_NAMES");
            table = new TableInfo();
            table.setChName(comments);
            table.setEnName(tableName);
            table.setTableType(tableType);
            table.setDesc("");
            table.setDirLink("'" + tableName + "'!A1");
            table.setNoUnique(noUniqueName);
            table.setOrder("");
            table.setPrimKeys(primKeys);
            table.setUnique(unionName);
            table.setForKeys(forKeys);
            table.setTablespaceName(tablespaceName);
            table.setPartitionNames(partitionNames);

            List columnList = getTableColumnList(conn, tableName);
            table.setColumnList(columnList);
            tableList.add(table);
        }

        JDBCUtil.close(pst, rs, null);

        return tableList; }

    public List<ColumnInfo> getTableColumnList(Connection conn, String tableName) throws SQLException {
        List columnList = new ArrayList();
        StringBuffer sqlBuf = new StringBuffer("select a.column_name,a.comments,c.data_type,c.data_length,c.data_scale,data_precision,c.nullable,c.internal_column_id,c.data_default");
        sqlBuf.append(" from user_col_comments a ");
        sqlBuf.append(" left join user_tab_comments b on a.table_name=b.table_name ");
        sqlBuf.append(" left join user_tab_cols c on a.table_name=c.table_name and a.column_name=c.column_name ");
        sqlBuf.append(" where a.table_name='" + tableName + "'");
        sqlBuf.append(" order by c.internal_column_id");

        PreparedStatement pst = conn.prepareStatement(sqlBuf.toString());
        ResultSet rs = pst.executeQuery();
        ColumnInfo column = null;
        while (rs.next()) {
            String columnName = rs.getString("COLUMN_NAME");
            String comments = rs.getString("COMMENTS");
            String dataType = rs.getString("DATA_TYPE");
            String dataLen = rs.getString("DATA_LENGTH");
            String dataScale = rs.getString("DATA_SCALE");
            String dataPrecision = rs.getString("DATA_PRECISION");
            String nullAble = rs.getString("NULLABLE");
            String order = rs.getString("INTERNAL_COLUMN_ID");
            String defValue = rs.getString("DATA_DEFAULT");

            dataType = (dataType.trim().toUpperCase().contains("TIMESTAMP")) ? "TIMESTAMP" : dataType;

            if (dataPrecision != null)
                dataType = dataType + "(" + dataScale + "," + dataPrecision + ")";
            else {
                dataType = dataType + "(" + dataLen + ")";
            }

            column = new ColumnInfo();
            column.setChName(comments);
            column.setDataType(dataType);
            column.setDefValue(defValue);
            column.setEnName(columnName);
            column.setMark("");
            column.setNullValidate(nullAble);
            column.setOrderNo(order);
            columnList.add(column);
        }

        JDBCUtil.close(pst, rs, null);

        return columnList;
    }
}