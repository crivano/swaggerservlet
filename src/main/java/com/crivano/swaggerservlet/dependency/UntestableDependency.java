package com.crivano.swaggerservlet.dependency;

public abstract class UntestableDependency extends DependencySupport {

	public UntestableDependency(String category, String service, boolean partial) {
		super(category, service, partial);
	}

	@Override
	public boolean isTestable() {
		return false;
	}

}
