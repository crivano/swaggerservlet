package com.crivano.swaggerservlet.test;

import java.util.ArrayList;
import java.util.List;

import com.crivano.swaggerservlet.SwaggerError;

public class TestResponse extends SwaggerError {
	String category;
	String service;
	String url;
	String responsable;
	Boolean partial;
	Boolean available;
	Boolean status;
	Boolean pass;
	Boolean skiped;
	List<TestResponse> dependencies;

	public TestResponse(String category, String service, String url, String responsable, Boolean partial) {
		this.category = category;
		this.service = service;
		this.url = url;
		this.responsable = responsable;
		this.partial = partial;
	}

	public void addDependency(TestResponse tr) {
		if (dependencies == null)
			dependencies = new ArrayList<>();
		dependencies.add(tr);
	}
}
