package com.ghbank.ifp.redis.access;


import com.ghbank.ifp.redis.util.MurmurHash;
import com.ghbank.ifp.util.StringUtils;

public class RedisKey {
	private String prefix;
	private String key;
	private String field;
	private long perfixHashValue;
	private long redisKeyHashValue;

	public RedisKey(String prefix, String key) {
		this.prefix = prefix;
		this.key = key;
	}

	public RedisKey(String prefix, String key, String field) {
		this(prefix, key);
		this.field = field;
	}

	public String getRedisKey() {
		return StringUtils.hasText(this.prefix) ? this.prefix + "_" + this.key : this.key;
	}

	public long getPrefixHashValue() {
		if (this.perfixHashValue == 0L) {
			this.perfixHashValue = MurmurHash.MURMUR_HASH.hash(this.prefix);
			if (this.perfixHashValue < 0L) {
				this.perfixHashValue = -this.perfixHashValue;
			}
		}

		return this.perfixHashValue;
	}

	public long getRedisKeyHashValue() {
		if (this.redisKeyHashValue == 0L) {
			this.redisKeyHashValue = MurmurHash.MURMUR_HASH.hash(this.getRedisKey());
			if (this.redisKeyHashValue < 0L) {
				this.redisKeyHashValue = -this.redisKeyHashValue;
			}
		}

		return this.redisKeyHashValue;
	}

	public int getDBIndex(int maxDBCount) {
		if (maxDBCount == 0) {
			maxDBCount = 15;
		}

		return (int) (this.getPrefixHashValue() % (long) maxDBCount);
	}

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getField() {
		return this.field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public long getPerfixHashValue() {
		return this.perfixHashValue;
	}

	public void setPerfixHashValue(long perfixHashValue) {
		this.perfixHashValue = perfixHashValue;
	}

	public void setRedisKeyHashValue(long redisKeyHashValue) {
		this.redisKeyHashValue = redisKeyHashValue;
	}

	public String toString() {
		return "[prefix:" + this.prefix + ",key:" + this.key + "]";
	}
}
