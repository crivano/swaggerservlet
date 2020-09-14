package com.crivano.swaggerservlet.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.crivano.swaggerservlet.Property;
import com.crivano.swaggerservlet.SwaggerError;

public class TestResponse extends SwaggerError {
	String category;
	String service;
	String version;
	String timestamp;
	String url;
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

	public TestResponse(String category, String service, String url, Boolean partial) {
		this.category = category;
		this.service = service;
		this.url = url;
		this.partial = partial;
	}

	public void addDependency(TestResponse tr) {
		if (dependencies == null)
			dependencies = new ArrayList<>();
		dependencies.add(tr);
	}

	public void addProperty(String name, Property property) {
		if (properties == null)
			properties = new TreeMap<>();
		properties.put(name,
				System.getProperty(name,
						property != null && property.isOptional() ? "[default: " + property.getDefaultValue() + "]"
								: "[undefined]"));
	}

	public void addPrivateProperty(String name) {
		if (properties == null)
			properties = new TreeMap<>();
		properties.put(name, System.getProperty(name) != null ? "[defined]" : "[undefined]");
	}
}
