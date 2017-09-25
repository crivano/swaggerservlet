package com.crivano.swaggerservlet.property;

public class PublicProperty extends PropertySupport {

	public PublicProperty(String name) {
		super(name);
	}

	@Override
	public boolean isPrivate() {
		return false;
	}

	@Override
	public boolean isRestricted() {
		return false;
	}

	@Override
	public boolean isPublic() {
		return true;
	}
}
