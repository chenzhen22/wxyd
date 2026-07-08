package com.chenzhen.test;

import com.chenzhen.util.MailUtil;

public class MailTest {

    public static void main(String[] args) {


        MailUtil.send("马总", "部署sit2", "jiajun.wu@ghbank.com.cn", "Password123?", "mayilin@ghbank.com.cn");

    }
}
