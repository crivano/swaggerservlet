package com.crivano.swaggerservlet;

import junit.framework.TestCase;

public abstract class SwaggerTestSupport extends TestCase {
	private SwaggerServlet ss = null;

	protected abstract Class getAPI();

	protected abstract String getPackage();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ss = new SwaggerServlet();

		ss.setAPI(getAPI());
		ss.setActionPackage(getPackage());
	}

	public void run(String method, String pathInfo, ISwaggerRequest req, ISwaggerResponse resp) {
		try {
			ss.prepare(method, pathInfo);
			ss.run(req, resp);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setProperty(String name, String value) {
		SwaggerServlet.instance = ss;
		ss.addPublicProperty(name, value);
	}
}
