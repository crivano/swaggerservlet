package com.crivano.swaggerservlet.dependency;

public abstract class SwaggerServletDependency extends TestableDependency {

	public SwaggerServletDependency(String category, String service, boolean partial, long msMin, long msMax) {
		super(category, service, partial, msMin, msMax);
	}

	public SwaggerServletDependency() {
		super();
	}

	@Override
	public boolean test() {
		return false;
	}

}
