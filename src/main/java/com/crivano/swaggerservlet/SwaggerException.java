package com.crivano.swaggerservlet;


public class SwaggerException extends Exception {
	private static final long serialVersionUID = 1819120780452830899L;

	ISwaggerRequest req;
	ISwaggerResponse resp;

	public SwaggerException(String error, ISwaggerRequest req,
			ISwaggerResponse resp, String context) {
		super(error);
		this.req = req;
		this.resp = resp;
	}
}
