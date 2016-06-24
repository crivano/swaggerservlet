package com.crivano.swaggerservlet;

import junit.framework.TestCase;

import org.json.JSONObject;

public abstract class SwaggerTestSupport extends TestCase {
	private SwaggerServlet ss = null;

	protected abstract String getPackage();

	protected String getSwaggerFilePathName() {
		return "/swagger.yaml";
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		buildTestServlet(getPackage(), getSwaggerFilePathName());
	}

	@SuppressWarnings("serial")
	public void buildTestServlet(String packag, String file) {
		ss = new SwaggerServlet() {
			@Override
			protected String getContext() {
				return "test";
			}
		};
		Swagger sv = null;
		sv = new Swagger();
		sv.loadFromInputStream(this.getClass().getResourceAsStream(file));

		ss.setSwagger(sv);
		ss.setActionPackage(packag);
	}

	public JSONObject run(String method, String pathInfo, JSONObject req) {
		try {
			JSONObject resp = new JSONObject();
			ss.prepare(method, pathInfo, req);
			ss.run(req, resp);
			return resp;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
