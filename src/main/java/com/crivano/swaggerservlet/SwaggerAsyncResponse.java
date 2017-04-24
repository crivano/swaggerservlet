package com.crivano.swaggerservlet;

public class SwaggerAsyncResponse<T extends ISwaggerResponse> {
	private T resp;
	private SwaggerException exception;

	public SwaggerAsyncResponse(T resp) {
		this.resp = resp;
	}

	public T getResp() {
		return this.resp;
	}

	public SwaggerException getException() {
		return exception;
	}

	public void setException(SwaggerException exception) {
		this.exception = exception;
	}

}
