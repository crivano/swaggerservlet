package com.crivano.restservlet;

import org.json.JSONObject;

public class RestAsyncResponse {
	private JSONObject json;

	public RestAsyncResponse(JSONObject json) {
		this.json = json;
	}

	public JSONObject getJSONObject() {
		return this.json;
	}
}
