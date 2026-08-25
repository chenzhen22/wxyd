package com.cyz.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * ZIP 与 Base64 互转工具类。
 *
 * <p>提供两个核心能力：
 * <ol>
 *   <li>{@link #zipToBase64} —— 把一个 .zip 文件的原始字节读出来，编码成 Base64 字符串；</li>
 *   <li>{@link #base64ToZip} —— 把 Base64 字符串解码回 ZIP 字节，并可写回 .zip 文件。</li>
 * </ol>
 *
 * <p>本质上是「zip 文件字节 ⇄ Base64 文本」的整体编解码，
 * 不对 zip 内部条目做解压/再压缩，因此往返一次得到的 zip 与原文件逐字节一致。
 *
 * <p>仅依赖 JDK（{@code java.util.zip} + {@code java.util.Base64}），Java 8 即可使用。
 */
public class ZipUtils {

    private ZipUtils() {
    }

    /**
     * 把 ZIP 文件转成 Base64 字符串。
     *
     * @param zipPath zip 文件路径，不能为 {@code null}
     * @return 该 zip 文件内容的 Base64 编码（标准 Base64，不带换行）
     * @throws IOException 读取文件失败时抛出
     */
    public static String zipToBase64(Path zipPath) throws IOException {
        byte[] zipBytes = Files.readAllBytes(zipPath);
        return Base64.getEncoder().encodeToString(zipBytes);
    }

    /**
     * 把 ZIP 字节数组转成 Base64 字符串。
     *
     * @param zipBytes zip 文件内容字节，不能为 {@code null}
     * @return Base64 编码字符串
     */
    public static String zipToBase64(byte[] zipBytes) {
        return Base64.getEncoder().encodeToString(zipBytes);
    }

    /**
     * 把 Base64 字符串解码回 ZIP 字节数组。
     *
     * <p>注意：返回的是 zip 文件本身的字节，不是 zip 内部条目解压后的内容。
     *
     * @param base64 Base64 编码的 zip 内容
     * @return zip 文件字节
     */
    public static byte[] base64ToZip(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * 把 Base64 字符串解码后写回 .zip 文件。
     *
     * @param base64       Base64 编码的 zip 内容
     * @param outputZipPath 输出 zip 文件路径，不能已存在目录
     * @return 写出的字节数
     * @throws IOException 写文件失败时抛出
     */
    public static int base64ToZipFile(String base64, Path outputZipPath) throws IOException {
        byte[] zipBytes = Base64.getDecoder().decode(base64);
        Files.write(outputZipPath, zipBytes);
        return zipBytes.length;
    }

    /**
     * 简单校验给定字节数组是否是一个 ZIP 文件（按本地文件头魔数 {@code PK\x03\x04} 判定）。
     *
     * <p>仅做头部魔数检查，不解压、不枚举条目，因此对包含加密条目的 zip 也能正确识别为合法。
     * 空压缩包（{@code PK\x05\x06}）与分卷/跨度包（{@code PK\x07\x08}）同样视为合法。
     *
     * @param zipBytes 待校验字节
     * @return {@code true} 表示头部是 ZIP 魔数
     */
    public static boolean isValidZip(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length < 4) {
            return false;
        }
        int b0 = zipBytes[0] & 0xFF;
        int b1 = zipBytes[1] & 0xFF;
        int b2 = zipBytes[2] & 0xFF;
        int b3 = zipBytes[3] & 0xFF;
        // 必须以 "PK" 开头
        if (b0 != 0x50 || b1 != 0x4B) {
            return false;
        }
        // 本地文件头 / 空包 EOCD / 跨度/分卷
        return (b2 == 0x03 && b3 == 0x04)
                || (b2 == 0x05 && b3 == 0x06)
                || (b2 == 0x07 && b3 == 0x08);
    }

    // ============================ 7z 与 Base64 互转 ============================
    //
    // 7z 与 zip 一样是二进制归档，整体字节 ⇄ Base64 的编解码逻辑完全相同，
    // 唯一区别是合法性校验的魔数头（7z 签名：6 字节 37 7A BC AF 27 1C）。

    /**
     * 把 7z 文件转成 Base64 字符串。
     *
     * @param sevenZPath 7z 文件路径，不能为 {@code null}
     * @return 该 7z 文件内容的 Base64 编码（标准 Base64，不带换行）
     * @throws IOException 读取文件失败时抛出
     */
    public static String sevenZToBase64(Path sevenZPath) throws IOException {
        byte[] bytes = Files.readAllBytes(sevenZPath);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 把 7z 字节数组转成 Base64 字符串。
     *
     * @param sevenZBytes 7z 文件内容字节，不能为 {@code null}
     * @return Base64 编码字符串
     */
    public static String sevenZToBase64(byte[] sevenZBytes) {
        return Base64.getEncoder().encodeToString(sevenZBytes);
    }

    /**
     * 把 Base64 字符串解码回 7z 字节数组。
     *
     * <p>注意：返回的是 7z 文件本身的字节，不是 7z 内部条目解压后的内容。
     *
     * @param base64 Base64 编码的 7z 内容
     * @return 7z 文件字节
     */
    public static byte[] base64ToSevenZ(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * 把 Base64 字符串解码后写回 .7z 文件。
     *
     * @param base64          Base64 编码的 7z 内容
     * @param outputSevenZPath 输出 7z 文件路径
     * @return 写出的字节数
     * @throws IOException 写文件失败时抛出
     */
    public static int base64ToSevenZFile(String base64, Path outputSevenZPath) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        Files.write(outputSevenZPath, bytes);
        return bytes.length;
    }

    /**
     * 简单校验给定字节数组是否是一个 7z 文件（按签名魔数 {@code 37 7A BC AF 27 1C} 判定）。
     *
     * @param sevenZBytes 待校验字节
     * @return {@code true} 表示头部是 7z 魔数
     */
    public static boolean isValidSevenZ(byte[] sevenZBytes) {
        if (sevenZBytes == null || sevenZBytes.length < 6) {
            return false;
        }
        return (sevenZBytes[0] & 0xFF) == 0x37  // '7'
                && (sevenZBytes[1] & 0xFF) == 0x7A  // 'z'
                && (sevenZBytes[2] & 0xFF) == 0xBC
                && (sevenZBytes[3] & 0xFF) == 0xAF
                && (sevenZBytes[4] & 0xFF) == 0x27
                && (sevenZBytes[5] & 0xFF) == 0x1C;
    }
}
