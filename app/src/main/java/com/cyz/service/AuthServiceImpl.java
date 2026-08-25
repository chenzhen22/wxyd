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
        if (u == null || u.getPassword() == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        try {
            // SM4 CBC 随机 IV：不能比较两次密文，必须解密存储的密文后比对明文
            String storedPlain = sm4KeyHolder.decrypt(u.getPassword());
            if (!storedPlain.equals(password)) {
                throw new IllegalArgumentException("用户名或密码错误");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            // 解密失败（密文损坏/格式不符）视为密码错误，不抛 500
            throw new IllegalArgumentException("用户名或密码错误");
        }
        u.setPassword(null);
        return u;
    }
}
