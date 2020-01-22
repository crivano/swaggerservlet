package com.crivano.swaggerservlet.dependency;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.crivano.swaggerservlet.ISwaggerResponseFile;
import com.crivano.swaggerservlet.SwaggerAsyncResponse;
import com.crivano.swaggerservlet.SwaggerCall;

public class SwaggerHttpGetDependency extends TestableDependency {
	String testsite;
	SwaggerAsyncResponse resp;

	public SwaggerHttpGetDependency(String category, String service, String testsite, boolean partial, long msMin,
			long msMax) {
		super(category, service, partial, msMin, msMax);
		this.testsite = testsite;
	}

	@Override
	public String getUrl() {
		return testsite;
	}

	@Override
	public boolean test() throws Exception {
		Future<SwaggerAsyncResponse<AnyResponse>> future = SwaggerCall.callAsync("test", null, "GET", testsite, null,
				AnyResponse.class);
		SwaggerAsyncResponse<AnyResponse> futureresponse = future.get(getMsMax(), TimeUnit.MILLISECONDS);
		this.resp = futureresponse;
		return true;
	}

	private static class AnyResponse implements ISwaggerResponseFile {

		@Override
		public String getContenttype() {
			return null;
		}

		@Override
		public void setContenttype(String contenttype) {
		}

		@Override
		public String getContentdisposition() {
			return null;
		}

		@Override
		public void setContentdisposition(String contentdisposition) {

		}

		@Override
		public Long getContentlength() {
			return null;
		}

		@Override
		public void setContentlength(Long contentlength) {
		}

		@Override
		public InputStream getInputstream() {
			return null;
		}

		@Override
		public void setInputstream(InputStream inputstream) {
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			// TODO Auto-generated method stub
			return new HashMap<>();
		}

		@Override
		public void setHeaderFields(Map<String, List<String>> headerFields) {
			// TODO Auto-generated method stub

		}
	}
}
