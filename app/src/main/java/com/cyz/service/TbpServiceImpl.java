package com.cyz.service;

import com.cyz.pojo.Result;
import com.cyz.util.CommUtils;
import com.cyz.util.DesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TbpServiceImpl implements TbpService {

    @Override
    public Result dec(String type, String password) {
        Result result = Result.getInstance();
        String secretKey = CommUtils.getParamValue("passworkKey");
        if ("1".equals(type)) {
            password = DesUtil.encode(secretKey, password);
        } else if ("2".equals(type)) {
            password = DesUtil.decode(secretKey, password);
        }
        result.setBody(password.trim());
        return result;
    }
}
