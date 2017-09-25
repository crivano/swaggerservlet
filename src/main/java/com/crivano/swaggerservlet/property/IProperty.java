package com.crivano.swaggerservlet.property;

public interface IProperty {
	String getName();

	boolean isPrivate();

	boolean isRestricted();

	boolean isPublic();
}