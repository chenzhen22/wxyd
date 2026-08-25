package com.cyz.pojo;

import lombok.Data;

@Data
public class DingRobot {
    private Long id;
    private Long userId;
    private String name;
    private String accessToken;
    private String secret;
    private String createTime;
}
