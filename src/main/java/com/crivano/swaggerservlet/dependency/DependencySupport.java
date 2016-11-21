package com.crivano.swaggerservlet.dependency;

public abstract class DependencySupport implements IDependency {
	private String service;
	private String category;
	private boolean partial;

	public DependencySupport() {
	}

	public DependencySupport(String category, String service, boolean partial) {
		this.setCategory(category);
		this.setService(service);
		this.setPartial(partial);
	}

	@Override
	public String getService() {
		return service;
	}

	@Override
	public void setService(String service) {
		this.service = service;
	}

	@Override
	public String getCategory() {
		return category;
	}

	@Override
	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public String getResponsable() {
		return null;
	}

	public boolean isPartial() {
		return partial;
	}

	public void setPartial(boolean partial) {
		this.partial = partial;
	}

}
