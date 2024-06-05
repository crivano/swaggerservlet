package com.crivano.swaggerservlet;

import java.util.List;

@SuppressWarnings("serial")
public class PresentableUnloggedException extends SwaggerDetailedException
		implements IPresentableException, IUnloggedException {

	public PresentableUnloggedException(String string) {
		super(string);
	}

	public PresentableUnloggedException(String string, int code) {
		super(string, code);
	}

	public PresentableUnloggedException(String string, List<SwaggerCallStatus> status) {
		super(string, status);
	}

	public PresentableUnloggedException(String string, Throwable cause) {
		super(string, cause);
	}

	public PresentableUnloggedException(String string, Throwable cause, List<SwaggerCallStatus> status) {
		super(string, cause, status);
	}
}
