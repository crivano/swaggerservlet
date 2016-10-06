package com.crivano.swaggerservlet;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwaggerCall {
	private static final Logger log = LoggerFactory
			.getLogger(SwaggerCall.class);

	private static ExecutorService executor = Executors
			.newFixedThreadPool(new Integer(SwaggerUtils.getProperty(
					"swaggerservlet.threadpool.size", "20")));

	private static IHTTP http = new DefaultHTTP();

	public static void setHttp(IHTTP http) {
		SwaggerCall.http = http;
	}

	public static <T extends ISwaggerResponse> T call(String context,
			String authorization, String method, String url,
			ISwaggerRequest req, Class<T> clazz) throws Exception {

		if (context != null) {
			log.debug(context + " url: " + url + " req: "
					+ SwaggerUtils.toJson(req));
		}

		T resp = null;
		try {
			resp = doHTTP(authorization, url.toString(), method, req, clazz);
		} catch (SwaggerException ex) {
			ex.context = context;
			throw ex;
		} catch (Exception ex) {
			String errmsg = SwaggerUtils.messageAsString(ex);
			String errstack = SwaggerUtils.stackAsString(ex);
			throw new SwaggerException(errmsg, req, null, context);
		}

		if (context != null)
			log.debug(context + " resp: " + SwaggerUtils.toJson(resp));

		if (resp instanceof SwaggerError) {
			SwaggerError err = (SwaggerError) resp;
			throw new SwaggerException(err.errormsg, req, resp, context);
		}
		return resp;
	}

	public static <T extends ISwaggerResponse> T doHTTP(String authorization,
			String url, String method, ISwaggerRequest req, Class<T> respClass)
			throws Exception {

		if ("GET".equals(method)) {
			StringBuilder sb = new StringBuilder(url);

			boolean first = true;
			for (Field f : req.getClass().getDeclaredFields()) {
				f.setAccessible(true);
				Object v = f.get(req);
				if (v == null)
					continue;
				sb.append(first && !url.contains("?") ? "?" : "&");
				first = false;
				sb.append(f.getName());
				sb.append("=");
				sb.append(URLEncoder.encode(v.toString(), "UTF-8"));
			}
			url = sb.toString();
		}

		return (T) http.fetch(authorization, url, method, req, respClass);
	}

	public static <T extends ISwaggerResponse> Future<SwaggerAsyncResponse<T>> callAsync(
			String context, String authorization, String method, String url,
			ISwaggerRequest req, Class<T> clazz) throws Exception {
		try {
			// Fire a request.
			return executor.submit(new SwaggerAsyncRequest(context,
					authorization, method, url, req, clazz));
		} catch (Exception ex) {
			String errmsg = SwaggerUtils.messageAsString(ex);
			String errstack = SwaggerUtils.stackAsString(ex);
			throw new SwaggerException(errmsg, req, null, context);
		}
	}

}
