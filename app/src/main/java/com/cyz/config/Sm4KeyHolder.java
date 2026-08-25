package com.cyz.config;

import com.cyz.util.SMUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Sm4KeyHolder {

    @Value("${wxyd.sm4.key}")
    private String sm4Key;

    public String encrypt(String plain) {
        return SMUtil.sm4Encrypt(sm4Key, plain);
    }

    public String decrypt(String cipherHex) {
        return SMUtil.sm4Decrypt(sm4Key, cipherHex);
    }
}
