package com.crivano.swaggerservlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	private static IMemCache memcache = new DefaultMemCache();

	private static IUploadHandler uploadHandler = null;

	private static Map<Class, Logger> mapLogger = new HashMap<>();

	public static void setCache(IMemCache memcache) {
		SwaggerUtils.memcache = memcache;
	}

	public static void setUploadHandler(IUploadHandler streamHandler) {
		SwaggerUtils.uploadHandler = streamHandler;
	}

	public static Object upload(String fileName, String contentType, InputStream stream) {
		if (uploadHandler == null)
			throw new RuntimeException("no stream handler defined");
		return uploadHandler.upload(fileName, contentType, stream);
	}
	
	public static IDateAdapter dateAdapter = new DefaultDateAdapter();

	public static void setDateAdapter(IDateAdapter dateAdapter) {
		SwaggerUtils.dateAdapter = dateAdapter;
	}

	public static Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(InputStream.class, new InputStreamTypeAdapter())
			.registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
			.registerTypeHierarchyAdapter(Date.class, new DateToStringTypeAdapter()).setPrettyPrinting().create();

	public static class InputStreamTypeAdapter implements JsonSerializer<InputStream>, JsonDeserializer<InputStream> {
		public InputStream deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return null;
		}

		public JsonElement serialize(InputStream src, Type typeOfSrc, JsonSerializationContext context) {
			return null;
		}
	}

	public static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
		public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return base64Decode(json.getAsString());
		}

		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(base64Encode(src));
		}
	}

	public static class DateToStringTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return dateAdapter.parse(json.getAsString());
		}

		public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(dateAdapter.format(src));
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

	public static String getBody(HttpServletRequest request) throws IOException {

		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
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

	public static ISwaggerRequest getJsonReq(HttpServletRequest request, String context,
			Class<? extends ISwaggerRequest> clazz) {
		try {
			String sJson = getBody(request);
			ISwaggerRequest req = (ISwaggerRequest) fromJson(sJson, clazz);
			return req;
		} catch (Exception ex) {
			throw new RuntimeException("Cannot parse request body as JSON", ex);
		}
	}

	public static <T extends ISwaggerModel> T fromJson(String sJson, Class<T> clazz) {
		return gson.fromJson(sJson, clazz);
	}

	public static void writeJsonResp(HttpServletResponse response, ISwaggerResponse resp, String context,
			String service) throws Exception {
		String sJson = toJson(resp);
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().println(sJson);
	}

	public static String toJson(ISwaggerModel resp) {
		return gson.toJson(resp);
	}

	public static SwaggerError writeJsonError(int status, String errorcode, HttpServletRequest request, HttpServletResponse response,
			final Exception e, ISwaggerRequest req, ISwaggerResponse resp, String context, String service, String user,
			List<SwaggerCallStatus> errorstatus) {
		SwaggerError error = new SwaggerError();

		try {
			buildSwaggerError(request, e, context, service, user, error, errorstatus);
			error.errorcode = errorcode;
			response.setStatus(status);
			writeJsonResp(response, error, context, service);
			return error;
		} catch (Exception e1) {
			throw new RuntimeException("Error building error message.", e1);
		}
	}

	public static void buildSwaggerError(HttpServletRequest request, final Exception e, String context, String service,
			String user, SwaggerError error, List<SwaggerCallStatus> errorstatus) {
		String errmsg = messageAsString(e);
		String errstack = stackAsString(e);
		boolean errpresentable = e instanceof IPresentableException;
		error.errormsg = errmsg;

		// Error Details
		for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
			if (t instanceof SwaggerException) {
				SwaggerException wse = (SwaggerException) t;
				if (wse.resp != null && wse.resp instanceof SwaggerError) {
					SwaggerError previousError = (SwaggerError) wse.resp;
					error.errordetails = previousError.errordetails;
				}
				break;
			}
		}

		if (error.errordetails != null && error.errordetails.size() > 0) {
			if (!errpresentable)
				errpresentable = error.errordetails.get(0).presentable;
		} else {
			error.errordetails = new ArrayList<>();
		}

		SwaggerError.Detail detail = new SwaggerError.Detail();
		detail.context = context;
		detail.service = service;
		detail.stacktrace = errstack;
		detail.presentable = errpresentable;
		detail.logged = e == null || !(e instanceof IUnloggedException);
		// if (request.getRequestURI() != null)
		detail.user = user;
		if (request != null)
			detail.url = request.getRequestURI();
		error.errordetails.add(detail);

		error.errorstatus = errorstatus;
	}

	public static String messageAsString(final Exception e) {
		String errmsg = e.getMessage();
		if (errmsg == null)
			if (e instanceof NullPointerException)
				errmsg = "null pointer.";
			else
				errmsg = e.getClass().getSimpleName();
		return errmsg;
	}

	public static String stackAsString(final Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String errstack = sw.toString(); // stack trace as a string
		return errstack;
	}

	public static String simplifyStackTrace(Throwable t, String[] pkgs) {
		if (t == null)
			return null;
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);
		t.printStackTrace(pw);
		String s = sw.toString();
		if (true) {
			StringBuilder sb = new StringBuilder();
			String[] lines = s.split(System.getProperty("line.separator"));
			for (int i = 0; i < lines.length; i++) {
				String l = lines[i];
				boolean isInPackages = false;
				if (pkgs != null) {
					for (String pkg : pkgs) {
						isInPackages |= l.contains(pkg);
					}
				}
				if (!l.startsWith("\t") || (isInPackages && !l.contains(".invoke(") && !l.contains(".doFilter("))) {
					sb.append(l);
					sb.append(System.getProperty("line.separator"));
				}
			}
			s = sb.toString();
		}
		return s;
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
		if (is == null)
			return null;
		try (java.util.Scanner s = new java.util.Scanner(is, "UTF-8")) {
			return s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
	}

	public static void transferContent(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		in.close();
	}

	public static Logger log(Class clazz) {
		if (!mapLogger.containsKey(clazz))
			mapLogger.put(clazz, LoggerFactory.getLogger(clazz));
		return mapLogger.get(clazz);
	}
}
