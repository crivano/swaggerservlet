package com.crivano.swaggerservlet;

import java.util.List;

@SuppressWarnings("serial")
public class PresentableException extends SwaggerDetailedException implements IPresentableException {

	public PresentableException(String string) {
		super(string);
	}

	public PresentableException(String string, Throwable cause) {
		super(string, cause);
	}

	public PresentableException(String string, List<SwaggerCallStatus> status) {
		super(string, status);
	}

	public PresentableException(String string, Throwable cause, List<SwaggerCallStatus> status) {
		super(string, cause, status);
	}
}
