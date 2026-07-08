package com.chenzhen.util;

import com.chenzhen.config.ProperConfig;
import com.chenzhen.pojo.Result;
import lombok.SneakyThrows;
import net.sf.json.xml.XMLSerializer;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.DocumentException;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

/**
 *
 * @description: 工具类
 * @author: chenzhen 254669004@qq.com
 * @create: 2024-06-06
 *
 **/
public class CommUtils {
    public static int generateRandomStringIndex = 62;

    /**
     * 获取日期格式字符串
     * @param format
     * @return
     */
    public static String getDateString(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = new Date();
        String dateString = sdf.format(date);
        return dateString;
    }

    /**
     * 获取随机数
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        StringBuffer dateString = new StringBuffer();
        Random random = new Random();
        for(int i=0; i<length; i++) {
            dateString.append(random.nextInt(10));
        }
        return dateString.toString();
    }

    /**
     * 获取随机数
     * @param length
     * @return
     */
    public static String generateRandomString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random(Thread.currentThread().getId());
        Random random2 = new Random();

        for (int i = 0; i < length; ++i) {
            if (i % 2 == 0) {
                sb.append("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                        .charAt(random.nextInt(generateRandomStringIndex)));
            } else {
                sb.append("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                        .charAt(random2.nextInt(generateRandomStringIndex)));
            }
        }

        return sb.toString();
    }

    /**
     * 获取带日期格式的随机数
     * @param format
     * @param length
     * @return
     */
    public static String getDateRandomString(String format, int length) {
        StringBuffer dateString = new StringBuffer(getDateString(format));
        Random random = new Random();
        for(int i=0; i<length; i++) {
            dateString.append(random.nextInt(10));
        }
        return dateString.toString();
    }

    /**
     * 字符串相减
     * @param oneStr
     * @param twoStr
     * @return
     */
    public static String StrSubtract(String oneStr, String twoStr) {
        oneStr = StringUtils.hasText(oneStr) ? oneStr : "0";
        twoStr = StringUtils.hasText(twoStr) ? twoStr : "0";
        BigDecimal one = new BigDecimal(oneStr);
        BigDecimal two = new BigDecimal(twoStr);
        one = one.subtract(two);
        return one.toString();
    }

    /**
     * 日期偏移设置
     * @return
     */
    public static String dateSet(String dateString, String format, String year, String month, String day, String minute) {
        year = StringUtils.hasText(year)? year : "0";
        month = StringUtils.hasText(month)? month : "0";
        day = StringUtils.hasText(day)? day : "0";
        minute = StringUtils.hasText(minute)? minute : "0";

        Calendar calendar = GregorianCalendar.getInstance(Locale.getDefault());
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = null;
        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        calendar.setTime(date);
        calendar.add(Calendar.YEAR, Integer.parseInt(year));
        calendar.add(Calendar.MONTH, Integer.parseInt(month));
        calendar.add(Calendar.DATE, Integer.parseInt(day));
        calendar.add(Calendar.MINUTE, Integer.parseInt(minute));

        Date returnDate = calendar.getTime();
        String returnDateString = sdf.format(returnDate);

        return returnDateString;
    }

    /**
     * 处理更新数据库返回结果
     * @param daoResult
     * @return
     */
    public static Result handleDaoResult(int daoResult) {
        Result result = Result.getInstance();
        if(daoResult < 1) {
            result.setErrorCode("666666");
            result.setErrorMsg("提交失败");
        }
        return result;
    }

    /**
     * 含有{}字符串进行转换
     * @param content
     * @param object
     * @return
     */
    public static String changeParam(String content,Object... object) {

        for(Object oject : object) {
            if(oject instanceof  String) {
                content = content.replaceFirst("\\{\\}", String.valueOf(oject));
            }
        }
        System.out.println(content);
        return content;
    }

    /**
     * 当前日期分钟向上偏移
     * @param setNum
     * @param format
     * @return
     */
    public static String minuteSet(int setNum, String format){
        Calendar calendar = GregorianCalendar.getInstance(Locale.getDefault());
        int minute = calendar.get(Calendar.MINUTE);
        String minuteStr = String.valueOf(minute);
        int addMinute = setNum - Integer.parseInt(minuteStr.substring(minuteStr.length()-1));
        calendar.add(Calendar.MINUTE, addMinute);
        calendar.set(Calendar.SECOND ,0);
        Date returnDate = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String returnDateString = sdf.format(returnDate);
        return returnDateString;
    }

    /**
     * 分页参数处理
     * @param body
     * @return
     */
    public static Map<String, Object> pageParamHandle(Map<String, Object> body) {
        String pageNum = (String) body.get("pageNum");
        String pageSize = (String) body.get("pageSize");
        if(!StringUtils.hasText(pageNum) || !StringUtils.hasText(pageSize)) {
            pageNum = "1";
            pageSize = "10";
        }
        Map<String, Object> parambody = new HashMap<String, Object>();
        parambody.put("offset",(Integer.parseInt(pageNum)-1)*Integer.parseInt(pageSize));
        parambody.put("limit",Integer.parseInt(pageSize));
        return parambody;
    }

    /**
     * 获取客户端IP
     * @param request
     * @return
     */
    @SneakyThrows
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                ip = InetAddress.getLocalHost().getHostAddress();
            }
        }
        return ip;
    }

    public static String getClientIpByMDC() {
        return MDC.get("clientIp");
    }

    public static String getHostAddress() {
        String ip = null;

        try {

            if (null == ip) {
                ip = InetAddress.getByName(InetAddress.getLocalHost().getHostName()).getHostAddress();
            }
        } catch (Exception var2) {
            ;
        }

        if (null == ip) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    public static String getParamValue(String key) {
        String value = ProperConfig.getMapperValue(key);
        return StringUtils.hasText(value) ? value : "";
    }


    /**
     * json转xml
     * @param jsonStr
     * @return
     * @throws DocumentException
     */
    public static String json2xml(Object jsonStr) {
        net.sf.json.JSON json = net.sf.json.JSONSerializer.toJSON(jsonStr.toString());
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setTrimSpaces(false);
        xmlSerializer.setTypeHintsEnabled(false);
        xmlSerializer.setRootName("ROOT");
        String xml = xmlSerializer.write(json);
        xml = xml.replace("<o>", "").replace("</o>", "");
        xml = xml.replace("<e>", "").replace("</e>", "");
        xml = xml.replaceAll("\r\n", "");
        return xml;
    }

    public static int getStringLen(String str, String charsetName) {
        String anotherString = null;
        if (null != str && !"".equals(str)) {
            try {
                anotherString = new String(str.getBytes(charsetName), "ISO8859_1");
            } catch (UnsupportedEncodingException var4) {

            }

            return anotherString.length();
        } else {
            return 0;
        }
    }

    public static String handelErrorCode(String errorCode) {
        int ec = 0;
        try {
            if(errorCode != null && (errorCode.length() < 10 || !checkErrorCode(errorCode))){
                ec = Integer.parseInt(errorCode);
            }else{
                ec = Integer.parseInt(errorCode.substring(7));
            }
        } catch (Exception e) {
            ec = -1;
        }
        return String.valueOf(ec);
    }

    public static Boolean checkErrorCode(String errorCode) {
        Pattern p = Pattern.compile("^[A-Z]+$");
        if(null == errorCode){
            return false;
        }else if(errorCode.length() < 7){
            return false;
        }else{
            Matcher m =p.matcher(errorCode.substring(0,7));
            return m.matches();
        }
    }

    /**
     * 获取下载excel模板
     *
     * @param path
     * @return
     */
    public static Workbook getExcelTemplate(String path) {

        Workbook workBook = null;
        FileInputStream fis = null;
        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            workBook = new XSSFWorkbook(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return workBook;
    }

    /**
     * 使用zip进行解压缩
     * @param compressedStr 压缩后的文本
     * @return 返回解压后的字符串
     */
    public static String unzip(String compressedStr){
        if(compressedStr == null){
            return null;
        }
        ByteArrayOutputStream out = null;
        ByteArrayInputStream in = null;
        ZipInputStream zin = null;
        String decompressed = null;
        try {
            byte[] compressed = new sun.misc.BASE64Decoder().decodeBuffer(compressedStr);
            out = new ByteArrayOutputStream();
            in = new ByteArrayInputStream(compressed);
            zin = new ZipInputStream(in);
            zin.getNextEntry();
            byte[] buffer = new byte[1024];
            int offset = -1;
            while((offset = zin.read(buffer)) != -1){
                out.write(buffer,  0 ,offset);
            }
            decompressed = out.toString();
        } catch (Exception e) {
            decompressed = null;
        }finally{
            if(zin != null){
                try {
                    zin.close();
                } catch (IOException e) {
                }
            }
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if(out != null){
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return decompressed;
    }

    public static Map<String, String> extractAllAttributes(String tag) {
        Map<String, String> map = new HashMap<>();
        Pattern pattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(tag);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            map.put(key, value);
        }
        return map;
    }

    public static String timeConverter(String timeStr, String format, String tarFormatStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(timeStr, formatter);
        DateTimeFormatter targetFormtter = DateTimeFormatter.ofPattern(tarFormatStr);
        return offsetDateTime.format(targetFormtter);
    }
}
