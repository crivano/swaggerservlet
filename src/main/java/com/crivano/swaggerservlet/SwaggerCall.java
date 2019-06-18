package com.crivano.swaggerservlet;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.swaggerservlet.SwaggerMultipleCallResult.ListStatus;
import com.crivano.swaggerservlet.test.TestResponse;

public class SwaggerCall {
	public static final String SWAGGERSERVLET_THREADPOOL_SIZE_PROPERTY_NAME = "swaggerservlet.threadpool.size";
	public static final String SWAGGERSERVLET_THREADPOOL_SIZE_DEFAULT_VALUE = "20";

	private static final Logger log = LoggerFactory.getLogger(SwaggerCall.class);

	private static ExecutorService executor = Executors
			.newFixedThreadPool(new Integer(SwaggerServlet.getProperty(SWAGGERSERVLET_THREADPOOL_SIZE_PROPERTY_NAME)));

	private static IHTTP http = new DefaultHTTP();

	public static void setHttp(IHTTP http) {
		SwaggerCall.http = http;
	}

	public static void setExecutor(ExecutorService executor) {
		SwaggerCall.executor = executor;
	}

	public static <T extends ISwaggerResponse> T call(String context, String authorization, String method, String url,
			ISwaggerRequest req, Class<T> clazz) throws Exception {
		T resp = null;
		LogResponse lr = new LogResponse();
		lr.method = method;
		lr.path = url;
		try {
			try {
				resp = doHTTP(authorization, url.toString(), method, req, clazz);
			} catch (SwaggerException ex) {
				ex.context = context;
				throw ex;
			} catch (Exception ex) {
				String errmsg = SwaggerUtils.messageAsString(ex);
				String errstack = SwaggerUtils.stackAsString(ex);
				throw new SwaggerException(errmsg, 500, ex, req, null, context);
			}

			if (!(resp instanceof TestResponse) && resp instanceof SwaggerError) {
				SwaggerError err = (SwaggerError) resp;
				throw new SwaggerException(err.errormsg, 500, null, req, resp, context);
			}
			return resp;
		} finally {
			lr.request = req;
			lr.response = resp;
			log.debug("HTTP-CALL: " + SwaggerUtils.toJson(lr));
		}
	}

	public static <T extends ISwaggerResponse> T doHTTP(String authorization, String url, String method,
			ISwaggerRequest req, Class<T> respClass) throws Exception {

		if ("GET".equals(method)) {
			StringBuilder sb = new StringBuilder(url);

			if (req != null) {
				boolean first = true;
				for (Field f : req.getClass().getDeclaredFields()) {
					f.setAccessible(true);
					Object v = f.get(req);
					if (v == null)
						continue;
					if (v instanceof Date)
						v = SwaggerUtils.format((Date) v);
					sb.append(first && !url.contains("?") ? "?" : "&");
					first = false;
					sb.append(f.getName());
					sb.append("=");
					sb.append(URLEncoder.encode(v.toString(), "UTF-8"));
				}
			}
			url = sb.toString();
		}

		return (T) http.fetch(authorization, url, method, req, respClass);
	}

	public static <T extends ISwaggerResponse> Future<SwaggerAsyncResponse<T>> callAsync(String context,
			String authorization, String method, String url, ISwaggerRequest req, Class<T> clazz) throws Exception {
		try {
			// Fire a request.
			return executor.submit(new SwaggerAsyncRequest(context, authorization, method, url, req, clazz));
		} catch (Exception ex) {
			String errmsg = SwaggerUtils.messageAsString(ex);
			String errstack = SwaggerUtils.stackAsString(ex);
			throw new SwaggerException(errmsg, 500, ex, req, null, context);
		}
	}

	public static SwaggerMultipleCallResult callMultiple(Map<String, SwaggerCallParameters> mapp,
			long timeoutMilliseconds) throws Exception {
		SwaggerMultipleCallResult r = new SwaggerMultipleCallResult();

		Map<String, Future<SwaggerAsyncResponse<ISwaggerResponse>>> map = new HashMap<>();
		final CountDownLatch responseWaiter = new CountDownLatch(mapp.size());

		Date dt1 = new Date();

		for (String system : mapp.keySet()) {
			SwaggerCallParameters scp = mapp.get(system);
			map.put(system, callAsync(scp.context, scp.authorization, scp.method, scp.url, scp.req, scp.clazz));
		}

		for (String system : mapp.keySet()) {
			try {
				long timeout = timeoutMilliseconds - ((new Date()).getTime() - dt1.getTime());
				if (timeout < 0L)
					timeout = 0;
				SwaggerAsyncResponse futureresponse = map.get(system).get(timeout, TimeUnit.MILLISECONDS);
				ListStatus ls = new ListStatus();
				ls.system = system;
				r.status.add(ls);
				SwaggerException ex = futureresponse.getException();
				if (ex != null) {
					log.error("Erro obtendo a usuário de {}", system, ex);
					ls.errormsg = SwaggerUtils.messageAsString(ex);
					ls.stacktrace = SwaggerUtils.stackAsString(ex);
				}
				ISwaggerResponse o = futureresponse.getRespOrThrowException();
				if (o != null)
					r.responses.put(system, o);
			} catch (Exception ex) {
				ListStatus ls = new ListStatus();
				ls.system = system;
				ls.errormsg = SwaggerUtils.messageAsString(ex);
				if (ls.errormsg == null)
					ls.errormsg = ex.getClass().getName();
				ls.stacktrace = SwaggerUtils.stackAsString(ex);
				r.status.add(ls);
				if (ex instanceof TimeoutException)
					map.get(system).cancel(true);
			}
		}
		return r;
	}

}
