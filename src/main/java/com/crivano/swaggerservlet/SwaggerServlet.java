package com.crivano.swaggerservlet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.restservlet.ICacheableRestAction;
import com.crivano.restservlet.RestServlet;
import com.crivano.restservlet.RestUtils;

public class SwaggerServlet extends HttpServlet {
	private static final Logger log = LoggerFactory
			.getLogger(SwaggerServlet.class);

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// Return the swagger.yaml that is placed at
		// src/main/webapp/{servletpath}/swagger.yaml
		//
		if (req.getPathInfo() != null
				&& req.getPathInfo().endsWith("/swagger.yaml")) {
			InputStream is = this.getServletContext().getResourceAsStream(
					req.getServletPath() + req.getPathInfo());
			if (is == null) {
				is = this.getClass().getResourceAsStream("/swagger.yaml");
			}
			String sSwagger = RestUtils.convertStreamToString(is);
			byte[] ab = sSwagger.getBytes();
			resp.setContentType("text/x-yaml");
			resp.setContentLength(ab.length);
			resp.getOutputStream().write(ab);
			resp.getOutputStream().flush();
		} else
			super.doGet(req, resp);
	}

	private static final long serialVersionUID = 4436503480265700847L;

	private Swagger swagger = null;
	private String actionpackage = null;

	private class Prepared {
		ISwaggerMethod action;
		String context;
		boolean cacheable;
	}

	private ThreadLocal<Prepared> current = new ThreadLocal<Prepared>();

	@Override
	protected void prepare(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
		String requestMethod = request.getMethod();
		String requestPathInfo = request.getPathInfo();
		prepare(requestMethod, requestPathInfo, req);
	}

	public void prepare(String requestMethod, String requestPathInfo,
			JSONObject req) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		Prepared p = new Prepared();
		current.set(null);

		String method = requestMethod.toLowerCase();
		String path = swagger.checkRequest(requestPathInfo, method, req);

		path = toCamelCase(path + " " + method);

		Class<?> clazz = Class.forName(actionpackage + "." + path);
		Constructor<?> ctor = clazz.getConstructor();
		p.action = (ISwaggerMethod) ctor.newInstance();

		p.context = p.action.getContext();
		p.cacheable = p.action instanceof ICacheableRestAction;

		current.set(p);
	}

	protected void run(ISwaggerRequest req, ISwaggerResponse resp)
			throws Exception {
		Prepared p = current.get();

		p.action.run(req, resp);
	}

	public String toCamelCase(String path) {
		path = path.replaceAll("[^A-Za-z0-9]", " ");
		path = path.trim();
		path = path.replaceAll("\\s+", "_");

		StringBuilder sb = new StringBuilder();
		for (String oneString : path.split("_")) {
			sb.append(oneString.substring(0, 1).toUpperCase());
			sb.append(oneString.substring(1));
		}
		return sb.toString();
	}

	protected String getContext() {
		Prepared prepared = current.get();
		if (prepared == null)
			return null;
		String context = prepared.context;
		return context;
	}

	protected boolean isCacheable() {
		Prepared prepared = current.get();
		if (prepared == null)
			return false;
		Boolean cacheable = prepared.cacheable;
		return cacheable;
	}

	protected String getService() {
		return swagger.getInfoTitle();
	}

	public void setSwagger(Swagger sw) {
		this.swagger = sw;
	}

	public void setActionPackage(String ap) {
		this.actionpackage = ap;
	}

	private String authorization = null;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject req = new JSONObject();
		JSONObject resp = new JSONObject();

		try {
			try {
				if (request.getContentType() != null
						&& request.getContentType().startsWith(
								"application/json"))
					req = RestUtils.getJsonReq(request, getContext());
			} catch (Exception e) {
			}

			Enumeration paramNames = request.getParameterNames();
			while (paramNames != null && paramNames.hasMoreElements()) {
				String paramName = (String) paramNames.nextElement();
				String[] paramValues = request.getParameterValues(paramName);
				if (request.getParameter(paramName) instanceof String
						&& !req.has(paramName))
					req.put(paramName, request.getParameter(paramName));
			}

			for (Object key : request.getParameterMap().keySet())
				if (key instanceof String
						&& request.getParameter((String) key) instanceof String
						&& !req.has((String) key))
					req.put((String) key, request.getParameter((String) key));

			prepare(request, response, req, resp);

			corsHeaders(response);

			if (isCacheable()) {
				String cache = RestUtils.cacheRetrieveJson(getContext(), req);
				if (cache != null) {
					RestUtils.writeJsonRespFromCache(response, cache,
							getContext(), getService());
					return;
				}
			}

			if (getAuthorization() != null
					&& !getAuthorization().equals(
							request.getHeader("Authorization")))
				throw new Exception("Unauthorized.");

			run(req, resp);

			response.setHeader("Swagger-Servlet-Version", "0.0.2-SNAPSHOT");

			if (resp.has("content-type")) {
				byte[] payload = RestUtils.base64Decode(resp
						.getString("payload"));
				response.setContentLength(payload.length);
				response.setContentType(resp.getString("content-type"));
				response.getOutputStream().write(payload);
				response.getOutputStream().flush();
				response.getOutputStream().close();
				return;
			}

			if (isCacheable() && resp.optString("error", null) == null) {
				RestUtils.cacheStoreJson(getContext(), req, resp);
			}

			log.info("EXT-HTTP: method:\"" + request.getMethod()
					+ "\", path:\"" + request.getPathInfo() + "\", "
					+ (req != null ? "\", request:" + req : "") + ", response:"
					+ resp.toString());
			RestUtils.writeJsonResp(response, resp, getContext(), getService());
		} catch (Exception e) {
			RestUtils.writeJsonError(request, response, e, req, resp,
					getContext(), getService());
		} finally {
			response.getWriter().close();
		}
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	public void doOptions(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		corsHeaders(response);
		response.setStatus(200);
		response.getWriter().write("OK");
		response.getWriter().close();
	}

	protected void corsHeaders(HttpServletResponse response) {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods",
				"GET,POST,DELETE,PUT,OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");
	}

	protected boolean isCacheable() {
		return false;
	};

	public String getAuthorization() {
		return authorization;
	}

	public void setAuthorization(String authorization) {
		this.authorization = authorization;
	}
}
