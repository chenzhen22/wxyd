package com.cyz.service;

import com.cyz.pojo.DingRobot;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RobotService {

    List<DingRobot> list(Long userId);

    int addRobot(Long userId, DingRobot robot);

    int updateRobot(Long userId, DingRobot robot);

    int deleteRobot(Long userId, Long id);

    Map<String, Object> send(Long userId, Long robotId, String type, String text, MultipartFile file) throws Exception;
}
