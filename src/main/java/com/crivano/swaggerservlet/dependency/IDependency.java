package com.crivano.swaggerservlet.dependency;

public interface IDependency {

	String getService();

	void setService(String service);

	String getUrl();

	String getCategory();

	void setCategory(String category);

	String getResponsable();

	boolean isTestable();
	
	boolean isPartial();
}