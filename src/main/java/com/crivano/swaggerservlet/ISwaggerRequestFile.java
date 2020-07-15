package com.crivano.swaggerservlet;

import java.util.List;
import java.util.Map;

public interface ISwaggerRequestFile extends ISwaggerResponse {
	public String getFilename();

	public void setFilename(String filename);

	public String getContenttype();

	public void setContenttype(String contenttype);

	public Object getContent();

	public void setContent(Object content);

	public Map<String, List<String>> getHeaderFields();

	public void setHeaderFields(Map<String, List<String>> headerFields);
}
