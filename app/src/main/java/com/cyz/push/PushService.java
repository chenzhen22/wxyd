package com.cyz.push;

import com.cyz.constant.ErrorEnum;
import com.cyz.pojo.DingRobot;
import com.cyz.pojo.Result;
import com.cyz.service.MailService;
import com.cyz.service.RobotService;
import com.cyz.util.DingTalkRobotUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 系统推送统一出口（羊毛雷达 / 巡检日报共用）。
 * <p>
 * 钉钉：校验机器人归属后走 SM4 解密 + sendMarkdownMsg（markdown 格式，日报排版可读）；
 * 邮件：委托 MailService。
 * <p>
 * 🔒 归属校验：robotId 必须属于该 userId，防止拿着别人的机器人发消息。
 * 推送是旁路能力：失败只记日志不抛异常（由调用方决定是否重试/更新水位）。
 */
@Service
@Slf4j
public class PushService {

    @Autowired
    private RobotService robotService;

    @Autowired
    private MailService mailService;

    @Autowired
    private com.cyz.mapper.mysqlMapper.MysqlMapper mysqlMapper;

    @Autowired
    private com.cyz.config.Sm4KeyHolder sm4KeyHolder;

    /**
     * 钉钉 markdown 推送。
     * @return null=成功；非 null=失败原因（调用方决定是否重试）
     */
    public String pushDingtalk(long userId, long robotId, String title, String markdown) {
        try {
            DingRobot robot = mysqlMapper.queryRobotById(robotId);
            if (robot == null || robot.getUserId() == null || robot.getUserId() != userId) {
                return "机器人不存在或无权操作";
            }
            String accessToken = robot.getAccessToken();
            String secret = sm4KeyHolder.decrypt(robot.getSecret());
            DingTalkRobotUtil.RobotConfig config = new DingTalkRobotUtil.RobotConfig(accessToken, secret);
            String resp = DingTalkRobotUtil.sendMarkdownMsg(config, title, markdown);
            // 钉钉 HTTP 恒 200，业务结果在 errcode：≠0 = 被拒（关键词不匹配/频率限制/ IP 白名单等）
            // 不解析会把失败误报为成功 —— 用户"看到成功但群里没消息"就是这么来的
            String fail = parseDingtalkError(resp);
            if (fail != null) {
                log.warn("[PUSH] 钉钉拒绝：user={} robot={} resp={}", userId, robotId, resp);
                return fail;
            }
            log.info("[PUSH] 钉钉推送完成：user={} robot={}", userId, robotId);
            return null;
        } catch (Exception e) {
            log.error("[PUSH] 钉钉推送失败：user={} robot={}", userId, robotId, e);
            return e.getMessage();
        }
    }

    /** 解析钉钉响应，errcode≠0 时返回可读失败原因；响应不可解析视为失败（保守） */
    private String parseDingtalkError(String resp) {
        if (resp == null || resp.isEmpty()) return "钉钉无响应";
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(resp);
            if (json == null) return "钉钉响应不可解析";
            int errcode = json.getIntValue("errcode", -1);
            if (errcode == 0) return null;
            String errmsg = json.getString("errmsg");
            String hint;
            if (errmsg != null && errmsg.toLowerCase().contains("keywords")) {
                hint = "（机器人安全设置含「自定义关键词」，推送内容未包含；请调整机器人关键词或在钉钉后台改为「加签」）";
            } else if (errmsg != null && errmsg.toLowerCase().contains("ip")) {
                hint = "（机器人开了 IP 白名单，需把服务器出口 IP 加入白名单）";
            } else {
                hint = "";
            }
            return "errcode=" + errcode + " " + (errmsg == null ? "未知错误" : errmsg) + hint;
        } catch (Exception e) {
            return "钉钉响应解析失败：" + brief(resp);
        }
    }

    private static String brief(String s) {
        return s.length() > 150 ? s.substring(0, 150) : s;
    }

    /**
     * 邮件推送。@return null=成功；非 null=失败原因
     */
    public String pushEmail(String to, String subject, String content) {
        try {
            mailService.send(to, subject, content);
            log.info("[PUSH] 邮件推送完成：to={} subject={}", to, subject);
            return null;
        } catch (Exception e) {
            log.error("[PUSH] 邮件推送失败：to={}", to, e);
            return e.getMessage();
        }
    }
}
