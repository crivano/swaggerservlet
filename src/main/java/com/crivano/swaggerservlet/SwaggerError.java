package com.crivano.swaggerservlet;

import java.io.Serializable;
import java.util.List;

public class SwaggerError implements ISwaggerResponse, Serializable {
	private static final long serialVersionUID = -6440680646287033407L;

	public static class Detail {
		String context;
		String service;
		String stacktrace;
		boolean presentable;
		boolean logged;

		String user;
		String url;
	}

	public String errormsg;
	public List<Detail> errordetails;
	public List<SwaggerCallStatus> errorstatus;
}
