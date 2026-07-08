package com.chenzhen.controller;

import com.chenzhen.pojo.Result;
import com.chenzhen.service.SsqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wxyd/ssq")
@Slf4j
public class SsqController {

    @Autowired
    SsqService ssqService;

    @GetMapping("marge")
    public Result marge(@Param("pageIndex") int pageIndex) {
        new Thread(() -> {
            //ssqService.doMarge(pageIndex);
        }).start();
        return Result.getInstance();
    }

}
