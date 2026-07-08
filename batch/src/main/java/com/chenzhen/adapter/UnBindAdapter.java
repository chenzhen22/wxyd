package com.chenzhen.adapter;

import cfca.yuzhi.fep.toolkit.ClientContext;
import cfca.yuzhi.vo.request.CertServiceRequestTx11VO;
import cfca.yuzhi.vo.response.CertServiceResponseTx11VO;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.mapper.BatchMapper;
import com.chenzhen.pojo.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Component
public class UnBindAdapter implements SocketMessageAdapter {

    @Resource
    BatchMapper batchMapper;

    @Override
    public void handleMessage(Message message) {
        JSONObject result = new JSONObject();
        if (StringUtils.hasText(message.getMessage())) {

            CertServiceRequestTx11VO request = new CertServiceRequestTx11VO();

            request.setKeyID(message.getMessage());

            CertServiceResponseTx11VO response = ClientContext.getInstance().tx1103(request);
            String code = response.getCode();
            result.put("code", code);
            result.put("msg", response.getMessage());
            Message msgg = new Message(message.getType(), message.getUrl(), message.getMessage(), result.toString(), "1", message.getDate(), "", "","");
            batchMapper.updateSocketMessage(msgg);
        }
    }
}
