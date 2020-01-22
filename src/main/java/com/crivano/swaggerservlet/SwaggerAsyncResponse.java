package com.crivano.swaggerservlet;

public class SwaggerAsyncResponse<T extends ISwaggerResponse> {
	private T resp;
	private SwaggerException exception;
	private Long miliseconds;

	public SwaggerAsyncResponse(T resp) {
		this.resp = resp;
	}

	public T getResp() {
		return this.resp;
	}

	public T getRespOrThrowException() throws SwaggerException {
		if (getException() != null)
			throw getException();
		return this.resp;
	}

	public SwaggerException getException() {
		return exception;
	}

	public void setException(SwaggerException exception) {
		this.exception = exception;
	}

	public Long getMiliseconds() {
		return miliseconds;
	}

	public void setMiliseconds(Long miliseconds) {
		this.miliseconds = miliseconds;
	}

}
