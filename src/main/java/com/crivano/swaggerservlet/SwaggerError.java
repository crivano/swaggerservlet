package com.crivano.swaggerservlet;

import java.io.Serializable;
import java.util.List;

public class SwaggerError implements ISwaggerResponse, Serializable {
	private static final long serialVersionUID = -6440680646287033407L;

	public static class Detail {
		public String context;
		public String service;
		public String stacktrace;
		public boolean presentable;
		public boolean logged;

		public String user;
		public String url;
	}

	public String errormsg;
	public List<Detail> errordetails;
	public List<SwaggerCallStatus> errorstatus;
}
