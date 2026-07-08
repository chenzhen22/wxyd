package com.chenzhen.mapper;

import com.chenzhen.pojo.CprUser;
import com.chenzhen.pojo.MsgBean;

public interface AtsMapper {

    String queryUKData(CprUser cprUser);

    int updateUK(CprUser cprUser);

    MsgBean queryMsgCode(String mobilePhone);

    int udOper(String udStatue);
}
