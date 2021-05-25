package com.crivano.swaggerservlet;

import java.io.IOException;

public class SwaggerApiContextSupport implements ISwaggerApiContext {

	private SwaggerContext ctx;

	@Override
	public void init(SwaggerContext ctx) {
		this.setCtx(ctx);
	}

	@Override
	public void onTryBegin() throws Exception {
	}

	@Override
	public void onTryEnd() throws Exception {
	}

	@Override
	public void onCatch(Exception e) throws Exception {
		throw e;
	}

	@Override
	public void onFinally() throws Exception {
	}

	@Override
	public void close() throws IOException {
	}

	public SwaggerContext getCtx() {
		return ctx;
	}

	public void setCtx(SwaggerContext ctx) {
		this.ctx = ctx;
	}

}
