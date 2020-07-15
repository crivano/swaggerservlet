package com.crivano.swaggerservlet;

import java.io.InputStream;

public interface IUploadHandler {
	public Object upload(String filename, String contenttype, InputStream stream);
}
