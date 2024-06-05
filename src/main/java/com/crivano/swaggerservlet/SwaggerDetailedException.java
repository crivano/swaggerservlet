package com.crivano.swaggerservlet;

import java.util.List;

@SuppressWarnings("serial")
public class SwaggerDetailedException extends Exception {
	List<SwaggerCallStatus> status;
	int code = 500;

	public SwaggerDetailedException(String string) {
		super(string);
	}

	public SwaggerDetailedException(String string, int code) {
		super(string);
		this.code = code;
	}

	public SwaggerDetailedException(Throwable cause) {
		super(cause);
	}

	public SwaggerDetailedException(String string, Throwable cause) {
		super(string, cause);
	}

	public SwaggerDetailedException(String string, Throwable cause, List<SwaggerCallStatus> status) {
		super(string, cause);
		this.status = status;
	}

	public SwaggerDetailedException(String string, List<SwaggerCallStatus> status) {
		super(string);
		this.status = status;
	}
}
