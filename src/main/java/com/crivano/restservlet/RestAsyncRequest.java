package com.crivano.restservlet;

import java.util.concurrent.Callable;

public class RestAsyncRequest implements Callable<RestAsyncResponse> {
	private String authorization;
	private String url;

	public RestAsyncRequest(String authorization, String url) {
		this.authorization = authorization;
		this.url = url;
	}

	@Override
	public RestAsyncResponse call() throws Exception {
		return new RestAsyncResponse(RestUtils.doHTTP(this.authorization,
				this.url, "GET", null));
	}
}
