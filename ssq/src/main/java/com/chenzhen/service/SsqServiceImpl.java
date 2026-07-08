package com.chenzhen.service;

import com.chenzhen.mapper.SsqMapper;
import com.chenzhen.pojo.Number;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class SsqServiceImpl implements SsqService {

    @Resource
    SsqMapper mapper;

    @Override
    public void doMarge(int index) {
        int pageIndex = index;
        int pageSize = 200;
        while (true) {
            int start = (pageIndex - 1) * pageSize;
            int end = pageSize;
            boolean flag = queryNum(start, end);
            if (flag) {
                pageIndex ++ ;
            }
        }
    }

    private boolean queryNum(int start, int end) {
        boolean flag = false;

        List<Number> list = mapper.queryNum(start, end);
        if (null != list && list.size() > 0) {
            flag = true;
        }
        list.forEach((number -> {
            updateNum(number.getNum());
        }));

        return flag;
    }

    @Override
    public void updateNum(String num) {
        int result = 0;
        try {
            result = mapper.addNum(num);
            if (result < 1) {
                mapper.updateNum(num);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            try {
                mapper.updateNum(num);
            } catch (Exception e1) {
                log.info(e1.getMessage());
            }

        }
        if (result < 1) {
            try {
                mapper.updateNum(num);
            } catch (Exception e1) {
                log.info(e1.getMessage());
            }
        }

    }
}
