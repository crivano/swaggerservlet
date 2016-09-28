package com.crivano.swaggerservlet;

public interface ISwaggerMethod {
	public String getContext();

	public void run(ISwaggerRequest req, ISwaggerResponse resp);
}
