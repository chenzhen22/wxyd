package com.cyz.batch.controller;

import com.cyz.batch.model.BatchTask;
import com.cyz.batch.service.BatchTaskService;
import com.cyz.constant.ErrorEnum;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 批次任务控制器（/batch/**，AuthInterceptor 鉴权 + 仅管理员）。
 * <p>
 * 任务可在服务器任意目录执行脚本，属于高权限能力，菜单隐藏之外接口也必须真拦截。
 */
@RestController
@RequestMapping("batch")
@Slf4j
public class BatchTaskController {

    @Autowired
    private BatchTaskService batchTaskService;

    private boolean notAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role == null || role != 0;
    }

    /** 任务列表（含上次/下次执行时间、次数、最近状态） */
    @GetMapping("list")
    public Result list(HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        Map<String, Object> body = new HashMap<>();
        body.put("list", batchTaskService.list());
        result.setBody(body);
        return result;
    }

    /**
     * 保存可编辑项：名称/任务目录/执行脚本/执行方式/执行计划/启用/超时。
     * 改动执行计划后自动重算 next_run_time。
     */
    @PostMapping("save")
    public Result save(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        Map<String, Object> body = body(req);
        try {
            result.setBody(batchTaskService.save(fromBody(body)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 新建任务（任务ID 由系统生成，前端无需传入） */
    @PostMapping("add")
    public Result add(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        try {
            result.setBody(batchTaskService.create(fromBody(body(req))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 立即执行（异步，接口立即返回） */
    @PostMapping("run")
    public Result run(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        Long id = longVal(body(req).get("id"));
        if (id == null) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg("缺少任务 id");
            return result;
        }
        try {
            batchTaskService.runNow(id);
            result.setBody("已开始执行，稍后刷新查看结果");
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 预览执行计划：返回接下来 count 次（默认 5 次）执行时间，同时用于表达式校验 */
    @GetMapping("preview")
    public Result preview(@RequestParam(required = false) String plan,
                          @RequestParam(defaultValue = "5") Integer count,
                          HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("plan", plan == null ? "" : plan.trim());
            body.put("list", batchTaskService.preview(plan, count == null ? 5 : count));
            result.setBody(body);
        } catch (IllegalArgumentException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 最近一次执行日志（默认 200 行） */
    @GetMapping("log")
    public Result log(@RequestParam Long id,
                      @RequestParam(defaultValue = "200") Integer lines,
                      HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        int n = lines == null || lines < 1 ? 200 : Math.min(lines, 2000);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("log", batchTaskService.tailLog(id, n));
            result.setBody(body);
        } catch (IllegalArgumentException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /* ---------------- 辅助 ---------------- */

    /** 请求体 -> 任务对象（新增/修改共用；任务ID 不接收） */
    private static BatchTask fromBody(Map<String, Object> body) {
        BatchTask in = new BatchTask();
        in.setId(longVal(body.get("id")));
        in.setName(str(body.get("name")));
        in.setWorkDir(str(body.get("workDir")));
        in.setScript(str(body.get("script")));
        in.setExecMode(str(body.get("execMode")));
        in.setPlan(str(body.get("plan")));
        String enabled = str(body.get("enabled"));
        in.setEnabled(("1".equals(enabled) || "true".equalsIgnoreCase(enabled)) ? 1 : 0);
        Object timeout = body.get("timeoutSec");
        in.setTimeoutSec(timeout == null ? null : intVal(timeout));
        return in;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static Long longVal(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer intVal(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) return new HashMap<>();
        return (Map<String, Object>) req.getBody();
    }
}
