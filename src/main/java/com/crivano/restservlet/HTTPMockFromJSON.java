package com.crivano.restservlet;

import java.io.InputStream;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPMockFromJSON implements IHTTP {
	private static final Logger log = LoggerFactory
			.getLogger(HTTPMockFromJSON.class);

	JSONObject map = new JSONObject();

	public void add(String url, InputStream is) throws JSONException {
		String s = RestUtils.convertStreamToString(is);
		JSONObject o = new JSONObject(s);
		map.put(url, o);
	}

	@Override
	public JSONObject fetch(String authorization, String url, String method,
			String body) throws Exception {
		// Find the API
		Iterator<?> keys = map.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			if (!url.startsWith(key))
				continue;
			JSONObject mock = map.getJSONObject(key);
			String path = url.substring(key.length());

			// Find the method
			Iterator<?> mockkeys = mock.keys();
			while (mockkeys.hasNext()) {
				String mockkey = (String) mockkeys.next();
				JSONObject mockmethod = mock.getJSONObject(mockkey);
				if (!method.equals(mockmethod.getString("method"))
						|| !path.equals(mockmethod.getString("path")))
					continue;
				return mockmethod.getJSONObject("response");
			}
		}

		log.error("\"<description>\": method: \"" + method + "\", path: \""
				+ url + "\", request: " + body);
		throw new Exception("Can't find a mock for: " + url + " " + body);
	}
}
