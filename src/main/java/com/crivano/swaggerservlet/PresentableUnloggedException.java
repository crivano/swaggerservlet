package com.crivano.swaggerservlet;

public class PresentableUnloggedException extends Exception implements IPresentableException, IUnloggedException {

	public PresentableUnloggedException(String string) {
		super(string);
	}

	public PresentableUnloggedException(String string, Throwable cause) {
		super(string, cause);
	}

}
