package com.ghbank.ifp.redis.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface Hashing {
	Hashing MURMUR_HASH = new MurmurHash();
	ThreadLocal<MessageDigest> MD5_HOLDER = new ThreadLocal();
	public static final Hashing MD5 = new Hashing() {
	    @Override
	    public long hash(String key) {
	      return hash(SafeEncoder.encode(key));
	    }

	    @Override
	    public long hash(byte[] key) {
	      try {
	        if (MD5_HOLDER.get() == null) {
	        	MD5_HOLDER.set(MessageDigest.getInstance("MD5"));
	        }
	      } catch (NoSuchAlgorithmException e) {
	        throw new IllegalStateException("++++ no md5 algorythm found");
	      }
	      MessageDigest md5 = MD5_HOLDER.get();

	      md5.reset();
	      md5.update(key);
	      byte[] bKey = md5.digest();
	      long res = ((long) (bKey[3] & 0xFF) << 24) | ((long) (bKey[2] & 0xFF) << 16)
	          | ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
	      return res;
	    }
	  };

	long hash(String var1);

	long hash(byte[] var1);
}
