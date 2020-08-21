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

import com.crivano.swaggerservlet.test.TestResponse;

public class SwaggerCall {
	private static final Logger log = LoggerFactory.getLogger(SwaggerCall.class);

	private static IHTTP http = new DefaultHTTP();

	public static void setHttp(IHTTP http) {
		SwaggerCall.http = http;
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
			return SwaggerServlet.executor
					.submit(new SwaggerAsyncRequest(context, authorization, method, url, req, clazz));
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

		long dt1 = System.currentTimeMillis();

		for (String system : mapp.keySet()) {
			SwaggerCallParameters scp = mapp.get(system);
			map.put(system, callAsync(scp.context, scp.authorization, scp.method, scp.url, scp.req, scp.clazz));
		}

		for (String system : mapp.keySet()) {
			SwaggerCallStatus ls = new SwaggerCallStatus();
			ls.system = system;
			r.status.add(ls);
			long time = System.currentTimeMillis();
			try {
				long timeout = timeoutMilliseconds - (time - dt1);
				if (timeout < 0L)
					timeout = 0;
				SwaggerAsyncResponse futureresponse = map.get(system).get(timeout, TimeUnit.MILLISECONDS);
				// SwaggerException ex = futureresponse.getException();
				// if (ex != null) {
				// log.error("Erro acessando " + system, ex);
				// ls.errormsg = SwaggerUtils.messageAsString(ex);
				// ls.stacktrace = SwaggerUtils.stackAsString(ex);
				// }
				ls.miliseconds = futureresponse.getMiliseconds();
				ISwaggerResponse o = futureresponse.getRespOrThrowException();
				if (o != null)
					r.responses.put(system, o);
			} catch (Exception ex) {
				boolean logged = true;
				if (ex instanceof SwaggerException) {
					SwaggerException se = (SwaggerException) ex;
					if (se.resp instanceof SwaggerError) {
						SwaggerError err = (SwaggerError) se.resp;
						if (err.errordetails != null && err.errordetails.size() > 0)
							logged = err.errordetails.get(0).logged;
					}
				}
				if (!(ex instanceof IUnloggedException))
					logged = false;
				if (logged)
					log.error("Erro acessando " + system, ex);
				ls.errormsg = SwaggerUtils.messageAsString(ex);
				if (ls.errormsg == null)
					ls.errormsg = ex.getClass().getName();
				ls.stacktrace = SwaggerUtils.stackAsString(ex);
				if (ls.miliseconds == null)
					ls.miliseconds = System.currentTimeMillis() - time;
				if (ex instanceof TimeoutException)
					map.get(system).cancel(true);
			}
		}
		return r;
	}

}
