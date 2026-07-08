package com.chenzhen.factory;

import com.chenzhen.mapper.AtsMapper;
import com.chenzhen.mapper.fourMapper.FourMapper;
import com.chenzhen.mapper.sit2atsMapper.Sit2atsMapper;
import com.chenzhen.mapper.sitatsMapper.SitatsMapper;
import com.chenzhen.mapper.threeMapper.ThreeMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AtsMapperFactory {
    @Resource
    ThreeMapper threeMapper;

    @Resource
    FourMapper fourMapper;

    @Resource
    SitatsMapper sitatsMapper;

    @Resource
    Sit2atsMapper sit2atsMapper;

    public AtsMapper getAtsMapper(String type) {
        switch (type){
            case "0":
                return sitatsMapper;
            case "1":
                return sit2atsMapper;
            case "2":
                return threeMapper;
            case "3":
                return fourMapper;
        }
        return null;
    }
}
