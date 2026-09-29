package com.cyz.service;

import com.cyz.config.Sm4KeyHolder;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSericeImpl implements UserService {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Override
    public List<User> listUsers() {
        return mysqlMapper.listUsers();
    }

    @Override
    public int approveUser(Long id) {
        return mysqlMapper.updateUserStatus(id, 0);
    }

    @Override
    public int rejectUser(Long id) {
        return mysqlMapper.updateUserStatus(id, 2);
    }

    @Override
    public int pauseUser(Long id) {
        return mysqlMapper.updateUserStatus(id, 3);
    }

    @Override
    public int resumeUser(Long id) {
        return mysqlMapper.updateUserStatus(id, 0);
    }

    @Override
    public int deleteUser(Long id) {
        mysqlMapper.deleteRobotByUserId(id);
        return mysqlMapper.deleteUser(id);
    }

    @Override
    public User getUserById(Long id) {
        return mysqlMapper.queryUserById(id);
    }

    @Override
    public int updateDisplayName(Long id, String displayName) {
        return mysqlMapper.updateDisplayName(id, displayName);
    }

    @Override
    public int updateUserTheme(Long id, String theme) {
        return mysqlMapper.updateUserTheme(id, theme);
    }

    @Override
    public String getTheme(Long id) {
        return mysqlMapper.selectThemeById(id);
    }

    @Override
    public int updatePassword(Long id, String oldPassword, String newPassword) {
        User u = mysqlMapper.queryUserPwdById(id);
        if (u == null || u.getPassword() == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String storedPlain;
        try {
            // SM4 随机 IV：解密存储密文后与原密码明文比对
            storedPlain = sm4KeyHolder.decrypt(u.getPassword());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("密码校验失败，请联系管理员");
        }
        if (!storedPlain.equals(oldPassword)) {
            throw new IllegalArgumentException("原密码错误");
        }
        return mysqlMapper.updateUserPassword(id, sm4KeyHolder.encrypt(newPassword));
    }

    @Override
    public String getUserName(String clientIp) {
        // 旧 client 表已废弃；留言板改用登录用户名（见 MessageController 改造）
        return null;
    }
}
