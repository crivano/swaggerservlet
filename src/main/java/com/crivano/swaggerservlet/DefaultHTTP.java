package com.crivano.swaggerservlet;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHTTP implements IHTTP {
	private static final Logger log = LoggerFactory
			.getLogger(DefaultHTTP.class);

	public static String convertStreamToString(java.io.InputStream is) {
		@SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is, "UTF-8")
				.useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	public static JSONObject convertStreamToObject(java.io.InputStream is) {
		JSONObject json;
		try {
			json = new JSONObject(convertStreamToString(is));
		} catch (JSONException e) {
			return null;
		}
		return json;
	}

	@Override
	public <T extends ISwaggerResponse> T fetch(String authorization,
			String url, String method, ISwaggerRequest req, Class<T> clazzResp)
			throws Exception {
		HttpURLConnection con = null;
		URL obj = new URL(url);
		con = (HttpURLConnection) obj.openConnection();

		if (authorization != null)
			con.setRequestProperty("Authorization", authorization);
		con.setRequestMethod(method);

		String body = null;

		if (req != null && ("POST".equals(method) || "PUT".equals(method))) {
			body = SwaggerUtils.toJson(req);

			con.setRequestProperty("Content-Type", "application/json");
			con.setConnectTimeout(5000); // set timeout to 5 seconds
			con.setReadTimeout(20000); // set read timeout to 20 seconds
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(body);
			wr.flush();
			wr.close();
		}

		int responseCode = con.getResponseCode();

		if (responseCode >= 400 && responseCode < 600) {
			SwaggerError err = (SwaggerError) SwaggerUtils.fromJson(
					convertStreamToString(con.getErrorStream()),
					SwaggerError.class);
			throw new SwaggerException(err.errormsg, req, err, null);
		}

		String respString = convertStreamToString(con.getInputStream());
		T resp = (T) SwaggerUtils.fromJson(respString, clazzResp);
		log.info("INT-HTTP: method:\"" + method.toUpperCase() + "\", path:\""
				+ url + "\", " + (body != null ? "\", request:" + body : "")
				+ ", response:" + respString);
		return resp;
	}

}
