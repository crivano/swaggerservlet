package com.crivano.restservlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class RestUtils {
	private static final Logger log = Logger.getLogger(RestUtils.class
			.getName());

	private static final Map<String, byte[]> cache = new HashMap<String, byte[]>();

	public static JSONObject getJsonReq(HttpServletRequest request,
			String context) {
		try {
			String sJson = RestUtils.getBody(request);
			JSONObject req = new JSONObject(sJson);
			if (context != null)
				log.info(context + " req: " + req.toString(3));
			return req;
		} catch (Exception ex) {
			throw new RuntimeException("Cannot parse request body as JSON", ex);
		}
	}

	public static void writeJsonResp(HttpServletResponse response,
			JSONObject resp, String context, String service)
			throws JSONException, IOException {
		if (context != null)
			log.info(context + " resp: " + resp.toString(3));

		String s = resp.toString(2);
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().println(s);
	}

	public static String getBody(HttpServletRequest request) throws IOException {

		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(
						inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				stringBuilder.append("");
			}
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					throw ex;
				}
			}
		}

		body = stringBuilder.toString();
		return body;
	}

	public static JSONObject getJsonObjectFromURL(URL url, String context)
			throws Exception {
		if (context != null)
			log.info(context + " url: " + url);

		JSONObject o = null;
		try {
			final HttpResponse<JsonNode> jsonResponse = Unirest.get(
					url.toString()).asJson();

			o = jsonResponse.getBody().getObject();
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new RestException(errmsg, null, null, context);
		}

		if (context != null)
			log.info(context + " resp: " + o.toString(3));

		String error = o.optString("error", null);
		if (error != null)
			throw new Exception(error);

		return o;
	}

	public static JSONObject getJsonObjectFromJsonPost(URL url, JSONObject req,
			String context) throws Exception {
		if (context != null) {
			log.info(context + " url: " + url + " req: " + req.toString(3));
		}

		JSONObject o = null;
		try {
			final HttpResponse<JsonNode> jsonResponse = Unirest
					.post(url.toString()).body(new JsonNode(req.toString()))
					.asJson();
			o = jsonResponse.getBody().getObject();
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new RestException(errmsg, req, null, context);
		}
		if (context != null)
			log.info(context + " resp: " + o.toString(3));

		String error = o.optString("error", null);
		if (error != null)
			throw new RestException(error, req, o, context);

		return o;
	}

	public static Future getJsonObjectFromJsonPostAsync(URL url,
			JSONObject req, String context, final RestAsyncCallback callback)
			throws Exception {
		if (context != null) {
			log.info(context + " url: " + url + " req: " + req.toString(3));
		}

		try {
			return Unirest
					.post(url.toString())
					.body(new JsonNode(req.toString()))
					.asJsonAsync(
							new RestLoggingCallback(callback, req, context, log));
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new RestException(errmsg, req, null, context);
		}
	}

	public static void writeJsonError(HttpServletResponse response,
			final Exception e, String context, String service) {
		JSONObject json = new JSONObject();
		String errmsg = messageAsString(e);
		String errstack = stackAsString(e);

		try {
			json.put("error", errmsg);

			// Error Details
			JSONArray arr = new JSONArray();
			for (Throwable t = e; t != null && t != t.getCause(); t = t
					.getCause()) {
				if (t instanceof RestException) {
					RestException wse = (RestException) t;
					if (wse.jsonresp != null) {
						JSONObject resp = new JSONObject(wse.jsonresp);
						JSONArray arrsub = resp.optJSONArray("errordetails");
						if (arrsub != null)
							arr = arrsub;
					}
					break;
				}
			}
			JSONObject detail = new JSONObject();
			detail.put("context", context);
			detail.put("service", service);
			detail.put("stacktrace", errstack);
			arr.put(detail);
			json.put("errordetails", arr);

			response.setStatus(500);
			writeJsonResp(response, json, context, service);
		} catch (Exception e1) {
			throw new RuntimeException("Erro retornando mensagem de erro.", e1);
		}
	}

	public static String messageAsString(final Exception e) {
		String errmsg = e.getMessage();
		if (errmsg == null)
			if (e instanceof NullPointerException)
				errmsg = "null pointer.";
		return errmsg;
	}

	public static String stackAsString(final Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String errstack = sw.toString(); // stack trace as a string
		return errstack;
	}

	public static void store(String sha1, byte[] ba) {
		cache.put(sha1, ba);
	}

	public static byte[] retrieve(String sha1) {
		if (cache.containsKey(sha1)) {
			byte[] ba = cache.get(sha1);
			cache.remove(sha1);
			return ba;
		}
		return null;
	}

	public static String getProperty(String propertyName, String defaultValue) {
		String s = System.getProperty(propertyName);
		if (s != null)
			return s;
		s = System.getenv("PROP_"
				+ propertyName.replace(".", "_").toUpperCase());
		if (s != null)
			return s;
		return defaultValue;
	}
}