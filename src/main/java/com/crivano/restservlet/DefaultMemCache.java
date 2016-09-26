package com.crivano.restservlet;

import java.util.HashMap;
import java.util.Map;

public class DefaultMemCache implements IMemCache {
	private static final Map<String, byte[]> cache = new HashMap<String, byte[]>();

	public void store(String key, byte[] ba) {
		cache.put(key, ba);
	}

	@Override
	public byte[] retrieve(String key) {
		return cache.get(key);
	}

	public byte[] remove(String key) {
		if (cache.containsKey(key)) {
			byte[] ba = cache.get(key);
			cache.remove(key);
			return ba;
		}
		return null;
	}
}
