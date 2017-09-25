package com.crivano.swaggerservlet.property;

public class RestrictedProperty extends PropertySupport {

	public RestrictedProperty(String name) {
		super(name);
	}

	@Override
	public boolean isPrivate() {
		return false;
	}

	@Override
	public boolean isRestricted() {
		return true;
	}

	@Override
	public boolean isPublic() {
		return false;
	}

}
