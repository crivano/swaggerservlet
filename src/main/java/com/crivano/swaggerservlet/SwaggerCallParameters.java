package com.crivano.swaggerservlet;

public class SwaggerCallParameters {
	public SwaggerCallParameters(String context, String authorization, String method, String url, ISwaggerRequest req,
			Class clazz) {
		super();
		this.context = context;
		this.authorization = authorization;
		this.method = method;
		this.url = url;
		this.req = req;
		this.clazz = clazz;
	}

	String context;
	String authorization;
	String method;
	String url;
	ISwaggerRequest req;
//	Class<T extends ISwaggerResponse>  clazz;
	Class clazz;
}
