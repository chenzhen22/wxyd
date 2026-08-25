package com.cyz.service;

import com.cyz.config.Sm4KeyHolder;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Override
    public User register(String username, String password, String displayName) {
        if (mysqlMapper.queryUserByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(sm4KeyHolder.encrypt(password));
        u.setDisplayName(displayName);
        u.setRole(1);
        u.setStatus(1); // 待审批
        mysqlMapper.insertUser(u);
        u.setPassword(null);
        return u;
    }

    @Override
    public User login(String username, String password) {
        User u = mysqlMapper.queryUserByUsername(username);
        if (u == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String inputCipher = sm4KeyHolder.encrypt(password);
        if (!inputCipher.equals(u.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        u.setPassword(null);
        return u;
    }
}
