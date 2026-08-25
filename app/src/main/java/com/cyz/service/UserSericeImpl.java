package com.cyz.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.WhiteUser;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserSericeImpl implements UserService {

    @Resource
    MysqlMapper mysqlMapper;

    @Override
    public List<WhiteUser> queryWhiteInfo(WhiteUser whiteUser) {
        return mysqlMapper.queryWhiteInfo(whiteUser);
    }

    @Override
    public int addWhite(WhiteUser user) {
        return mysqlMapper.addWhite(user);
    }

    @Override
    public int updateWhiteByUsername(WhiteUser user) {
        return mysqlMapper.updateWhiteByUsername(user);
    }

    @Override
    public int updateWhiteByIp(WhiteUser user) {
        return mysqlMapper.updateWhiteByIp(user);
    }

    @Override
    public int deleteWhite(WhiteUser user) {
        return mysqlMapper.deleteWhite(user);
    }

    @Override
    public String getUserName(String clientIp) {
        return mysqlMapper.getUserName(clientIp);
    }

    @Override
    public String getUserNameByStatus(String clientIp) {
        return mysqlMapper.getUserNameByStatus(clientIp);
    }
}
