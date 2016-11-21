package com.crivano.swaggerservlet;

public class SwaggerException extends Exception {
	private static final long serialVersionUID = 1819120780452830899L;

	ISwaggerRequest req;
	ISwaggerResponse resp;
	String context;

	public SwaggerException(String error, Throwable cause, ISwaggerRequest req, ISwaggerResponse resp, String context) {
		super(error, cause);
		this.req = req;
		this.resp = resp;
		this.context = context;
	}
}
