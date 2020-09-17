package com.crivano.swaggerservlet.test;

import java.util.concurrent.Callable;

import com.crivano.swaggerservlet.SwaggerUtils;
import com.crivano.swaggerservlet.dependency.TestableDependency;

public class TestAsync implements Callable<TestResponse> {
	private TestableDependency dep;
	private TestResponse r;

	public TestAsync(TestableDependency dep, TestResponse r) {
		this.dep = dep;
		this.r = r;
	}

	@Override
	public TestResponse call() throws Exception {
		long time = System.currentTimeMillis();
		try {
			this.r.available = this.dep.test();
			this.r.ms = System.currentTimeMillis() - time;
			return r;
		} catch (Exception ex) {
			SwaggerUtils.buildSwaggerError(null, ex, "test", r.category, r.service, r, null);
			this.r.ms = System.currentTimeMillis() - time;
			return r;
		}
	}
}