package com.crivano.swaggerservlet.dependency;

public abstract class SwaggerServletDependency extends TestableDependency {

	public SwaggerServletDependency(String category, String service, boolean partial) {
		super(category, service, partial);
	}

	public SwaggerServletDependency() {
		super();
	}

	@Override
	public boolean test() {
		return false;
	}

}
