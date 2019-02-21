package com.crivano.swaggerservlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwaggerMultipleCallResult {

	public static class ListStatus {
		public String system;
		public String errormsg;
		public String stacktrace;
	}

	public Map<String, ISwaggerResponse> responses = new HashMap<>();
	public List<ListStatus> status = new ArrayList<>();
}