package com.chenzhen.test;

import java.io.File;

//import weblogic.security.internal.SerializedSystemIni;
//import weblogic.security.internal.encryption.ClearOrEncryptedService;

public class WeblogicCracker {

    public static void main(String[] args) {
        String secretDirectory = "D:\\GIT\\wxyd\\client\\src\\test\\resources\\FIRS_UAT1";
        String password = "{AES256}P6Tsz1O4uRtI2hpkFErF5JvPp5efF3hNK6KXJXn0U/8=";
        //ClearOrEncryptedService ces = new ClearOrEncryptedService(SerializedSystemIni.getEncryptionService(new File(secretDirectory).getAbsolutePath()));
        //String pwd = ces.decrypt(password);
        //System.out.println(pwd);
    }
}
