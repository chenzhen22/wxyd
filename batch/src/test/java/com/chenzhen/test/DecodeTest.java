package com.chenzhen.test;

import cfca.yuzhi.vo.util.Base64;
import org.bouncycastle.util.encoders.Hex;

public class DecodeTest {

    public static void main(String[] args) {
        String encryptPin = "MCwwLDAsMCwwO3siZGF0YVR5cGUiOiJudW1iZXIiLCJuYW1lIjoicHdkMCIsInZhbHVlIjoiVGFsV29pS3A5bFJBR2F1c0NQU3BJdz09In0=";
        byte[] encryptPinBs =  Base64.decode(encryptPin);
        System.out.println(new String(encryptPinBs));
        String encryptPinHexString = Hex.toHexString(encryptPinBs).toUpperCase();
        System.out.println(encryptPinHexString);
    }
}
