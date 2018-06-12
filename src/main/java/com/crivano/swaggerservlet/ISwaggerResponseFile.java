package com.crivano.swaggerservlet;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ISwaggerResponseFile {
	public String getContenttype();

	public void setContenttype(String contenttype);

	public String getContentdisposition();

	public void setContentdisposition(String contentdisposition);

	public Long getContentlength();

	public void setContentlength(Long contentlength);

	public InputStream getInputstream();

	public void setInputstream(InputStream inputstream);

	public Map<String, List<String>> getHeaderFields();

	public void setHeaderFields(Map<String, List<String>> headerFields);
}
