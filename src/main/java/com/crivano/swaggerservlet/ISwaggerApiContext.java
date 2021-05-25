package com.crivano.swaggerservlet;

import java.io.Closeable;

public interface ISwaggerApiContext extends Closeable {

	void init(SwaggerContext ctx);

	void onTryBegin() throws Exception;

	void onTryEnd() throws Exception;

	void onCatch(Exception e) throws Exception;

	void onFinally() throws Exception;

}
