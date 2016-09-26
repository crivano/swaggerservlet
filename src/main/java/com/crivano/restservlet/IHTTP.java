package com.crivano.restservlet;

import org.json.JSONObject;

public interface IHTTP {
	JSONObject fetch(String authorization, String url,
			String method, String body) throws Exception;
}
