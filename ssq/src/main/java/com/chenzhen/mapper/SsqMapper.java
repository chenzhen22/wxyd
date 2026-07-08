package com.chenzhen.mapper;

import com.chenzhen.pojo.Number;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SsqMapper {

    List<Number> queryNum(int start, int end);

    int addNum(String num);

    int updateNum(String num);
}
