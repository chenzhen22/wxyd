package com.cyz.service;

import cn.hutool.core.thread.ThreadUtil;
import com.cyz.config.Sm4KeyHolder;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.DingRobot;
import com.cyz.util.DingTalkRobotUtil;
import com.cyz.util.ZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RobotServiceImpl implements RobotService {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Override
    public List<DingRobot> list(Long userId) {
        List<DingRobot> list = mysqlMapper.listRobotsByUserId(userId);
        for (DingRobot r : list) {
            r.setSecret(mask(r.getSecret()));
        }
        return list;
    }

    @Override
    public int addRobot(Long userId, DingRobot robot) {
        robot.setUserId(userId);
        robot.setSecret(sm4KeyHolder.encrypt(robot.getSecret()));
        return mysqlMapper.insertRobot(robot);
    }

    @Override
    public int updateRobot(Long userId, DingRobot robot) {
        robot.setUserId(userId);
        if (robot.getSecret() != null && !robot.getSecret().isEmpty()) {
            robot.setSecret(sm4KeyHolder.encrypt(robot.getSecret()));
        }
        return mysqlMapper.updateRobot(robot);
    }

    @Override
    public int deleteRobot(Long userId, Long id) {
        return mysqlMapper.deleteRobot(id, userId);
    }

    @Override
    public Map<String, Object> send(Long userId, Long robotId, String type, String text, MultipartFile file) throws Exception {
        DingRobot robot = mysqlMapper.queryRobotById(robotId);
        Map<String, Object> resp = new HashMap<>();
        if (robot == null || !userId.equals(robot.getUserId())) {
            resp.put("error", "机器人不存在或无权操作");
            return resp;
        }
        String accessToken = robot.getAccessToken();
        String secret = sm4KeyHolder.decrypt(robot.getSecret());
        DingTalkRobotUtil.RobotConfig config = new DingTalkRobotUtil.RobotConfig(accessToken, secret);

        if ("text".equals(type)) {
            if(text.length()/1024 > 18) {
                throw new IllegalArgumentException("文本大小超过限制（最大 20KB）");
            }
            String r = DingTalkRobotUtil.sendTextMsg(config, text, null, false);
            resp.put("type", "text");
            resp.put("result", r);
            return resp;
        }

        // file —— 限制文件大小最大 200KB
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        long maxFileSize = 800 * 1024L; // 800KB
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制（最大 800KB）");
        }
        byte[] fileBytes = file.getBytes();
        String filename = file.getOriginalFilename();
        String base64 = null;
        if(filename.endsWith(".7z") || filename.endsWith(".zip")) {
            base64 = ZipUtils.zipToBase64(fileBytes);
        } else {
            base64 = ZipUtils.compressToBase64(fileBytes, filename);
        }

        int threshold = 20480;   // 20KB
        int maxChunk = 18432;     // 18KB
        List<String> chunks = new ArrayList<>();
        if (base64.length() > threshold) {
            for (int i = 0; i < base64.length(); i += maxChunk) {
                chunks.add(base64.substring(i, Math.min(i + maxChunk, base64.length())));
            }
        } else {
            chunks.add(base64);
        }
        int total = chunks.size();
        Random random = new Random();
        List<String> results = new ArrayList<>();
        for (int idx = 1; idx <= total; idx++) {
            String msg = String.format("[FILE|%s|%d|%d|%s]", filename, idx, total, chunks.get(idx-1));
            String r = DingTalkRobotUtil.sendTextMsg(config, msg, null, false);
            TimeUnit.SECONDS.sleep(random.nextInt(4) + 1);
            results.add(r);
        }
        resp.put("type", "file");
        resp.put("filename", filename);
        resp.put("totalChunks", total);
        resp.put("results", results);
        return resp;
    }

    private String mask(String secret) {
        if (secret == null) return null;
        return "****";
    }
}
