package com.crivano.swaggerservlet.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.crivano.swaggerservlet.SwaggerError;
import com.crivano.swaggerservlet.SwaggerServlet;

public class TestResponse extends SwaggerError {
	String category;
	String service;
	String version;
	String timestamp;
	String url;
	String responsable;
	Boolean partial;
	Boolean available;
	Boolean status;
	Boolean pass;
	Boolean skiped;
	long ms;
	List<TestResponse> dependencies;
	Map<String, String> properties;

	public TestResponse() {
	}

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

	public void addProperty(String name) {
		if (properties == null)
			properties = new TreeMap<>();
		properties.put(name, System.getProperty(name, "[undefined]"));
	}

	public void addPrivateProperty(String name) {
		if (properties == null)
			properties = new TreeMap<>();
		properties.put(name, System.getProperty(name) != null ? "[defined]" : "[undefined]");
	}
}
