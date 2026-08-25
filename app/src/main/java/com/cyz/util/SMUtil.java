package com.cyz.util;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 国密 SM2/SM4 工具类（基于 BouncyCastle）。
 *
 * <p>对外提供 6 个静态 API：
 * <ol>
 *   <li>{@link #sm2Encrypt(String, String)} 国密 SM2 加密</li>
 *   <li>{@link #sm2Decrypt(String, String)} 国密 SM2 解密</li>
 *   <li>{@link #sm4Encrypt(String, String)} 国密 SM4 加密</li>
 *   <li>{@link #sm4Decrypt(String, String)} 国密 SM4 解密</li>
 *   <li>{@link #generateSM2KeyPair()} 生成 SM2 密钥对</li>
 *   <li>{@link #generateSM4Key()} 生成 SM4 密钥</li>
 * </ol>
 *
 * <p><b>密钥格式约定：</b>
 * <ul>
 *   <li>SM2 私钥：64 位 HEX（不加 04 前缀，32 字节 d）；</li>
 *   <li>SM2 公钥：130 位 HEX（04 + X + Y，未压缩格式）；</li>
 *   <li>SM4 密钥：32 位 HEX（16 字节，128bit）；</li>
 *   <li>密文统一以 HEX 字符串返回/接收。</li>
 * </ul>
 *
 * <p>SM2 采用国标 GB/T 32918.4-2016 的 C1C3C2 模式；
 * SM4 采用 CBC + PKCS7Padding，随机 IV 随密文一起输出（IV 拼接在密文前）。
 */
public class SMUtil {

    /** SM2 曲线参数（sm2p256v1）。 */
    private static final X9ECParameters SM2_CURVE = GMNamedCurves.getByName("sm2p256v1");
    private static final ECDomainParameters SM2_DOMAIN = new ECDomainParameters(
            SM2_CURVE.getCurve(), SM2_CURVE.getG(), SM2_CURVE.getN(), SM2_CURVE.getH());

    /** SM4 分组长度 16 字节。 */
    private static final int SM4_BLOCK = 16;

    // ===================== 公开 API =====================

    /**
     * 生成 SM2 密钥对。
     *
     * @return 长度为 2 的数组：[0]=私钥 HEX（64 字符），[1]=公钥 HEX（130 字符，04+X+Y）
     */
    public static String[] generateSM2KeyPair() {
        ECKeyPairGenerator gen = new ECKeyPairGenerator();
        gen.init(new ECKeyGenerationParameters(SM2_DOMAIN, new SecureRandom()));
        AsymmetricCipherKeyPair kp = gen.generateKeyPair();

        ECPrivateKeyParameters priv = (ECPrivateKeyParameters) kp.getPrivate();
        ECPublicKeyParameters pub = (ECPublicKeyParameters) kp.getPublic();

        String privateKeyHex = toHex(leftPad(priv.getD().toByteArray(), 32));
        String publicKeyHex = toHex(pub.getQ().getEncoded(false)); // 04 + X + Y
        return new String[]{privateKeyHex, publicKeyHex};
    }

    /**
     * 生成 SM4 密钥（16 字节，32 位 HEX）。
     */
    public static String generateSM4Key() {
        byte[] key = new byte[SM4_BLOCK];
        new SecureRandom().nextBytes(key);
        return toHex(key);
    }

    /**
     * 国密 SM2 加密。
     *
     * @param publicKeyHex 公钥 HEX（130 字符，04+X+Y）
     * @param plainText   明文（UTF-8）
     * @return 密文 HEX（C1C3C2 模式，C1 为未压缩点 04 开头）
     */
    public static String sm2Encrypt(String publicKeyHex, String plainText) {
        try {
            ECPublicKeyParameters pubKey = toPublicKey(publicKeyHex);
            SM2Engine engine = new SM2Engine();
            engine.init(true, new ParametersWithRandom(pubKey, new SecureRandom()));
            byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] cipher = engine.processBlock(data, 0, data.length);
            return toHex(cipher);
        } catch (Exception e) {
            throw new RuntimeException("SM2 加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 国密 SM2 解密。
     *
     * @param privateKeyHex 私钥 HEX（64 字符）
     * @param cipherHex     密文 HEX（C1C3C2 模式）
     * @return 明文（UTF-8）
     */
    public static String sm2Decrypt(String privateKeyHex, String cipherHex) {
        try {
            ECPrivateKeyParameters privKey = toPrivateKey(privateKeyHex);
            SM2Engine engine = new SM2Engine();
            engine.init(false, privKey);
            byte[] cipher = fromHex(cipherHex);
            byte[] plain = engine.processBlock(cipher, 0, cipher.length);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM2 解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 国密 SM4 加密（CBC + PKCS7，随机 IV 拼接在密文前）。
     *
     * @param keyHex    16 字节密钥 HEX（32 字符）
     * @param plainText 明文（UTF-8）
     * @return 密文 HEX（IV(16) + 密文）
     */
    public static String sm4Encrypt(String keyHex, String plainText) {
        try {
            byte[] key = fromHex(keyHex);
            checkSM4Key(key);
            byte[] iv = new byte[SM4_BLOCK];
            new SecureRandom().nextBytes(iv);

            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                    new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
            cipher.init(true, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] out = new byte[cipher.getOutputSize(data.length)];
            int len = cipher.processBytes(data, 0, data.length, out, 0);
            len += cipher.doFinal(out, len);

            // 拼接 IV + 密文
            byte[] result = new byte[SM4_BLOCK + len];
            System.arraycopy(iv, 0, result, 0, SM4_BLOCK);
            System.arraycopy(out, 0, result, SM4_BLOCK, len);
            return toHex(result);
        } catch (Exception e) {
            throw new RuntimeException("SM4 加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 国密 SM4 解密（CBC + PKCS7，密文前 16 字节为 IV）。
     *
     * @param keyHex    16 字节密钥 HEX（32 字符）
     * @param cipherHex 密文 HEX（IV(16) + 密文）
     * @return 明文（UTF-8）
     */
    public static String sm4Decrypt(String keyHex, String cipherHex) {
        try {
            byte[] key = fromHex(keyHex);
            checkSM4Key(key);
            byte[] all = fromHex(cipherHex);
            if (all.length < SM4_BLOCK) {
                throw new IllegalArgumentException("密文长度不足，至少需要 IV(16 字节)。");
            }
            byte[] iv = Arrays.copyOfRange(all, 0, SM4_BLOCK);
            byte[] data = Arrays.copyOfRange(all, SM4_BLOCK, all.length);

            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                    new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
            cipher.init(false, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] out = new byte[cipher.getOutputSize(data.length)];
            int len = cipher.processBytes(data, 0, data.length, out, 0);
            len += cipher.doFinal(out, len);
            return new String(out, 0, len, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4 解密失败: " + e.getMessage(), e);
        }
    }

    // ===================== 内部辅助 =====================

    private static ECPublicKeyParameters toPublicKey(String publicKeyHex) {
        byte[] raw = fromHex(publicKeyHex);
        ECPoint q = SM2_CURVE.getCurve().decodePoint(raw);
        return new ECPublicKeyParameters(q, SM2_DOMAIN);
    }

    private static ECPrivateKeyParameters toPrivateKey(String privateKeyHex) {
        byte[] raw = fromHex(privateKeyHex);
        // 兼容前导零被裁剪或多余的情况
        byte[] d = leftPad(stripLeadingZero(raw), 32);
        return new ECPrivateKeyParameters(new java.math.BigInteger(1, d), SM2_DOMAIN);
    }

    private static void checkSM4Key(byte[] key) {
        if (key.length != SM4_BLOCK) {
            throw new IllegalArgumentException("SM4 密钥必须为 16 字节（32 位 HEX），实际 " + key.length + " 字节。");
        }
    }

    /** 去掉可能的符号位前导零。 */
    private static byte[] stripLeadingZero(byte[] in) {
        int i = 0;
        while (i < in.length - 1 && in[i] == 0) i++;
        if (i > 0) return Arrays.copyOfRange(in, i, in.length);
        return in;
    }

    /** 左补零到指定长度（不足则补；超长原样返回）。 */
    private static byte[] leftPad(byte[] in, int len) {
        if (in.length >= len) return in;
        byte[] out = new byte[len];
        System.arraycopy(in, 0, out, len - in.length, in.length);
        return out;
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        String s = hex.trim();
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException("HEX 串长度必须为偶数：" + s.length());
        }
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private SMUtil() {
    }

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("C:\\Users\\爱亲\\AppData\\Roaming\\Scooter Software\\Beyond Compare 4\\Helpers\\Java\\1.7z");
        String plainText = ZipUtils.sevenZToBase64(path);
        System.out.println("one:" + plainText);
        String cipherHex = sm2Encrypt("04102701902c6bab5509bb3fce90729e44954fab7e1e8bda640e505af52c7e2f37b114f63914ca76bd4b102a9ed43648a3d6d5f3c9c2e7cc688b66622038063f16", plainText);
        System.out.println("two:" + cipherHex);
        String oriStr = sm2Decrypt("00e47ddd812f1a6df1e6180f27c9f90761fc376eea7d9d24b5332219e8a5a37cd1", cipherHex);
        System.out.println("three:" + oriStr);
    }
}
