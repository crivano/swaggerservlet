package com.crivano.swaggerservlet.property;

public abstract class PropertySupport implements IProperty {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PropertySupport() {
	}

	public PropertySupport(String name) {
		this.setName(name);
	}

}
