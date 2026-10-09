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
            log.info("[PUSH] 钉钉推送完成：user={} robot={} resp={}", userId, robotId, resp);
            return null;
        } catch (Exception e) {
            log.error("[PUSH] 钉钉推送失败：user={} robot={}", userId, robotId, e);
            return e.getMessage();
        }
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
