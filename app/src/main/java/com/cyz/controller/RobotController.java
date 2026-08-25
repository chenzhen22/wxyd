package com.cyz.controller;

import com.cyz.pojo.DingRobot;
import com.cyz.pojo.Result;
import com.cyz.service.RobotService;
import com.cyz.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class RobotController implements CommController {

    @Autowired
    private RobotService robotService;

    @ResponseBody
    @RequestMapping("robot/list")
    public Result list(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Result result = Result.getInstance();
        result.setBody(robotService.list(uid));
        return result;
    }

    @ResponseBody
    @RequestMapping("robot/add")
    public Result add(@RequestBody Result result, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        DingRobot robot = new DingRobot();
        robot.setName((String) map.get("name"));
        robot.setAccessToken((String) map.get("accessToken"));
        robot.setSecret((String) map.get("secret"));
        int idx = robotService.addRobot(uid, robot);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("robot/update")
    public Result update(@RequestBody Result result, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        DingRobot robot = new DingRobot();
        robot.setId(((Number) map.get("id")).longValue());
        robot.setName((String) map.get("name"));
        robot.setAccessToken((String) map.get("accessToken"));
        robot.setSecret((String) map.get("secret"));
        int idx = robotService.updateRobot(uid, robot);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("robot/delete")
    public Result delete(@RequestBody Result result, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        int idx = robotService.deleteRobot(uid, id);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("robot/send")
    public Result send(@RequestParam("robotId") Long robotId,
                       @RequestParam("type") String type,
                       @RequestParam(value = "text", required = false) String text,
                       @RequestParam(value = "file", required = false) MultipartFile file,
                       HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Result result = Result.getInstance();
        try {
            Map<String, Object> resp = robotService.send(uid, robotId, type, text, file);
            result.setBody(resp);
        } catch (Exception e) {
            log.error("机器人发送失败", e);
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }
}
