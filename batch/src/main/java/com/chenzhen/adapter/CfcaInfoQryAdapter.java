package com.chenzhen.adapter;

import cfca.yuzhi.fep.toolkit.ClientContext;
import cfca.yuzhi.vo.request.CertServiceRequestTx13VO;
import cfca.yuzhi.vo.response.CertServiceResponseTx13VO;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.mapper.BatchMapper;
import com.chenzhen.pojo.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Component
public class CfcaInfoQryAdapter implements SocketMessageAdapter {

    @Resource
    BatchMapper batchMapper;

    @Override
    public void handleMessage(Message message) {
        JSONObject result = new JSONObject();
        if (StringUtils.hasText(message.getMessage())) {

            StringBuffer sbString = new StringBuffer();

            CertServiceRequestTx13VO request = new CertServiceRequestTx13VO();

            request.setKeyID(message.getMessage());

            CertServiceResponseTx13VO response = ClientContext.getInstance().tx1301(request);
            String code = response.getCode();
            String msg = response.getMessage();
            result.put("code", code);
            if ("2000".equals(code)) {
                String status = response.getCertStatus();
                String statusMsg = "";
                switch (status) {
                    case "3":
                        statusMsg = "未下载";
                        break;
                    case "4":
                        statusMsg = "激活";
                        break;
                    case "5":
                        statusMsg = "冻结";
                        break;
                    case "6":
                        statusMsg = "吊销";
                        break;
                    default:
                        statusMsg = status;
                        break;
                }


                StringBuffer sb = new StringBuffer();
                sb.append("key编号：" + response.getKeyID()).append(", SN：" + response.getSubjectDn()).append(", DN：" + response.getIssueDn())
                        .append(", 用户名：" + response.getUserName()).append(", 证件类型：" + response.getIdentificationType())
                        .append(", 证件号码：" + response.getIdentificationNo()).append(", 生效日：" + response.getStartTime())
                        .append(", 截止日：" + response.getEndTime()).append(", 证书状态：" + statusMsg)
                        .append(", 证书序列号：" + response.getSerialNo());
                result.put("msg", sb.toString());
            } else {
                result.put("msg", msg);

            }

        }
        Message msg = new Message(message.getType(), message.getUrl(), message.getMessage(), result.toString(), "1", message.getDate(), "", "","");
        batchMapper.updateSocketMessage(msg);
    }
}
