package com.crivano.swaggerservlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SwaggerUtils {
	private static final Logger log = LoggerFactory
			.getLogger(SwaggerUtils.class);

	private static Map<String, String> properties = new HashMap<>();

	private static IMemCache memcache = new DefaultMemCache();

	public static void setCache(IMemCache memcache) {
		SwaggerUtils.memcache = memcache;
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

	public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final SimpleDateFormat isoFormatter = new SimpleDateFormat(
			ISO_FORMAT);

	public static final Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(byte[].class,
					new ByteArrayToBase64TypeAdapter())
			.registerTypeHierarchyAdapter(Date.class,
					new DateToStringTypeAdapter()).create();

	private static class ByteArrayToBase64TypeAdapter implements
			JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
		public byte[] deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			return base64Decode(json.getAsString());
		}

		public JsonElement serialize(byte[] src, Type typeOfSrc,
				JsonSerializationContext context) {
			return new JsonPrimitive(base64Encode(src));
		}
	}

	private static class DateToStringTypeAdapter implements
			JsonSerializer<Date>, JsonDeserializer<Date> {
		public Date deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			return parse(json.getAsString());
		}

		public JsonElement serialize(Date src, Type typeOfSrc,
				JsonSerializationContext context) {
			return new JsonPrimitive(format(src));
		}
	}

	public static String base64Encode(byte[] bytes) {
		if (bytes == null)
			return null;
		return Base64Coder.encodeLines(bytes, 0, bytes.length, 4000, "");
	}

	public static byte[] base64Decode(String b64) {
		if (b64 == null)
			return null;
		return Base64Coder.decodeLines(b64);
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

	public static ISwaggerRequest getJsonReq(HttpServletRequest request,
			String context, Class<? extends ISwaggerRequest> clazz) {
		try {
			String sJson = getBody(request);
			ISwaggerRequest req = (ISwaggerRequest) fromJson(sJson, clazz);
			if (context != null)
				log.debug(context + " req: " + sJson);
			return req;
		} catch (Exception ex) {
			throw new RuntimeException("Cannot parse request body as JSON", ex);
		}
	}

	public static <T extends ISwaggerModel> T fromJson(String sJson,
			Class<T> clazz) {
		return gson.fromJson(sJson, clazz);
	}

	public static void writeJsonResp(HttpServletResponse response,
			ISwaggerResponse resp, String context, String service)
			throws JSONException, IOException {
		String sJson = toJson(resp);
		if (context != null)
			log.debug(context + " resp: " + sJson);

		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().println(sJson);
	}

	public static String toJson(ISwaggerModel resp) {
		return gson.toJson(resp);
	}

	public static void writeJsonError(HttpServletRequest request,
			HttpServletResponse response, final Exception e,
			ISwaggerRequest req, ISwaggerResponse resp, String context,
			String service) {
		JSONObject json = new JSONObject();
		String errmsg = messageAsString(e);
		String errstack = stackAsString(e);
		boolean errpresentable = e instanceof IPresentableException;

		SwaggerError error = new SwaggerError();

		try {
			error.errormsg = errmsg;

			// Error Details
			JSONArray arr = new JSONArray();
			for (Throwable t = e; t != null && t != t.getCause(); t = t
					.getCause()) {
				if (t instanceof SwaggerException) {
					SwaggerException wse = (SwaggerException) t;
					if (wse.resp != null && wse.resp instanceof SwaggerError) {
						SwaggerError previousError = (SwaggerError) wse.resp;
						error.errordetails = previousError.errordetails;
					}
					break;
				}
			}

			if (error.errordetails != null) {
				errpresentable = error.errordetails.get(0).presentable;
			} else {
				error.errordetails = new ArrayList<>();
			}

			SwaggerError.Detail detail = error.new Detail();
			detail.context = context;
			detail.service = service;
			detail.stacktrace = errstack;
			detail.presentable = errpresentable;
			detail.logged = true;
			error.errordetails.add(detail);

			response.setStatus(500);
			writeJsonResp(response, error, context, service);

			try {
				detail.url = request.getRequestURL().toString();
			} catch (Exception ex) {

			}
			detail.req = req;
			detail.resp = resp;
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

	public static String format(Date date) {
		return isoFormatter.format(date);
	}

	public static Date parse(String date) {
		try {
			return isoFormatter.parse(date);
		} catch (ParseException e) {
			return null;
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

	public static String convertStreamToString(java.io.InputStream is) {
		try (java.util.Scanner s = new java.util.Scanner(is)) {
			return s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
	}

}
