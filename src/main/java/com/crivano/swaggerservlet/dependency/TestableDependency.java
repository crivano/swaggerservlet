package com.crivano.swaggerservlet.dependency;

public abstract class TestableDependency extends DependencySupport {
	public TestableDependency() {
		super();
	}

	public TestableDependency(String category, String service, boolean partial, long msMin, long msMax) {
		super(category, service, partial, msMin, msMax);
	}

	public abstract boolean test() throws Exception;

	@Override
	public boolean isTestable() {
		return true;
	}

}
