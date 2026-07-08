package com.chenzhen.test;

import sun.misc.BASE64Decoder;

import java.io.*;
import java.util.Base64;

public class PdfTest {


    public static void main(String[] args) throws Exception {
        File file = new File("D:\\华兴IM\\企网开发内部群\\叶训劲(40773)\\base.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String msg = null;
        while((msg = br.readLine()) != null) {
            msg = msg.replace("data:application/pdf;base64,", "");
            System.out.println(msg);
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] decodeByte = decoder.decodeBuffer(msg);;
            OutputStream os = new FileOutputStream("C:\\Users\\chenzhen.GHBANK\\Desktop\\11.pdf");
            os.write(decodeByte);
            os.flush();

        }

    }
}
