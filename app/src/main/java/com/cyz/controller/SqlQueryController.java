package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.SqlQueryResultVO;
import com.cyz.service.SqlQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * SQL 查询工具 — 动态 SQL 查询、分页、导出。
 * <p>
 * 仅超级管理员（session role == 0）可用；普通用户调用返回权限不足。
 * </p>
 */
@RestController
public class SqlQueryController {

    @Autowired
    private SqlQueryService sqlQueryService;

    private boolean isAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role != null && role == 0;
    }

    private Result forbidden() {
        Result r = Result.getInstance();
        r.setErrorCode("000003");
        r.setErrorMsg("权限不足，仅超级管理员可使用 SQL 查询");
        return r;
    }

    /**
     * 获取当前数据库所有表名。
     */
    @ResponseBody
    @RequestMapping("sql/tables")
    public Result tables(HttpSession session) {
        Result r = Result.getInstance();
        if (!isAdmin(session)) return forbidden();
        try {
            List<String> tables = sqlQueryService.getTables();
            r.setBody(tables);
        } catch (Exception e) {
            r.setErrorCode("000003");
            r.setErrorMsg("获取表列表失败: " + e.getMessage());
        }
        return r;
    }

    /**
     * 执行分页查询。请求体：{ body: { sql, page, size } }。
     */
    @ResponseBody
    @RequestMapping("sql/query")
    public Result query(@RequestBody Result result, HttpSession session) {
        Result r = Result.getInstance();
        if (!isAdmin(session)) return forbidden();
        try {
            Map<String, Object> map = (Map<String, Object>) result.getBody();
            String sql = map != null ? (String) map.get("sql") : null;
            int page = map != null && map.get("page") != null
                    ? Integer.parseInt(map.get("page").toString()) : 1;
            int size = map != null && map.get("size") != null
                    ? Integer.parseInt(map.get("size").toString()) : 20;
            if (sql == null || sql.trim().isEmpty()) {
                r.setErrorCode("000003");
                r.setErrorMsg("SQL 语句不能为空");
                return r;
            }
            SqlQueryResultVO vo = sqlQueryService.query(sql, page, size);
            r.setBody(vo);
        } catch (IllegalArgumentException e) {
            r.setErrorCode("000003");
            r.setErrorMsg(e.getMessage());
        } catch (Exception e) {
            r.setErrorCode("000003");
            r.setErrorMsg("查询失败: " + e.getMessage());
        }
        return r;
    }

    /**
     * 导出查询结果（CSV / Excel）。GET 参数：type=csv|xlsx，sql=URL 编码后的 SQL。
     */
    @RequestMapping("sql/export")
    public void export(javax.servlet.http.HttpServletRequest request,
                       HttpServletResponse response) throws Exception {
        // 鉴权：未登录或不是管理员都不允许导出
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("权限不足，仅超级管理员可导出");
            return;
        }
        String type = request.getParameter("type");
        String sql = request.getParameter("sql");
        if (sql != null) {
            sql = URLDecoder.decode(sql, "UTF-8");
        }
        try {
            String fileName = "query_result";
            if ("xlsx".equalsIgnoreCase(type)) {
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + ".xlsx\"");
                sqlQueryService.exportExcel(sql, response.getOutputStream());
            } else {
                response.setContentType("text/csv;charset=UTF-8");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + ".csv\"");
                sqlQueryService.exportCsv(sql, response.getOutputStream());
            }
            response.flushBuffer();
        } catch (IllegalArgumentException e) {
            response.reset();
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(e.getMessage());
        } catch (Exception e) {
            response.reset();
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("导出失败: " + e.getMessage());
        }
    }
}
