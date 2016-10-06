package com.crivano.swaggerservlet;

public interface IHTTP {

	<T extends ISwaggerResponse> T fetch(String authorization, String url,
			String method, ISwaggerRequest req, Class<T> clazzResp)
			throws Exception;
}
