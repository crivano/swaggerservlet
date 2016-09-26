package com.crivano.restservlet;

public interface IMemCache {
	public void store(String key, byte[] ba);

	public byte[] retrieve(String key);

	public byte[] remove(String key);
}
