package com.crivano.swaggerservlet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.swaggerservlet.dependency.IDependency;
import com.crivano.swaggerservlet.property.IProperty;
import com.crivano.swaggerservlet.test.Test;

public class SwaggerServlet extends HttpServlet {
	private static final Logger log = LoggerFactory
			.getLogger(SwaggerServlet.class);

	private static final long serialVersionUID = 4436503480265700847L;

	private Swagger swagger = null;
	private String actionpackage = null;
	private Map<String, IDependency> dependencies = new TreeMap<>();
	private List<IProperty> properties = new ArrayList<>();
	private Map<String, String> manifest = new TreeMap<>();

	private String authorization = null;
	private String authorizationToProperties = null;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		try (InputStream is = config.getServletContext().getResourceAsStream(
				"/META-INF/MANIFEST.MF")) {
			String m = SwaggerUtils.convertStreamToString(is);
			m = m.replaceAll("\r\n", "\n");
			for (String s : m.split("\n")) {
				String a[] = s.split(":", 2);
				if (a.length == 2)
					manifest.put(a[0].trim(), a[1].trim());
			}
		} catch (IOException e) {
			log.error("INIT ERROR: ", e);
		}
	}

	private class Prepared {
		String actionName;
		ISwaggerMethod action;
		String context;
		boolean cacheable;
		Swagger.Path matchingPath;
		Class<? extends ISwaggerRequest> clazzRequest;
		Class<? extends ISwaggerResponse> clazzResponse;
		ISwaggerRequest req;
		ISwaggerResponse resp;
		HttpServletRequest request;
		HttpServletResponse response;
	}

	private static ThreadLocal<Prepared> current = new ThreadLocal<Prepared>();

	public static HttpServletRequest getHttpServletRequest() {
		return current.get().request;
	}

	public static HttpServletResponse getHttpServletResponse() {
		return current.get().response;
	}

	public Map<String, String> getManifestEntries() {
		return manifest;
	}

	protected void prepare(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String requestMethod = request.getMethod();
		String requestPathInfo = request.getPathInfo();
		prepare(requestMethod, requestPathInfo);
	}

	public void prepare(String requestMethod, String requestPathInfo)
			throws ClassNotFoundException, NoSuchMethodException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		Prepared p = new Prepared();
		current.set(null);

		String method = requestMethod.toLowerCase();

		p.matchingPath = swagger.checkRequestPath(requestPathInfo, method);

		p.actionName = toCamelCase(p.matchingPath.swaggerPath + " " + method);

		Class<?> clazz = Class.forName(actionpackage + "." + p.actionName);
		Constructor<?> ctor = clazz.getConstructor();
		p.action = (ISwaggerMethod) ctor.newInstance();

		p.context = p.action.getContext();
		p.cacheable = p.action instanceof ISwaggerCacheableMethod;

		p.clazzRequest = (Class<? extends ISwaggerRequest>) Class
				.forName(swagger.getInterfacePackage() + "."
						+ swagger.getInterfaceName() + "$" + p.actionName
						+ "Request");

		p.clazzResponse = (Class<? extends ISwaggerResponse>) Class
				.forName(swagger.getInterfacePackage() + "."
						+ swagger.getInterfaceName() + "$" + p.actionName
						+ "Response");
		p.req = p.clazzRequest.newInstance();
		p.resp = p.clazzResponse.newInstance();

		current.set(p);
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

	public String getService() {
		return swagger.getInfoTitle();
	}

	public String getUser() {
		return null;
	}

	public void setSwagger(Swagger sw) {
		this.swagger = sw;
	}

	public void setActionPackage(String ap) {
		this.actionpackage = ap;
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {

		// Return the swagger.yaml that is placed at
		// src/main/webapp/{servletpath}/swagger.yaml
		//
		if (req.getPathInfo() != null
				&& req.getPathInfo().endsWith("/swagger.yaml")) {
			InputStream is = getSwaggerYamlAsStream();
			if (is == null) {
				is = this.getClass().getResourceAsStream("/swagger.yaml");
			}
			String sSwagger = SwaggerUtils.convertStreamToString(is);
			sSwagger = sSwagger.replaceFirst("basePath: [^\\s]+", "basePath: "
					+ req.getRequestURI().replace("/swagger.yaml", ""));
			byte[] ab = sSwagger.getBytes();
			response.setContentType("text/x-yaml");
			response.setContentLength(ab.length);
			response.getOutputStream().write(ab);
			response.getOutputStream().flush();
		} else if (req.getPathInfo() != null
				&& req.getPathInfo().endsWith("/swagger-ui")) {
			response.sendRedirect(req.getRequestURL()
					+ "/index.html?url="
					+ req.getRequestURL().substring(
							0,
							req.getRequestURL().length()
									- "/swagger-ui".length()) + "/swagger.yaml");
		} else if (req.getPathInfo() != null
				&& req.getPathInfo().contains("/swagger-ui/")) {
			// Return components of the swagger-ui
			//
			String resource = req.getPathInfo().substring(
					req.getPathInfo().lastIndexOf("/swagger-ui/")
							+ "/swagger-ui/".length());
			if (!resource.matches("^[a-z0-9\\.-]+$"))
				throw new ServletException("Invalid swagger-ui resource");
			InputStream is = this.getClass().getResourceAsStream(
					"/com/crivano/swaggerservlet/dist/" + resource);
			String sSwagger = SwaggerUtils.convertStreamToString(is);
			byte[] ab = sSwagger.getBytes(StandardCharsets.UTF_8);
			if (resource.endsWith(".html"))
				response.setContentType("text/html; charset=utf-8");
			else if (resource.endsWith(".css"))
				response.setContentType("text/css; charset=utf-8");
			else if (resource.endsWith(".js"))
				response.setContentType("text/javascript; charset=utf-8");
			else if (resource.endsWith(".png"))
				response.setContentType("image/png");
			response.setContentLength(ab.length);
			response.getOutputStream().write(ab);
			response.getOutputStream().flush();
		} else if ("GET".equals(req.getMethod()) && req.getPathInfo() != null
				&& req.getPathInfo().equals("/test")) {
			Test.run(this, dependencies, properties, req, response);
		} else
			doPost(req, response);
	}

	public InputStream getSwaggerYamlAsStream() {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		String path = swagger.getInterfacePackage().replace(".", "/");
		InputStream is = classLoader
				.getResourceAsStream(path + "/swagger.yaml");
		return is;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ISwaggerRequest req = null;
		ISwaggerResponse resp = null;

		// String loggerPath = request.getContextPath().replace("/", "") +
		// request.getPathInfo().replace("/", ".") + "." +
		// request.getMethod().toLowerCase();
		// Logger loghttp = LoggerFactory.getLogger(loggerPath);
		LogResponse lr = new LogResponse();
		lr.method = request.getMethod();
		lr.path = request.getContextPath() + request.getPathInfo();

		try {
			prepare(request, response);
			Prepared prepared = current.get();
			prepared.request = request;
			prepared.response = response;
			req = prepared.req;
			resp = prepared.resp;

			req = injectVariables(request, req);

			corsHeaders(response);

			if (isCacheable()) {
				// String cache = RestUtils.cacheRetrieveJson(getContext(),
				// req);
				// if (cache != null) {
				// RestUtils.writeJsonRespFromCache(response, cache,
				// getContext(), getService());
				// return;
				// }
			}

			if (getAuthorization() != null
					&& !getAuthorization().equals(
							getAuthorizationFromHeader(request)))
				throw new Exception("Unauthorized.");

			run(req, resp);

			response.setHeader("Swagger-Servlet-Version", "0.0.2-SNAPSHOT");

			String userAgent = request.getHeader("User-Agent");
			if (userAgent == null)
				userAgent = request.getHeader("user-agent");
			response.setHeader("Swagger-Servlet-Request-UA", userAgent);
			boolean fCanReturnPayload = !"SwaggerServlet".equals(userAgent);

			if (fCanReturnPayload && resp instanceof ISwaggerResponseFile) {
				ISwaggerResponseFile r = (ISwaggerResponseFile) resp;
				if (r.getContentlength() != null)
					response.setContentLength(r.getContentlength().intValue());
				response.setContentType(r.getContenttype());
				if (r.getContentdisposition() != null)
					response.setHeader("Content-Disposition",
							r.getContentdisposition());
				SwaggerUtils.transferContent(r.getInputstream(),
						response.getOutputStream());
				return;
			}

			try {
				if (fCanReturnPayload && swagger.has(resp, "contenttype")) {
					byte[] payload = (byte[]) swagger.get(resp, "payload");
					response.setContentLength(payload.length);
					response.setContentType((String) swagger.get(resp,
							"contenttype"));
					response.getOutputStream().write(payload);
					response.getOutputStream().flush();
					response.getOutputStream().close();
					return;
				}
			} catch (Exception ex) {

			}

			if (isCacheable()) {
				// RestUtils.cacheStoreJson(getContext(), req, resp);
			}

			lr.request = req;
			lr.response = resp;
			log.debug("HTTP-OK: " + SwaggerUtils.toJson(lr));

			SwaggerUtils.writeJsonResp(response, resp, getContext(),
					getService());
			response.getWriter().close();
		} catch (Exception e) {
			try {
				int sts = errorCode(e);
				if (e instanceof SwaggerAuthorizationException)
					sts = 401;

				SwaggerError error = SwaggerUtils.writeJsonError(sts, request,
						response, e, req, resp, getContext(), getService(),
						getUser());
				response.getWriter().close();

				if (shouldBeLogged(sts, e)) {
					lr.request = req;
					lr.response = error;
					String details = SwaggerUtils.toJson(lr);
					log.error("HTTP-ERROR: {}, EXCEPTION", details, e);
				}
			} catch (Exception e2) {
				if (e.getMessage() != null
						&& e.getMessage().contains("Connection reset"))
					return;
				throw new RuntimeException("Error returning error message.", e);
			}
		}
	}

	/**
	 * What error code should we output for this exception?
	 * 
	 * @param e
	 * @return
	 */
	public int errorCode(Exception e) {
		if (e instanceof SwaggerException)
			return ((SwaggerException) e).getStatus();
		return 500;
	}

	/**
	 * Should this exception be logged?
	 * 
	 * @param sts
	 * @param s
	 * @return
	 */
	public boolean shouldBeLogged(int sts, Exception e) {
		return sts >= 500 && !(e instanceof IUnloggedException);
	}

	public String getAuthorizationFromHeader(HttpServletRequest request) {
		String s = request.getHeader("Authorization");
		if (s == null)
			return null;
		s = s.trim();

		if (s.startsWith("Basic ")) {
			String userpass = new String(SwaggerUtils.base64Decode(s
					.substring(6)));
			s = userpass.split(":")[1];
		}
		return s;

	}

	public ISwaggerRequest injectVariables(HttpServletRequest request,
			ISwaggerRequest req) throws Exception {
		Prepared prepared = current.get();

		// Inject JSON body parameters
		try {
			if (request.getContentType() != null
					&& request.getContentType().startsWith("application/json")) {
				ISwaggerRequest reqFromJson = SwaggerUtils.getJsonReq(request,
						getContext(), prepared.clazzRequest);
				if (reqFromJson != null) {
					req = reqFromJson;
					prepared.req = req;
				}
			}
		} catch (Exception e) {
			throw e;
		}

		// Inject form parameters
		Enumeration paramNames = request.getParameterNames();
		while (paramNames != null && paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			String[] paramValues = request.getParameterValues(paramName);
			if (request.getParameter(paramName) instanceof String
					&& !swagger.has(req, paramName))
				swagger.set(req, paramName, request.getParameter(paramName));
		}

		// Inject querystring parameters
		for (Object key : request.getParameterMap().keySet())
			if (key instanceof String
					&& request.getParameter((String) key) instanceof String
					&& !Swagger.has(req, (String) key))
				Swagger.set(req, (String) key,
						request.getParameter((String) key));

		// Inject path parameters
		swagger.injectPathVariables(req, prepared.matchingPath);
		return req;
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

	public static void corsHeaders(HttpServletResponse response) {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods",
				"GET,POST,DELETE,PUT,OPTIONS");
		response.addHeader("Access-Control-Allow-Headers",
				"Content-Type,Authorization");
	}

	public String getAuthorization() {
		return authorization;
	}

	public void setAuthorization(String authorization) {
		this.authorization = authorization;
	}

	public String getAuthorizationToProperties() {
		return authorizationToProperties;
	}

	public void setAuthorizationToProperties(String authorization) {
		this.authorizationToProperties = authorization;
	}

	public void run(ISwaggerRequest req, ISwaggerResponse resp)
			throws Exception {
		Prepared prepared = current.get();
		Class<? extends ISwaggerMethod> clazzAction = prepared.action
				.getClass();
		try {
			clazzAction.getMethod("run", prepared.clazzRequest,
					prepared.clazzResponse).invoke(prepared.action, req, resp);
		} catch (InvocationTargetException ex) {
			if (ex.getCause() instanceof Exception)
				throw (Exception) ex.getCause();
			else
				throw ex;
		}
	}

	public void setAPI(Class clazzInterface) {
		Swagger sw = new Swagger();
		sw.setInterfaceName(clazzInterface.getSimpleName());
		sw.setInterfacePackage(clazzInterface.getPackage().getName());
		setSwagger(sw);
		InputStream is = getSwaggerYamlAsStream();
		sw.loadFromInputStream(is);
	}

	public boolean test() {
		return true;
	}

	public void addDependency(IDependency dep) {
		dependencies.put(dep.getService(), dep);
	}

	public void addProperty(IProperty p) {
		properties.add(p);
	}

}
