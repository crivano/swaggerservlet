package com.crivano.swaggerservlet;

public class SwaggerAuthorizationException extends Exception {

	public SwaggerAuthorizationException(String string) {
		super(string);
	}

	public SwaggerAuthorizationException() {
		super("unauthorized");
	}

	public SwaggerAuthorizationException(Throwable cause) {
		super(cause);
	}

}
