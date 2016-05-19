package com.crivano.restservlet;

import org.json.JSONObject;

public interface RestAsyncCallback {
	void completed(JSONObject obj) throws Exception;

	void failed(Exception e) throws Exception;

	void cancelled() throws Exception;
}
