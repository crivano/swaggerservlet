package com.crivano.swaggerservlet.dependency;

public abstract class DependencySupport implements IDependency {
	private String service;
	private String category;
	private boolean partial;
	private long msMin;
	private long msMax;

	public DependencySupport() {
	}

	public DependencySupport(String category, String service, boolean partial, long msMin, long msMax) {
		this.setCategory(category);
		this.setService(service);
		this.setPartial(partial);
		this.setMsMin(msMin);
		this.setMsMax(msMax);
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

	public long getMsMin() {
		return msMin;
	}

	public void setMsMin(long msMin) {
		this.msMin = msMin;
	}

	public long getMsMax() {
		return msMax;
	}

	public void setMsMax(long msMax) {
		this.msMax = msMax;
	}

}
