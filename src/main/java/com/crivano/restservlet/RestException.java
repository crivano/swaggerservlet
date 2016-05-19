package com.crivano.restservlet;

import org.json.JSONObject;

public class RestException extends Exception {
	private static final long serialVersionUID = 1819120780452830899L;

	JSONObject jsonreq;
	JSONObject jsonresp;

	public RestException(String error, JSONObject jsonreq, JSONObject jsonresp,
			String context) {
		super(error);
		this.jsonreq = jsonreq;
		this.jsonresp = jsonresp;
	}
}