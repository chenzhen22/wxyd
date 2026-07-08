package com.ghbank.ifp.redis.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MurmurHash implements Hashing {
	public static int hash(byte[] data, int seed) {
		return hash(ByteBuffer.wrap(data), seed);
	}

	public static int hash(byte[] data, int offset, int length, int seed) {
		return hash(ByteBuffer.wrap(data, offset, length), seed);
	}

	public static int hash(ByteBuffer buf, int seed) {
		ByteOrder byteOrder = buf.order();
		buf.order(ByteOrder.LITTLE_ENDIAN);
		int m = 1540483477;
		int r = 24;
		int h = seed ^ buf.remaining();

		int k;
		for (byte i = 4; buf.remaining() >= i; h ^= k) {
			k = buf.getInt();
			k *= m;
			k ^= k >>> r;
			k *= m;
			h *= m;
		}

		if (buf.remaining() > 0) {
			ByteBuffer finish = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
			finish.put(buf).rewind();
			h ^= finish.getInt();
			h *= m;
		}

		h ^= h >>> 13;
		h *= m;
		h ^= h >>> 15;
		buf.order(byteOrder);
		return h;
	}

	public static long hash64A(byte[] data, int seed) {
		return hash64A(ByteBuffer.wrap(data), seed);
	}

	public static long hash64A(byte[] data, int offset, int length, int seed) {
		return hash64A(ByteBuffer.wrap(data, offset, length), seed);
	}

	public static long hash64A(ByteBuffer buf, int seed) {
		ByteOrder byteOrder = buf.order();
		buf.order(ByteOrder.LITTLE_ENDIAN);
		long m = -4132994306676758123L;
		int r = 47;
		long h = (long) seed ^ (long) buf.remaining() * m;

		for (byte i = 8; buf.remaining() >= i; h *= m) {
			long k = buf.getLong();
			k *= m;
			k ^= k >>> r;
			k *= m;
			h ^= k;
		}

		if (buf.remaining() > 0) {
			ByteBuffer finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
			finish.put(buf).rewind();
			h ^= finish.getLong();
			h *= m;
		}

		h ^= h >>> r;
		h *= m;
		h ^= h >>> r;
		buf.order(byteOrder);
		return h;
	}

	public long hash(byte[] key) {
		return hash64A(key, 305441741);
	}

	public long hash(String key) {
		return this.hash(SafeEncoder.encode(key));
	}
}
