package com.ghbank.ifp.redis.util;

import java.io.UnsupportedEncodingException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

public class SafeEncoder {
	public static byte[][] encodeMany(String... strs) {
		byte[][] many = new byte[strs.length][];

		for (int i = 0; i < strs.length; ++i) {
			many[i] = encode(strs[i]);
		}

		return many;
	}

	public static byte[] encode(String str) {
		try {
			if (str == null) {
				throw new JedisDataException("value sent to redis cannot be null");
			} else {
				return str.getBytes("UTF-8");
			}
		} catch (UnsupportedEncodingException var2) {
			throw new JedisException(var2);
		}
	}

	public static String encode(byte[] data) {
		try {
			return new String(data, "UTF-8");
		} catch (UnsupportedEncodingException var2) {
			throw new JedisException(var2);
		}
	}
}
