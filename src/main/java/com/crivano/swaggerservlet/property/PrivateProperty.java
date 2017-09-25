package com.crivano.swaggerservlet.property;

public class PrivateProperty extends PropertySupport {

	public PrivateProperty(String name) {
		super(name);
	}

	@Override
	public boolean isPrivate() {
		return true;
	}

	@Override
	public boolean isRestricted() {
		return false;
	}

	@Override
	public boolean isPublic() {
		return false;
	}
}
