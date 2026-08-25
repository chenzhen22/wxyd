package com.dingding.util;

import com.alibaba.fastjson2.JSON;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sqlite.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 钉钉自定义机器人（webhook）发送工具。
 *
 * <p>仅用于「发送」群消息：把文本推进钉钉群。消息文本会跨内外网边界，
 * 由内网接收端的钉钉 PC 客户端接收，再由 {@code DingTalkLocalExporter} 落盘到文件。
 *
 * <p>注意：webhook 机器人是单向推送，无法读取群历史消息。
 */
public class DingTalkRobotUtil {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    /**
     * 钉钉机器人配置
     */
    public static class RobotConfig {
        // webhook中access_token
        public String accessToken;
        // 加签密钥：SEC开头
        public String secret;

        public RobotConfig(String accessToken, String secret) {
            this.accessToken = accessToken;
            this.secret = secret;
        }
    }

    /**
     * 发送Text文本消息
     * @param config 机器人配置
     * @param content 消息内容
     * @param atMobiles 需要@的手机号列表
     * @param isAtAll 是否@所有人
     * @return 返回钉钉响应json字符串
     */
    public static String sendTextMsg(RobotConfig config, String content, List<String> atMobiles, boolean isAtAll) throws Exception {
        // 组装消息体
        Map<String, Object> atMap = new HashMap<>();
        atMap.put("atMobiles", atMobiles == null ? Arrays.asList() : atMobiles);
        atMap.put("isAtAll", isAtAll);

        Map<String, Object> textMap = new HashMap<>();
        textMap.put("content", content);

        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "text");
        body.put("text", textMap);
        body.put("at", atMap);

        String jsonBody = JSON.toJSONString(body);
        String requestUrl = buildSignedUrl(config);
        return doPost(requestUrl, jsonBody);
    }

    /**
     * 构建带timestamp和sign签名的完整URL
     */
    private static String buildSignedUrl(RobotConfig config) throws Exception {
        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + config.secret;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec sk = new SecretKeySpec(config.secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(sk);
        byte[] signBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String signBase64 = Base64.getEncoder().encodeToString(signBytes);
        String signUrlEncode = URLEncoder.encode(signBase64, StandardCharsets.UTF_8.name());

        return String.format("https://oapi.dingtalk.com/robot/send?access_token=%s&timestamp=%d&sign=%s",
                config.accessToken, timestamp, signUrlEncode);
    }

    /**
     * 发起POST请求
     */
    private static String doPost(String url, String jsonBody) throws Exception {
        RequestBody requestBody = RequestBody.create(jsonBody, JSON_MEDIA);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.body() == null) {
                return null;
            }
            return response.body().string();
        }
    }

    // ==================== 测试入口 ====================
    public static void main(String[] args) {
        try {
            // --------------------------【修改这里为你自己的配置】--------------------------
            String ACCESS_TOKEN = "b8269e6a2d77632ffb792220dd890f98aaf7904ff2d3629fda0159984ad79b9e";
            String SECRET = "SEC2a575ed8c3313e38403c8492aaa7cd05b7ad0926c7513599264a67710d1997db";
            // ---------------------------------------------------------------------------
            RobotConfig robotConfig = new RobotConfig(ACCESS_TOKEN, SECRET);
            Path path = Paths.get("D:\\cz\\project\\Notepad++\\plugins\\NPPJSONViewer\\NPPJSONViewer.7z.013");
            String msg = ZipUtils.sevenZToBase64(path);
            // 测试发送消息
            String result = sendTextMsg(
                    robotConfig,
                    msg,
                    null, // 需要@的手机号，不需要则传null
                    false
            );
            System.out.println("钉钉返回结果：" + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
