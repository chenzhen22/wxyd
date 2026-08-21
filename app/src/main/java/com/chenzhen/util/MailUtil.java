package com.chenzhen.util;

import com.chenzhen.config.ProperConfig;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class MailUtil<main> {

    public static void send(String title , String content) {
        Properties mailProp = new Properties();
        javax.mail.Session session = javax.mail.Session.getInstance(mailProp);
        Transport ts = null;
        try {
            mailProp.setProperty("mail.host", ProperConfig.getMapperValue("mailIp"));
            mailProp.setProperty("mail.transport.protocol", "smtp");
            mailProp.setProperty("mail.smtp.auth", "true");
            ts = session.getTransport();
            String secretKey = ProperConfig.getMapperValue("passworkKey");
            String mailPasswd = ProperConfig.getMapperValue("mailPasswd");
            mailPasswd = DesUtil.decode(secretKey, mailPasswd);
            ts.connect(ProperConfig.getMapperValue("mailIp"), ProperConfig.getMapperValue("mailUser"),
                    mailPasswd);
            //4、创建邮件
            Message message = new MimeMessage(session);;
            //发件人
            message.setFrom(new InternetAddress(ProperConfig.getMapperValue("mailSendUser")));
            //收件人
            message.setRecipients(Message.RecipientType.TO, new InternetAddress().parse(ProperConfig.getMapperValue("mailRevUsers")));
            //邮件标题
            message.setSubject(title);
            //正文
            MimeBodyPart text = new MimeBodyPart();
            text.setContent(content, "text/html;charset=UTF-8");
            MimeMultipart mp = new MimeMultipart();
            mp.addBodyPart(text);
            message.setContent(mp);
            //5、发送邮件
            ts.sendMessage(message, message.getAllRecipients());
            ts.close();
        } catch (Exception e) {
            System.out.println(e);
        }finally{
            if(ts!=null){
                try {
                    ts.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void send(String title , String content, String username, String password, String revicenames) {
        Properties mailProp = new Properties();
        javax.mail.Session session = javax.mail.Session.getInstance(mailProp);
        Transport ts = null;
        try {
            mailProp.setProperty("mail.host", ProperConfig.getMapperValue("mailIp"));
            mailProp.setProperty("mail.transport.protocol", "smtp");
            mailProp.setProperty("mail.smtp.auth", "true");
            ts = session.getTransport();
            String mailPasswd =password;
            ts.connect(ProperConfig.getMapperValue("mailIp"), ProperConfig.getMapperValue("mailUser"),
                    mailPasswd);
            //4、创建邮件
            Message message = new MimeMessage(session);;
            //发件人
            message.setFrom(new InternetAddress(username));
            //收件人
            message.setRecipients(Message.RecipientType.TO, new InternetAddress().parse(revicenames));
            //邮件标题
            message.setSubject(title);
            //正文
            MimeBodyPart text = new MimeBodyPart();
            text.setContent(content, "text/html;charset=UTF-8");
            MimeMultipart mp = new MimeMultipart();
            mp.addBodyPart(text);
            message.setContent(mp);
            //5、发送邮件
            ts.sendMessage(message, message.getAllRecipients());
            ts.close();
        } catch (Exception e) {
            System.out.println(e);
        }finally{
            if(ts!=null){
                try {
                    ts.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
