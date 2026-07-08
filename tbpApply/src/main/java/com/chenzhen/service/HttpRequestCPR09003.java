package com.chenzhen.service;

import cfca.yuzhi.vo.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.pojo.SocketMessage;
import com.chenzhen.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.List;

@Slf4j
public class HttpRequestCPR09003 {
	
	private static String msgstr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
			"<Document>\r\n" + 
			"	<header>\r\n" + 
			"		<transCode>TBP0903T</transCode>\r\n" + 
			"		<transGlobalFlow>SCF01202408201544147636</transGlobalFlow>\r\n" + 
			"		<transFlow>SCF01202408201544147636SCF901324</transFlow>\r\n" + 
			"		<transDate>20240820</transDate>\r\n" + 
			"		<transTime>154414</transTime>\r\n" + 
			"		<systemID>SCF</systemID>\r\n" + 
			"		<srcSystemID>SCF</srcSystemID>\r\n" + 
			"		<branchId>800001</branchId>\r\n" + 
			"		<tranTellerNo>M0001</tranTellerNo>\r\n" + 
			"		<acctDate>20240820</acctDate>\r\n" + 
			"		<authrTellerNo />\r\n" + 
			"		<reviewAuthrTellerNo />\r\n" + 
			"		<authrTellerSeqNo />\r\n" + 
			"		<authrPwd />\r\n" + 
			"		<UNIQUE_SEQ_NUM />\r\n" + 
			"		<version />\r\n" + 
			"		<clientIP />\r\n" + 
			"		<macValue />\r\n" + 
			"		<type />\r\n" + 
			"	</header>\r\n" + 
			"	<body>\r\n" + 
			"		<sysId>SCF</sysId>\r\n" + 
			"		<userId>222164</userId>\r\n"; 
			
	public static SocketMessage sendMsg(String userId, String serviceName) {
		String msg = msgstr.concat("		<userNo>"+userId+"</userNo>\r\n" + 
				"	</body>\r\n" + 
				"</Document>");
		log.info("send message: {}", msg);
        String url = "http://" + CommUtils.getParamValue(serviceName) + ":7706/TBPinnerService/CPR09003.do";
        String date = CommUtils.getDateString("yyyyMMddHHmmssS");
        SocketMessage socketMessage = new SocketMessage("1",url,msg,"","0",date,"","");
		return socketMessage;
	}
	
	/**
     * xml转json
     * @param element
     * @param json
     */
    public static void dom4j2Json(Element element,JSONObject json){
        //如果是属性
        for(Object o:element.attributes()){
            Attribute attr=(Attribute)o;
            if(!StringUtil.isEmpty(attr.getValue())){
                json.put("@"+attr.getName(), attr.getValue());
            }
        }
        List<Element> chdEl=element.elements();
        if(chdEl.isEmpty()&&!StringUtil.isEmpty(element.getText())){//如果没有子元素,只有一个值
            json.put(element.getName(), element.getText());
        }

        for(Element e:chdEl){//有子元素
            if(!e.elements().isEmpty()){//子元素也有子元素
                JSONObject chdjson=new JSONObject();
                dom4j2Json(e,chdjson);
                Object o=json.get(e.getName());
                if(o!=null){
                    JSONArray jsona=null;
                    if(o instanceof JSONObject){//如果此元素已存在,则转为jsonArray
                        JSONObject jsono=(JSONObject)o;
                        json.remove(e.getName());
                        jsona=new JSONArray();
                        jsona.add(jsono);
                        jsona.add(chdjson);
                    }
                    if(o instanceof JSONArray){
                        jsona=(JSONArray)o;
                        jsona.add(chdjson);
                    }
                    json.put(e.getName(), jsona);
                }else{
                    if(!chdjson.isEmpty()){
                        json.put(e.getName(), chdjson);
                    }
                }


            }else{//子元素没有子元素
                for(Object o:element.attributes()){
                    Attribute attr=(Attribute)o;
                    if(!StringUtil.isEmpty(attr.getValue())){
                        json.put("@"+attr.getName(), attr.getValue());
                    }
                }
                //if(!isEmpty(e.getText())){
                	Object o=json.get(e.getName());
                    if(o!=null){
                        JSONArray jsona=null;
                        if(o instanceof String){
                            json.remove(e.getName());
                            jsona=new JSONArray();
                            jsona.add((String)o);
                            jsona.add(e.getText());
                        }
                        if(o instanceof JSONArray){
                            jsona=(JSONArray)o;
                            jsona.add(e.getText());
                        }
                        json.put(e.getName(), jsona);
                    }else{
                    	json.put(e.getName(), e.getText());
                    }
            }
        }
    }
}

