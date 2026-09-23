package com.cyz.service;

import com.cyz.pojo.User;

import java.util.List;

public interface UserService {

    List<User> listUsers();

    int approveUser(Long id);

    int rejectUser(Long id);

    int deleteUser(Long id);

    int updateDisplayName(Long id, String displayName);

    int updateUserTheme(Long id, String theme);

    String getTheme(Long id);

    String getUserName(String clientIp);
}
