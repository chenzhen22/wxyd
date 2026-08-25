package com.cyz.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSericeImpl implements UserService {

    @Autowired
    private MysqlMapper mysqlMapper;

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
    public int deleteUser(Long id) {
        return mysqlMapper.deleteUser(id);
    }

    @Override
    public String getUserName(String clientIp) {
        // 旧 client 表已废弃；留言板改用登录用户名（见 MessageController 改造）
        return null;
    }
}
