package com.crivano.restservlet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public class RestUtils {
	private static final Logger log = LoggerFactory.getLogger(RestUtils.class);

	private static Map<String, String> properties = new HashMap<>();

	private static ExecutorService executor = Executors
			.newFixedThreadPool(new Integer(getProperty(
					"swaggerservlet.threadpool.size", "20")));

	private static IHTTP http = new DefaultHTTP();

	private static IMemCache memcache = new DefaultMemCache();

	public static void setHttp(IHTTP http) {
		RestUtils.http = http;
	}

	public static void setCache(IMemCache memcache) {
		RestUtils.memcache = memcache;
	}

	protected static JSONObject doHTTP(String authorization, String url,
			String method, String body) throws Exception {
		return http.fetch(authorization, url, method, body);
	}

	public static JSONObject getJsonReq(HttpServletRequest request,
			String context) {
		try {
			String sJson = RestUtils.getBody(request);
			JSONObject req = new JSONObject(sJson);
			if (context != null)
				log.debug(context + " req: " + req.toString(3));
			return req;
		} catch (Exception ex) {
			throw new RuntimeException("Cannot parse request body as JSON", ex);
		}
	}

	public static void writeJsonResp(HttpServletResponse response,
			JSONObject resp, String context, String service)
			throws JSONException, IOException {
		if (context != null)
			log.debug(context + " resp: " + resp.toString(3));

		String s = resp.toString(2);
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().println(s);
	}

	public static void writeJsonRespFromCache(HttpServletResponse response,
			String resp, String context, String service) throws JSONException,
			IOException {
		if (context != null)
			log.debug(context + " resp from cache: " + resp);

		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().println(resp);
	}

	private static String getBody(HttpServletRequest request)
			throws IOException {

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

	public static JSONObject restGet(String context, String authorization,
			String url, String... params) throws Exception {
		if (params != null) {
			StringBuilder sb = new StringBuilder(url);
			sb.append(url.contains("?") ? "&" : "?");

			for (int i = 0; i < params.length; i += 2) {
				if (params[i + 1] == null)
					continue;
				if (!"?".equals(sb.substring(sb.length())))
					sb.append("&");
				sb.append(params[i]);
				sb.append("=");
				sb.append(URLEncoder.encode(params[i + 1], "UTF-8"));
			}

			url = sb.toString();
		}

		if (context != null)
			log.debug(context + " url: " + url);

		JSONObject o = null;
		try {
			o = doHTTP(authorization, url, "GET", null);
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new RestException(errmsg, null, null, context);
		}

		if (context != null)
			log.debug(context + " resp: " + o.toString(3));

		propagateError(context, o);

		return o;
	}

	private static void propagateError(String context, JSONObject o)
			throws RestException {
		String error = o.optString("errormsg", null);
		if (error == null) // Keep compatibility with earlier versions
			error = o.optString("error", null);
		if (error != null)
			throw new RestException(error, null, o, context);
	}

	public static JSONObject restPost(String context, String authorization,
			String url, JSONObject req) throws Exception {
		if (context != null) {
			log.debug(context + " url: " + url + " req: " + req.toString(3));
		}

		JSONObject o = null;
		try {
			o = doHTTP(authorization, url.toString(), "POST", req.toString());
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new RestException(errmsg, req, null, context);
		}
		if (context != null)
			log.debug(context + " resp: " + o.toString(3));

		propagateError(context, o);

		return o;
	}

	public static JSONObject restPut(String context, String authorization,
			String url, JSONObject req) throws Exception {
		if (context != null) {
			log.debug(context + " url: " + url + " req: " + req.toString(3));
		}

		JSONObject o = null;
		try {
			o = doHTTP(authorization, url, "PUT", req.toString());
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new RestException(errmsg, req, null, context);
		}
		if (context != null)
			log.debug(context + " resp: " + o.toString(3));

		propagateError(context, o);

		return o;
	}

	public static Future<RestAsyncResponse> restGetAsync(String context,
			String authorization, String url, JSONObject req) throws Exception {
		if (context != null) {
			String u = url;
			StringBuilder sb = new StringBuilder(u);
			sb.append(u.contains("?") ? "&" : "?");

			Iterator<?> keys = req.keys();

			while (keys.hasNext()) {
				if (!"?".equals(sb.substring(sb.length())))
					sb.append("&");
				String key = (String) keys.next();
				if (req.get(key) instanceof String) {
					sb.append(key);
					sb.append("=");
					sb.append(URLEncoder.encode(req.getString(key), "UTF-8"));
				}
			}
			url = sb.toString();

			log.debug(context + " get url: " + url);
		}

		try {
			// Fire a request.
			return executor.submit(new RestAsyncRequest(authorization, url));
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new RestException(errmsg, req, null, context);
		}
	}

	public static void writeJsonError(HttpServletRequest request,
			HttpServletResponse response, final Exception e,
			JSONObject jsonreq, JSONObject jsonresp, String context,
			String service) {
		JSONObject json = new JSONObject();
		String errmsg = messageAsString(e);
		String errstack = stackAsString(e);
		boolean errpresentable = e instanceof IPresentableException;

		try {
			json.put("errormsg", errmsg);

			// Error Details
			JSONArray arr = new JSONArray();
			for (Throwable t = e; t != null && t != t.getCause(); t = t
					.getCause()) {
				if (t instanceof RestException) {
					RestException wse = (RestException) t;
					if (wse.jsonresp != null) {
						JSONArray arrsub = wse.jsonresp
								.optJSONArray("errordetails");
						if (arrsub != null)
							arr = arrsub;
						errpresentable = wse.jsonresp.optBoolean("presentable",
								errpresentable);
					}
					break;
				}
			}
			JSONObject detail = new JSONObject();
			detail.put("context", context);
			detail.put("service", service);
			detail.put("stacktrace", errstack);
			detail.put("presentable", errpresentable);
			detail.put("logged", true);
			arr.put(detail);
			json.put("errordetails", arr);

			response.setStatus(500);
			writeJsonResp(response, json, context, service);

			detail.put("url", request.getRequestURL());
			detail.put("req", jsonreq);
			detail.put("resp", jsonresp);
			log.error(json.toString(3));
		} catch (Exception e1) {
			throw new RuntimeException("Error building error message.", e1);
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
		// if (errstack != null) {
		// String split[] = errstack
		// .split("\r?\n?\tat com.crivano.swaggerservlet");
		// errstack = split[0] + (split.length > 2 ? "\r\n" + split[1] : "");
		// }
		return errstack;
	}

	public static String getProperty(String propertyName, String defaultValue) {
		if (properties.containsKey(propertyName))
			return properties.get(propertyName);
		String s = System.getProperty(propertyName);
		if (s != null)
			return s;
		s = System.getenv("PROP_"
				+ propertyName.replace(".", "_").toUpperCase());
		if (s != null)
			return s;
		return defaultValue;
	}

	public static void setProperty(String propertyName, String value) {
		properties.put(propertyName, value);
	}

	public static String cacheKeyJson(String context, JSONObject json) {
		String key = String.valueOf(json.toString().hashCode());
		return key;
	}

	public static void cacheStoreJson(String context, JSONObject jsonRequest,
			JSONObject jsonResponse) {
		String key = cacheKeyJson(context, jsonRequest);

		byte[] value;
		try {
			value = jsonResponse.toString().getBytes("UTF-8");
			memcache.store(key, value); // Populate cache.
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String cacheRetrieveJson(String context,
			JSONObject jsonRequest) {

		String key = cacheKeyJson(context, jsonRequest);

		byte[] ab = (byte[]) memcache.retrieve(key); // Read from cache.
		if (ab != null) {
			try {
				return new String(ab, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public static String base64Encode(byte[] bytes) {
		return Base64Coder.encodeLines(bytes);
	}

	public static byte[] base64Decode(String b64) {
		return Base64Coder.decodeLines(b64);
	}

	public static String convertStreamToString(java.io.InputStream is) {
		try (java.util.Scanner s = new java.util.Scanner(is)) {
			return s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
	}

	public static void memCacheStore(String key, byte[] ba) {
		memcache.store(key, ba);
	}

	public static byte[] memCacheRetrieve(String key) {
		return memcache.retrieve(key);
	}

	public static byte[] memCacheRemove(String key) {
		return memcache.remove(key);
	}

	public static String dbStore(String payload) {
		String id = UUID.randomUUID().toString();
		memCacheStore(id, payload.getBytes());
		return id;
	}

	public static String dbRetrieve(String id, boolean remove) {
		byte[] ba = null;
		if (remove)
			ba = memCacheRemove(id);
		else
			ba = memCacheRetrieve(id);
		if (ba == null)
			return null;
		String s = new String(ba);

		if (s == null || s.trim().length() == 0)
			return null;

		return s;
	}
}
