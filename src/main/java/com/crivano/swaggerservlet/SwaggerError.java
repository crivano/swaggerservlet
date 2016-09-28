package com.crivano.swaggerservlet;

import java.util.List;

public class SwaggerError implements ISwaggerResponse {
	public class Detail {
		String context;
		String service;
		String stacktrace;
		boolean presentable;
		boolean logged;

		String url;
		ISwaggerRequest req;
		ISwaggerResponse resp;
	}

	public String errormsg;
	public List<Detail> errordetails;
}
