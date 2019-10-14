package com.crivano.swaggerservlet;

import java.util.List;

public class SwaggerException extends Exception {
	private static final long serialVersionUID = 1819120780452830899L;

	ISwaggerRequest req;
	ISwaggerResponse resp;
	String context;
	private int status;

	public SwaggerException(String error, int status, Throwable cause, ISwaggerRequest req, ISwaggerResponse resp,
			String contexts) {
		super(error, cause);
		this.req = req;
		this.resp = resp;
		this.context = context;
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
