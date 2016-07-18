package com.crivano.restservlet;

public class PresentableException extends Exception implements IPresentableException {

	public PresentableException(String string) {
		super(string);
	}

}
