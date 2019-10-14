package com.crivano.swaggerservlet;

import java.util.List;

public class SwaggerError implements ISwaggerResponse {
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
