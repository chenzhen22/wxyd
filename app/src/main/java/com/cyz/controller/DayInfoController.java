package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.service.DayInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 金融日报控制器。
 * <p>
 * 推送入口 {@code POST /common/dayinfo.do}（DoSuffixFilter 剥 .do 后映射 common/dayinfo）：
 * 免登录，外部脚本 SM2 加密 + SM4 加签，后端验签解密鉴权——照 RadarController 模式
 * 不实现 {@link com.cyz.controller.CommController}（避开 MockAspect 兜底拦截），也不加入 AuthInterceptor 保护清单。
 * <p>
 * 查询与订阅端点在 {@code dayinfo/**}，已加入 WebMvcConfig 保护清单（登录可见）。
 */
@RestController
@Slf4j
public class DayInfoController {

    @Autowired
    private DayInfoService dayInfoService;

    /* ==================== 推送（免登录，加密验签） ==================== */

    @PostMapping("common/dayinfo")
    public Result push(@RequestBody(required = false) Map<String, Object> req) {
        Result result = Result.getInstance();
        if (req == null) {
            result.setErrorMsg("请求体不能为空");
            result.setErrorCode("000015");
            return result;
        }
        Long timestamp = toLong(req.get("timestamp"));
        String data = toStr(req.get("data"));
        String sign = toStr(req.get("sign"));
        try {
            String err = dayInfoService.push(timestamp, data, sign);
            if (err != null) {
                result.setErrorMsg(err);
                result.setErrorCode("000015");
            }
        } catch (IllegalArgumentException e) {
            result.setErrorMsg(e.getMessage());
            result.setErrorCode("000015");
        } catch (Exception e) {
            log.error("[DAYINFO] 推送处理异常", e);
            result.setErrorMsg("处理失败，请稍后重试");
            result.setErrorCode("000015");
        }
        return result;
    }

    /* ==================== 查询（登录） ==================== */

    @GetMapping("dayinfo/list")
    public Result list(@RequestParam(defaultValue = "finance") String type,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Result result = Result.getInstance();
        Map<String, Object> body = new HashMap<>();
        body.put("total", dayInfoService.count(type));
        body.put("list", dayInfoService.list(type, pageNum, pageSize));
        result.setBody(body);
        return result;
    }

    /** 最新一期（进入页面默认展示） */
    @GetMapping("dayinfo/latest")
    public Result latest(@RequestParam(defaultValue = "finance") String type) {
        Result result = Result.getInstance();
        result.setBody(dayInfoService.latest(type));
        return result;
    }

    @GetMapping("dayinfo/detail")
    public Result detail(@RequestParam Long id) {
        Result result = Result.getInstance();
        result.setBody(dayInfoService.detail(id));
        return result;
    }

    /* ==================== 订阅提醒（登录，一人多条：钉钉机器人 + 邮箱可配多个） ==================== */

    /** 我的订阅列表 */
    @GetMapping("dayinfo/subscribe/my")
    public Result mySubscribe(HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(dayInfoService.mySubscribe(userId(session)));
        return result;
    }

    /** 新增一条订阅（channel=dingtalk 带 robotId，channel=email 带 email） */
    @PostMapping("dayinfo/subscribe/add")
    public Result addSubscribe(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            result.setBody(dayInfoService.addSubscribe(userId(session),
                    str(body.get("channel")), longVal(body.get("robotId")), str(body.get("email"))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 删除一条订阅 */
    @PostMapping("dayinfo/subscribe/delete")
    public Result deleteSubscribe(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        if (!dayInfoService.deleteSubscribe(userId(session), longVal(body.get("id")))) {
            return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        }
        return result;
    }

    /** 启用/暂停一条订阅 */
    @PostMapping("dayinfo/subscribe/toggle")
    public Result toggleSubscribe(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        Boolean enabled = boolVal(body.get("enabled"));
        if (!dayInfoService.toggleSubscribe(userId(session), longVal(body.get("id")), Boolean.TRUE.equals(enabled))) {
            return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        }
        return result;
    }

    /** 测试推送：把最新一期日报立即推给指定订阅，验证通道连通性 */
    @PostMapping("dayinfo/subscribe/test")
    public Result testSubscribe(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            String err = dayInfoService.testSubscribe(userId(session), longVal(body.get("id")));
            if (err != null) {
                result.setErrorMsg("推送失败：" + err);
                result.setErrorCode("000015");
            } else {
                result.setBody("推送成功，请查收");
            }
        } catch (IllegalArgumentException e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /* ==================== 辅助 ==================== */

    private static Long userId(HttpSession session) {
        Long id = (Long) session.getAttribute("userId");
        return id == null ? 0L : id;
    }

    private static String toStr(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long longVal(Object o) {
        return toLong(o);
    }

    private static Boolean boolVal(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.valueOf(String.valueOf(o));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) {
            return new HashMap<>();
        }
        return (Map<String, Object>) req.getBody();
    }
}
