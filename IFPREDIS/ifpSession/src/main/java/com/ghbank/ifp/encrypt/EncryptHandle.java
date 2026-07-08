package com.ghbank.ifp.encrypt;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class EncryptHandle implements Encrypt {
	public static final String KEY = "LmMGStGtOpF4xNyvYt54EQ==";
	public static final Charset CHARSET = Charset.forName("UTF-8");
	private static final char[] DICTIONARY = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
	private static Object syncObj = new Object();
	private String computerAddr;
	private static long counter = 0L;

	private String getLocalHost() {
		if (this.computerAddr == null) {
			try {
				this.computerAddr = InetAddress.getLocalHost().getHostAddress();
			} catch (Exception var2) {
				this.computerAddr = "UNKNOW_IP";
			}
		}

		return this.computerAddr;
	}

	public String getEncryptKey() {
		char separator = '#';
		StringBuffer buf = new StringBuffer();
		buf.append(this.getLocalHost());
		buf.append(separator);
		buf.append(String.valueOf(0));
		buf.append(separator);
		Object var5 = syncObj;
		long l;
		synchronized (syncObj) {
			l = ++counter;
		}

		buf.append(Long.toHexString(l));
		buf.append(separator);
		buf.append(Long.toHexString(System.currentTimeMillis()));
		buf.append(separator);
		return buf.toString();
	}

	private String getEncryptKey(String keyValue) {
		char separator = '#';
		StringBuffer buf = new StringBuffer();
		buf.append(Long.toHexString(System.currentTimeMillis()));
		buf.append(separator);
		buf.append(keyValue);
		buf.append(separator);
		Random random = new Random();
		String result = "";

		for (int i = 0; i < 6; ++i) {
			result = result + random.nextInt(10);
		}

		buf.append(Integer.toHexString(Integer.parseInt(result)));
		buf.append(separator);
		return buf.toString();
	}

	public String getEncryptString(String encryptionAlgorithm, String keyValue) {
		String sessionKey = this.getEncryptKey();
		if (keyValue != null && !"".equals(keyValue)) {
			sessionKey = this.getEncryptKey(keyValue);
		}

		byte[] abyte0 = sessionKey.getBytes();

		try {
			MessageDigest messageDigest = MessageDigest.getInstance(encryptionAlgorithm);
			abyte0 = messageDigest.digest(abyte0);
			StringBuffer stringbuffer = new StringBuffer(40);

			for (int l = 0; l < abyte0.length; ++l) {
				int k = abyte0[l] + 128;
				int i = k / DICTIONARY.length;
				int j = k % DICTIONARY.length;
				stringbuffer.append(DICTIONARY[i]);
				stringbuffer.append(DICTIONARY[j]);
			}

			return stringbuffer.toString();
		} catch (Exception var11) {
			return null;
		}
	}

	public static String encrypt(String xmlStr) {
		byte[] encrypt = null;

		try {
			encrypt = xmlStr.getBytes("utf-8");
		} catch (UnsupportedEncodingException var12) {
			var12.printStackTrace();
		}

		byte[] md5Hasn = null;

		try {
			md5Hasn = md5Hash(encrypt, 0, encrypt.length);
		} catch (Exception var11) {
			var11.printStackTrace();
		}

		byte[] totalByte = addMD5(md5Hasn, encrypt);
		byte[] key = new byte[8];
		byte[] iv = new byte[8];
		getKeyIV("LmMGStGtOpF4xNyvYt54EQ==", key, iv);
		SecretKeySpec deskey = new SecretKeySpec(key, "DES");
		IvParameterSpec ivParam = new IvParameterSpec(iv);
		byte[] temp = null;

		try {
			temp = desCbcEncrypt(totalByte, deskey, ivParam);
		} catch (Exception var10) {
			var10.printStackTrace();
		}

		return (new BASE64Encoder()).encode(temp);
	}

	public static String decrypt(String xmlStr) throws Exception {
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] encBuf = null;

		try {
			encBuf = decoder.decodeBuffer(xmlStr);
		} catch (IOException var12) {
			var12.printStackTrace();
		}

		byte[] key = new byte[8];
		byte[] iv = new byte[8];
		getKeyIV("LmMGStGtOpF4xNyvYt54EQ==", key, iv);
		SecretKeySpec deskey = new SecretKeySpec(key, "DES");
		IvParameterSpec ivParam = new IvParameterSpec(iv);
		byte[] temp = null;

		try {
			temp = desCbcDecrypt(encBuf, deskey, ivParam);
		} catch (Exception var11) {
			var11.printStackTrace();
		}

		byte[] md5Hash = null;

		try {
			md5Hash = md5Hash(temp, 16, temp.length - 16);
		} catch (Exception var10) {
			var10.printStackTrace();
		}

		for (int i = 0; i < md5Hash.length; ++i) {
			if (md5Hash[i] != temp[i]) {
				throw new Exception("MD5校验错误。");
			}
		}

		return new String(temp, 16, temp.length - 16, "utf-8");
	}

	public static byte[] tripleDesCbcEncrypt(byte[] sourceBuf, SecretKeySpec deskey, IvParameterSpec ivParam)
			throws Exception {
		Cipher encrypt = Cipher.getInstance("TripleDES/CBC/PKCS5Padding");
		encrypt.init(1, deskey, ivParam);
		byte[] cipherByte = encrypt.doFinal(sourceBuf, 0, sourceBuf.length);
		return cipherByte;
	}

	public static byte[] tripleDesCbcDecrypt(byte[] sourceBuf, SecretKeySpec deskey, IvParameterSpec ivParam)
			throws Exception {
		Cipher decrypt = Cipher.getInstance("TripleDES/CBC/PKCS5Padding");
		decrypt.init(2, deskey, ivParam);
		byte[] cipherByte = decrypt.doFinal(sourceBuf, 0, sourceBuf.length);
		return cipherByte;
	}

	public static byte[] desCbcEncrypt(byte[] sourceBuf, SecretKeySpec deskey, IvParameterSpec ivParam)
			throws Exception {
		Cipher encrypt = Cipher.getInstance("DES/CBC/PKCS5Padding");
		encrypt.init(1, deskey, ivParam);
		byte[] cipherByte = encrypt.doFinal(sourceBuf, 0, sourceBuf.length);
		return cipherByte;
	}

	public static byte[] desCbcDecrypt(byte[] sourceBuf, SecretKeySpec deskey, IvParameterSpec ivParam)
			throws Exception {
		Cipher decrypt = Cipher.getInstance("DES/CBC/PKCS5Padding");
		decrypt.init(2, deskey, ivParam);
		byte[] cipherByte = decrypt.doFinal(sourceBuf, 0, sourceBuf.length);
		return cipherByte;
	}

	public static byte[] md5Hash(byte[] buf, int offset, int length) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(buf, offset, length);
		return md.digest();
	}

	public static String byte2hex(byte[] inStr) {
		StringBuffer out = new StringBuffer(inStr.length * 2);

		for (int n = 0; n < inStr.length; ++n) {
			String stmp = Integer.toHexString(inStr[n] & 255);
			if (stmp.length() == 1) {
				out.append("0" + stmp);
			} else {
				out.append(stmp);
			}
		}

		return out.toString();
	}

	public static byte[] addMD5(byte[] md5Byte, byte[] bodyByte) {
		int length = bodyByte.length + md5Byte.length;
		byte[] resutlByte = new byte[length];

		for (int i = 0; i < length; ++i) {
			if (i < md5Byte.length) {
				resutlByte[i] = md5Byte[i];
			} else {
				resutlByte[i] = bodyByte[i - md5Byte.length];
			}
		}

		return resutlByte;
	}

	public static void getKeyIV(String encryptKey, byte[] key, byte[] iv) {
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] buf = null;

		try {
			buf = decoder.decodeBuffer(encryptKey);
		} catch (IOException var6) {
			var6.printStackTrace();
		}

		int i;
		for (i = 0; i < key.length; ++i) {
			key[i] = buf[i];
		}

		for (i = 0; i < iv.length; ++i) {
			iv[i] = buf[i + 8];
		}

	}

	public String getEncryptStr(String enc) {
		byte[] keyBytes = "LmMGStGtOpF4xNyvYt54EQ==".getBytes(CHARSET);
		byte[] b = enc.getBytes(CHARSET);
		int i = 0;

		for (int size = b.length; i < size; ++i) {
			byte[] var6 = keyBytes;
			int var7 = keyBytes.length;

			for (int var8 = 0; var8 < var7; ++var8) {
				byte keyBytes0 = var6[var8];
				b[i] ^= keyBytes0;
			}
		}

		String s = new String(b);
		return s;
	}

	public String getDecodeStr(String dec) {
		byte[] keyBytes = "LmMGStGtOpF4xNyvYt54EQ==".getBytes(CHARSET);
		byte[] e = dec.getBytes(CHARSET);
		byte[] dee = e;
		int i = 0;

		for (int size = e.length; i < size; ++i) {
			byte[] var7 = keyBytes;
			int var8 = keyBytes.length;

			for (int var9 = 0; var9 < var8; ++var9) {
				byte keyBytes0 = var7[var9];
				e[i] = (byte) (dee[i] ^ keyBytes0);
			}
		}

		String k = new String(e);
		return k;
	}
}
