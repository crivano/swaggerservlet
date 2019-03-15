package com.crivano.swaggerservlet;

public class Property {
	public static enum Scope {
		PRIVATE, RESTRICTED, PUBLIC
	}

	private String name;
	private String defaultValue;
	private boolean optional;
	private Scope scope;

	public Property() {
	}

	public Property(Scope scope, String name, boolean optional, String defaultValue) {
		this.scope = scope;
		this.name = name;
		this.defaultValue = defaultValue;
		this.optional = optional;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

}
