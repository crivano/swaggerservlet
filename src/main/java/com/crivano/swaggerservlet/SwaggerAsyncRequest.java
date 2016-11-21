package com.crivano.swaggerservlet;

import java.util.concurrent.Callable;

public class SwaggerAsyncRequest<T extends ISwaggerResponse> implements Callable<SwaggerAsyncResponse<T>> {
	private String context;
	private String authorization;
	private String url;
	private String method;
	private ISwaggerRequest req;
	private Class<? extends ISwaggerResponse> respClass;

	public SwaggerAsyncRequest(String context, String authorization, String method, String url, ISwaggerRequest req,
			Class<? extends ISwaggerResponse> clazz) {
		this.context = context;
		this.authorization = authorization;
		this.method = method;
		this.url = url;
		this.req = req;
		this.respClass = (Class<? extends ISwaggerResponse>) clazz;
	}

	@Override
	public SwaggerAsyncResponse<T> call() throws Exception {
		try {
			return new SwaggerAsyncResponse(SwaggerCall.call(this.context, this.authorization, this.method, this.url,
					this.req, this.respClass));
		} catch (Exception ex) {
			SwaggerAsyncResponse<T> ar = new SwaggerAsyncResponse<T>(null);
			String errmsg = SwaggerUtils.messageAsString(ex);
			String errstack = SwaggerUtils.stackAsString(ex);
			ar.setException(new SwaggerException(errmsg, ex, req, null, context));
			return ar;
		}
	}
}