package com.crivano.swaggerservlet;

public class LogResponse implements ISwaggerModel {
	public String method;
	public String path;
	public ISwaggerRequest request;
	public ISwaggerResponse response;
}
