package com.chenzhen.feign;

import com.chenzhen.pojo.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@FeignClient(name = "tbpApply", path = "/tbpApply")
public interface TbpApplyFeign {

    @RequestMapping("mmLogin")
    Result mmLogin(@RequestBody Result result) throws Exception;

    @RequestMapping("ngLogin")
    Result ngLogin(@RequestBody Result result) throws Exception;

    @RequestMapping("addMessage")
    Result addMessage(@RequestBody Result result);

    @RequestMapping("delMessage")
    Result delMessage(@RequestBody Result result);

    @RequestMapping("queryMessage")
    Result queryMessage(@RequestBody Result result);

    @RequestMapping("queryMsgCode")
    Result queryMsgCode(@RequestBody Result result) throws Exception;

    @RequestMapping("queryUdOper")
    Result queryUdOper(@RequestBody Result result) throws IOException;

    @RequestMapping("orderCreate")
    Result orderCreate(@RequestBody Result result) throws Exception;

    @RequestMapping("datadict")
    Result datadict(@RequestBody Result result) throws Exception;

    @RequestMapping("requestData")
    Result requestData(@RequestBody Result result) throws Exception;

    @RequestMapping("responseData")
    Result responseData(@RequestBody Result result) throws Exception;

    @RequestMapping("start")
    Result start(@RequestBody Result result) throws Exception;

    @RequestMapping("queryUdInfo")
    Result queryUdInfo(@RequestBody Result result);

    @RequestMapping("queryCprUser")
    Result queryCprUser(@RequestBody Result result);

    @RequestMapping("udOper")
    Result udOper(@RequestBody Result result);

    @RequestMapping("unBindUkey")
    Result unBindUkey(@RequestBody Result result) throws Exception;

    @RequestMapping("cfcaInfoQry")
    Result cfcaInfoQry(@RequestBody Result result) throws Exception;

    @RequestMapping("queryWhiteInfo")
    Result queryWhiteInfo(@RequestBody Result result) throws Exception;

    @RequestMapping("addWhite")
    Result addWhite(@RequestBody Result result) throws Exception;

    @RequestMapping("updateWhite")
    Result updateWhite(@RequestBody Result result) throws Exception;

    @RequestMapping("deleteWhite")
    Result deleteWhite(@RequestBody Result result) throws Exception;

    @RequestMapping("/docQryAll")
    Result docQryAll(@RequestBody Result result) throws IOException;

    @RequestMapping("/docQry")
    Result docQry(@RequestBody Result result);

    @RequestMapping("/queryDocAll")
    Result queryDocAll(@RequestBody Result result);

    @RequestMapping("/addDocFile")
    Result addDocFile(@RequestBody Result result);

    @RequestMapping("/getParamValue")
    Result getParamValue(@RequestBody Result result);

    @RequestMapping("/uploadDoc")
    Result uploadDoc(@RequestBody Result result);

    @RequestMapping("/uploadDocumentFile")
    Result uploadDocumentFile(@RequestBody Result result);

    @RequestMapping("/queryDocumentFileList")
    Result queryDocumentFileList(@RequestBody Result result);

    @RequestMapping("/deleteDocumentFile")
    Result deleteDocumentFile(@RequestBody Result result);

}
