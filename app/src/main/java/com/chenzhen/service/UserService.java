package com.chenzhen.service;

import com.chenzhen.pojo.WhiteUser;

import java.util.List;

public interface UserService {

    List<WhiteUser> queryWhiteInfo(WhiteUser whiteUser);

    int addWhite(WhiteUser user);

    int updateWhiteByUsername(WhiteUser user);

    int updateWhiteByIp(WhiteUser user);

    int deleteWhite(WhiteUser user);

    String getUserName(String clientIp);

    String getUserNameByStatus (String clientIp);
}
