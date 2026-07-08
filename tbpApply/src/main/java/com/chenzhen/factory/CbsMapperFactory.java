package com.chenzhen.factory;

import com.chenzhen.mapper.AtsMapper;
import com.chenzhen.mapper.CbsMapper;
import com.chenzhen.mapper.firstMapper.FirstMapper;
import com.chenzhen.mapper.secondMapper.SecondMapper;
import com.chenzhen.mapper.sit2cbsMapper.Sit2cbsMapper;
import com.chenzhen.mapper.sitcbsMapper.SitcbsMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CbsMapperFactory {
    @Resource
    FirstMapper firstMapper;

    @Resource
    SecondMapper secondMapper;

    @Resource
    SitcbsMapper sitcbsMapper;

    @Resource
    Sit2cbsMapper sit2cbsMapper;

    public CbsMapper getCbsMapper(String type) {
        switch (type) {
            case "0":
                return sitcbsMapper;
            case "1":
                return sit2cbsMapper;
            case "2":
                return firstMapper;
            case "3":
                return secondMapper;
        }
        return null;
    }
}
