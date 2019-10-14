package com.crivano.swaggerservlet;

import java.util.List;

@SuppressWarnings("serial")
public class SwaggerAuthorizationException extends SwaggerDetailedException {

	public SwaggerAuthorizationException(String string) {
		super(string);
	}

	public SwaggerAuthorizationException() {
		super("unauthorized");
	}

	public SwaggerAuthorizationException(Throwable cause) {
		super(cause);
	}

	public SwaggerAuthorizationException(String string, List<SwaggerCallStatus> status) {
		super(string, status);
	}

	public SwaggerAuthorizationException(String string, Throwable cause, List<SwaggerCallStatus> status) {
		super(string, cause, status);
	}

}
