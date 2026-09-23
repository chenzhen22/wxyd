package com.cyz.pojo;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String displayName;
    private Integer role;
    private Integer status;
    private String createTime;
    private String theme;
}
