package com.crivano.swaggerservlet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.swaggerservlet.dependency.IDependency;
import com.crivano.swaggerservlet.test.Test;

public class SwaggerServlet extends HttpServlet {
	private static final String SWAGGERSERVLET_PROPERTIES_SECRET_NAME = "swaggerservlet.properties.secret";
	public static final String SWAGGERSERVLET_THREADPOOL_SIZE_PROPERTY_NAME = "swaggerservlet.threadpool.size";
	public static final String SWAGGERSERVLET_THREADPOOL_SIZE_DEFAULT_VALUE = "20";
	public static final String SWAGGERSERVLET_CALL_CONTENT_TYPE_NAME = "swaggerservlet.call.content.type";
	public static final String SWAGGERSERVLET_CALL_CONTENT_TYPE_VALUE = "application/json";

	private static final Logger log = LoggerFactory.getLogger(SwaggerServlet.class);

	private static final long serialVersionUID = 4436503480265700847L;

//	protected static SwaggerServlet instance = null;

	private Swagger swagger = null;
	private String actionpackage = null;
	private Set<String> aditionalPackages = new TreeSet<>();
	private Map<String, IDependency> dependencies = new TreeMap<>();
	private List<Property> properties = new ArrayList<>();
	private Map<String, String> manifest = new TreeMap<>();

	private String authorization = null;
	private String authorizationToProperties = null;

	private Class<? extends ISwaggerApiContext> classApiContext;

	public String servletContext = null;
	public static String callContentType;

	public static ExecutorService executor = null;

//	public SwaggerServlet() {
//		if (instance == null)
//			instance = this;
//	}

	@Override
	public final void init(ServletConfig config) throws ServletException {
		super.init(config);
		servletContext = config.getServletContext().getContextPath().replace("/", "");

		addRestrictedProperty(SWAGGERSERVLET_THREADPOOL_SIZE_PROPERTY_NAME,
				SWAGGERSERVLET_THREADPOOL_SIZE_DEFAULT_VALUE);
		addRestrictedProperty(SWAGGERSERVLET_CALL_CONTENT_TYPE_NAME, SWAGGERSERVLET_CALL_CONTENT_TYPE_VALUE);

		callContentType = getProperty(SWAGGERSERVLET_CALL_CONTENT_TYPE_NAME);

		addPrivateProperty(SWAGGERSERVLET_PROPERTIES_SECRET_NAME, null);

		setAuthorizationToProperties(getProperty(SWAGGERSERVLET_PROPERTIES_SECRET_NAME));

		try (InputStream is = config.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF")) {
			String m = SwaggerUtils.convertStreamToString(is);
			if (m != null) {
				m = m.replaceAll("\r\n", "\n");
				for (String s : m.split("\n")) {
					String a[] = s.split(":", 2);
					if (a.length == 2)
						manifest.put(a[0].trim(), a[1].trim());
				}
			}
		} catch (IOException e) {
			log.error("INIT ERROR: ", e);
		}

		if (executor == null)
			executor = Executors
					.newFixedThreadPool(new Integer(getProperty(SWAGGERSERVLET_THREADPOOL_SIZE_PROPERTY_NAME)));

		try {
			initialize(config);
		} catch (Exception ex) {
			throw new ServletException(ex);
		}

		try {
			assertProperties();
		} catch (Exception ex) {
			log.error("PROPERTIES ERROR", ex);
		}
	}

	protected void initialize(ServletConfig config) throws Exception {
	}

	private static ThreadLocal<SwaggerContext> current = new ThreadLocal<SwaggerContext>();

	private Map<String, String> redefinedProperties = new HashMap<>();

	public String getProperty(final String propertyName) {
		String name = propertyName(propertyName);
		Property dp = getDefinedProperty(name);
		if (dp == null)
			throw new RuntimeException(
					"Can't access property '" + name + "' because it was not declared in servlet's initialization");
		if (redefinedProperties.containsKey(name))
			return redefinedProperties.get(name);
		String s = System.getProperty(name);
		if (s != null)
			return s;
		s = System.getenv("PROP_" + name.replace(".", "_").toUpperCase());
		if (s != null)
			return s;
		if (dp.isOptional())
			return dp.getDefaultValue();
		throw new RuntimeException("Property '" + name + "' not defined");
	}

	public void setProperty(String propertyName, String value) {
		redefinedProperties.put(propertyName, value);
	}

	public Property getDefinedProperty(String propertyName) {
		for (Property p : properties)
			if (p.getName().equals(propertyName))
				return p;
		return null;
	}

	// Used to simplify stack trace
	public void showPackageErrors(String pkg) {
		this.aditionalPackages.add(pkg);
	}

	public static HttpServletRequest getHttpServletRequest() {
		return current.get().getRequest();
	}

	public static HttpServletResponse getHttpServletResponse() {
		return current.get().getResponse();
	}

	public Map<String, String> getManifestEntries() {
		return manifest;
	}

	protected void prepare(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String requestMethod = request.getMethod();
		String requestPathInfo = request.getPathInfo();
		prepare(requestMethod, requestPathInfo);
	}

	public void prepare(String requestMethod, String requestPathInfo) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		SwaggerContext p = new SwaggerContext();
		current.set(null);

		String method = requestMethod.toLowerCase();

		p.setMatchingPath(swagger.checkRequestPath(requestPathInfo, method));

		p.setActionName(toCamelCase(p.getMatchingPath().swaggerPath + " " + method));

		Class<?> clazz = Class.forName(actionpackage + "." + p.getActionName());
		p.setAction((ISwaggerMethod) newInstance(clazz));

		p.setContext(p.getAction().getContext());
		p.setService(getService());
		p.setCacheable(p.getAction() instanceof ISwaggerCacheableMethod);

		p.setClazzRequest((Class<? extends ISwaggerRequest>) Class.forName(swagger.getInterfacePackage() + "."
				+ swagger.getInterfaceName() + "$I" + p.getActionName() + "$Request"));

		p.setClazzResponse((Class<? extends ISwaggerResponse>) Class.forName(swagger.getInterfacePackage() + "."
				+ swagger.getInterfaceName() + "$I" + p.getActionName() + "$Response"));

		p.setSubclazzContext(getApiContextClass());
		p.setClazzContext((Class<? extends ISwaggerApiContext>) Class
				.forName(swagger.getInterfacePackage() + "." + swagger.getInfoTitle() + "Context"));
		p.setReq(newInstance(p.getClazzRequest()));
		p.setResp(newInstance(p.getClazzResponse()));

		current.set(p);
	}

	public <T> T newInstance(Class<T> clazz) throws InstantiationException, IllegalAccessException {
		return clazz.newInstance();
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

	protected static String getContext() {
		SwaggerContext prepared = current.get();
		if (prepared == null)
			return null;
		String context = prepared.getContext();
		return context;
	}

	protected static boolean isCacheable() {
		SwaggerContext prepared = current.get();
		if (prepared == null)
			return false;
		Boolean cacheable = prepared.isCacheable();
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

	public Class<? extends ISwaggerApiContext> getApiContextClass() throws ClassNotFoundException {
		if (classApiContext == null)
			classApiContext = (Class<? extends ISwaggerApiContext>) Class
					.forName(swagger.getInterfacePackage() + "." + swagger.getInfoTitle() + "Context");
		return classApiContext;
	}

	public void setApiContextClass(Class<? extends ISwaggerApiContext> classApiContext) {
		this.classApiContext = classApiContext;
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

		// Return the swagger.yaml that is placed at
		// src/main/webapp/{servletpath}/swagger.yaml
		//
		if (req.getPathInfo() != null && req.getPathInfo().endsWith("/swagger.yaml")) {
			InputStream is = getSwaggerYamlAsStream();
			if (is == null) {
				is = this.getClass().getResourceAsStream("/swagger.yaml");
			}
			String sSwagger = SwaggerUtils.convertStreamToString(is);
			sSwagger = sSwagger.replaceFirst("basePath: [^\\s]+",
					"basePath: " + req.getRequestURI().replace("/swagger.yaml", ""));
			byte[] ab = sSwagger.getBytes();
			response.setContentType("text/x-yaml");
			response.setContentLength(ab.length);
			response.getOutputStream().write(ab);
			response.getOutputStream().flush();
		} else if (req.getPathInfo() != null && req.getPathInfo().endsWith("/swagger-ui")) {
			response.sendRedirect(req.getRequestURL() + "/index.html?url="
					+ req.getRequestURL().substring(0, req.getRequestURL().length() - "/swagger-ui".length())
					+ "/swagger.yaml");
		} else if (req.getPathInfo() != null && req.getPathInfo().contains("/swagger-ui/")) {
			// Return components of the swagger-ui
			//
			String resource = req.getPathInfo()
					.substring(req.getPathInfo().lastIndexOf("/swagger-ui/") + "/swagger-ui/".length());
			if (!resource.matches("^[a-z0-9\\.-]+$"))
				throw new ServletException("Invalid swagger-ui resource");
			InputStream is = this.getClass().getResourceAsStream("/com/crivano/swaggerservlet/dist/" + resource);
			String sSwagger = SwaggerUtils.convertStreamToString(is);
			if (sSwagger == null) {
				response.sendError(404);
				return;
			}
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
		} else if ("GET".equals(req.getMethod()) && req.getPathInfo() != null && req.getPathInfo().equals("/test")) {
			Test.run(this, dependencies, properties, req, response);
		} else
			doPost(req, response);
	}

	public InputStream getSwaggerYamlAsStream() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = swagger.getInterfacePackage().replace(".", "/");
		InputStream is = classLoader.getResourceAsStream(path + "/swagger.yaml");
		return is;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ISwaggerRequest req = null;
		ISwaggerResponse resp = null;

		// String loggerPath = request.getContextPath().replace("/", "") +
		// request.getPathInfo().replace("/", ".") + "." +
		// request.getMethod().toLowerCase();
		// Logger loghttp = LoggerFactory.getLogger(loggerPath);

		corsHeaders(request, response);

		try {
			prepare(request, response);
			SwaggerContext prepared = current.get();
			prepared.setRequest(request);
			prepared.setResponse(response);
			req = prepared.getReq();
			resp = prepared.getResp();

			req = injectVariables(request, req);

			if (isCacheable()) {
				// String cache = RestUtils.cacheRetrieveJson(getContext(),
				// req);
				// if (cache != null) {
				// RestUtils.writeJsonRespFromCache(response, cache,
				// getContext(), getService());
				// return;
				// }
			}

			if (!ISwaggerPublicMethod.class.isAssignableFrom(prepared.getAction().getClass())
					&& getAuthorization() != null && !getAuthorization().equals(getAuthorizationFromHeader(request)))
				throw new Exception("Unauthorized.");

			run(req, resp);

			// The run() method may have changed the threadlocal.
			current.set(prepared);

			if (!response.isCommitted())
				flush(req, resp);
		} catch (Exception e) {
			try {
				int sts = errorStatus(e);
				if (e instanceof SwaggerAuthorizationException)
					sts = 401;
				String errorcode = errorCode(e);

				SwaggerError error = SwaggerUtils.writeJsonError(sts, errorcode, request, response, e, req, resp,
						getContext(), getService(), getUser(),
						(e instanceof SwaggerDetailedException) ? ((SwaggerDetailedException) e).status : null);
				response.getWriter().close();

				if (shouldBeLogged(sts, e)) {
					LogResponse lr = new LogResponse();
					lr.method = request.getMethod();
					lr.path = request.getContextPath() + request.getPathInfo();
					lr.request = req;
					lr.response = error;
					String details = SwaggerUtils.toJson(lr);

					aditionalPackages.add(actionpackage);
					log.error("HTTP-ERROR: {}, EXCEPTION {}", details, SwaggerUtils.simplifyStackTrace(e,
							aditionalPackages.toArray(new String[aditionalPackages.size()])));
				}
			} catch (Exception e2) {
				if (e.getMessage() != null && e.getMessage().contains("Connection reset"))
					return;
				throw new RuntimeException("Error returning error message.", e);
			}
		}
	}

	public static void flush(ISwaggerRequest req, ISwaggerResponse resp) throws IOException, Exception {
		HttpServletRequest request = getHttpServletRequest();
		HttpServletResponse response = getHttpServletResponse();

		LogResponse lr = new LogResponse();
		lr.method = request.getMethod();
		lr.path = request.getContextPath() + request.getPathInfo();

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
				response.setHeader("Content-Disposition", r.getContentdisposition());
			SwaggerUtils.transferContent(r.getInputstream(), response.getOutputStream());
			return;
		}

		// try {
		// if (fCanReturnPayload && swagger.has(resp, "contenttype")) {
		// byte[] payload = (byte[]) swagger.get(resp, "payload");
		// response.setContentLength(payload.length);
		// response.setContentType((String) swagger.get(resp, "contenttype"));
		// response.getOutputStream().write(payload);
		// response.getOutputStream().flush();
		// response.getOutputStream().close();
		// return;
		// }
		// } catch (Exception ex) {
		//
		// }

		if (isCacheable()) {
			// RestUtils.cacheStoreJson(getContext(), req, resp);
		}

		lr.request = req;
		lr.response = resp;
		log.debug("HTTP-OK: " + SwaggerUtils.toJson(lr));

		SwaggerUtils.writeJsonResp(response, resp, getContext(), current.get().getService());
		response.getWriter().close();
	}

	// What error number should we output for this exception?
	//
	public int errorStatus(Exception e) {
		if (e instanceof SwaggerException)
			return ((SwaggerException) e).getStatus();
		return 500;
	}

	// What error code should we output for this exception?
	//
	public String errorCode(Exception e) {
		return null;
	}

	// Should this exception be logged?
	//
	public boolean shouldBeLogged(int sts, Exception e) {
		return sts >= 500 && !(e instanceof IUnloggedException);
	}

	public String getAuthorizationFromHeader(HttpServletRequest request) {
		String s = request.getHeader("Authorization");
		if (s == null)
			return null;
		s = s.trim();

		if (s.startsWith("Basic ")) {
			String userpass = new String(SwaggerUtils.base64Decode(s.substring(6)));
			s = userpass.split(":")[1];
		}
		return s;

	}

	public ISwaggerRequest injectVariables(HttpServletRequest request, ISwaggerRequest req) throws Exception {
		SwaggerContext prepared = current.get();

		// Inject file parameters
		if (req instanceof ISwaggerRequestFile && ServletFileUpload.isMultipartContent(request)) {
			ISwaggerRequestFile reqFile = (ISwaggerRequestFile) req;
			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload();

			// Parse the request
			FileItemIterator iter = upload.getItemIterator(request);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				String name = item.getFieldName();
				InputStream stream = item.openStream();
				if (item.isFormField()) {
					String value = Streams.asString(stream, StandardCharsets.UTF_8.toString());
					SwaggerUtils.log(this.getClass())
							.debug("Form field " + name + " with value " + value + " detected.");
					if (!swagger.has(req, name))
						swagger.set(req, name, value);
				} else {
					SwaggerUtils.log(this.getClass())
							.debug("File field " + name + " with file name " + item.getName() + " detected.");
					reqFile.setContenttype(item.getContentType());
					reqFile.setFilename(item.getName());
					reqFile.setContent(SwaggerUtils.upload(item.getName(), item.getContentType(), stream));

					Enumeration<String> headerNames = request.getHeaderNames();
					if (headerNames != null) {
						Map<String, List<String>> headerFields = new HashMap<>();
						while (headerNames.hasMoreElements()) {
							String headerName = headerNames.nextElement();
							if (headerFields.get(headerName) == null)
								headerFields.put(headerName, new ArrayList<String>());
							headerFields.get(headerName).add(request.getHeader(headerName));
						}
						reqFile.setHeaderFields(headerFields);
					}
				}
			}
		}

		// Inject JSON body parameters
		try {
			if (request.getContentType() != null && request.getContentType().startsWith("application/json")) {
				ISwaggerRequest reqFromJson = SwaggerUtils.getJsonReq(request, getContext(),
						prepared.getClazzRequest());
				if (reqFromJson != null) {
					req = reqFromJson;
					prepared.setReq(req);
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
			if (request.getParameter(paramName) instanceof String && !swagger.has(req, paramName))
				swagger.set(req, paramName, request.getParameter(paramName));
		}

		// Inject querystring parameters
		swagger.injectQueryStringParameters(req, request.getParameterMap());

		// Inject path parameters
		swagger.injectPathVariables(req, prepared.getMatchingPath());
		return req;
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	public void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		corsHeaders(request, response);
		response.setStatus(200);
		response.getWriter().write("OK");
		response.getWriter().close();
	}

	public static void corsHeaders(HttpServletRequest request, HttpServletResponse response) {
		String origin = request.getHeader("Origin");
		if (origin == null)
			origin = "*";
		response.addHeader("Access-Control-Allow-Origin", origin);
		response.addHeader("Access-Control-Allow-Methods", "GET,POST,DELETE,PUT,OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type,Authorization");
		response.addHeader("Access-Control-Allow-Credentials", "true");
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

	public void run(ISwaggerRequest req, ISwaggerResponse resp) throws Exception {
		SwaggerContext prepared = current.get();
		prepared.setReq(req);
		prepared.setResp(resp);
		try {
			invoke(prepared);
		} catch (InvocationTargetException ex) {
			Exception exc = ex;
			if (exc.getCause() != null && exc.getCause() instanceof Exception)
				exc = (Exception) exc.getCause();
			if (exc instanceof ServletException && exc.getCause() != null && exc.getCause() instanceof Exception)
				exc = (Exception) exc.getCause();
			throw exc;
		}
	}

	public void invoke(SwaggerContext prepared) throws Exception {
		Class<? extends ISwaggerMethod> clazzAction = prepared.getAction().getClass();
		Method method = clazzAction.getMethod("run", prepared.getClazzRequest(), prepared.getClazzResponse(),
				prepared.getClazzContext());
		try (ISwaggerApiContext ctx = newInstance(prepared.getSubclazzContext())) {
			ctx.init(prepared);
			try {
				ctx.onTryBegin();
				method.invoke(prepared.getAction(), prepared.getReq(), prepared.getResp(), ctx);
				ctx.onTryEnd();
			} catch (Throwable t) {
				if (t instanceof Exception)
					ctx.onCatch((Exception) t);
				else
					ctx.onCatch(new Exception(t));
			} finally {
				ctx.onFinally();
			}
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

	public static void setExecutor(ExecutorService newExecutor) {
		executor = newExecutor;
	}

	public boolean test() {
		return true;
	}

	public void addDependency(IDependency dep) {
		dependencies.put(dep.getService(), dep);
	}

	public String propertyName(String name) {
		if (servletContext == null || servletContext.length() == 0)
			return name;
		else if (name.startsWith("/"))
			return name.substring(1);
		else
			return servletContext + "." + name;
	}

	public void addPrivateProperty(String name) {
		properties.add(new Property(Property.Scope.PRIVATE, propertyName(name), false, null));
	}

	public void addRestrictedProperty(String name) {
		properties.add(new Property(Property.Scope.RESTRICTED, propertyName(name), false, null));
	}

	public void addPublicProperty(String name) {
		properties.add(new Property(Property.Scope.PUBLIC, propertyName(name), false, null));
	}

	public void addPrivateProperty(String name, String defaultValue) {
		properties.add(new Property(Property.Scope.PRIVATE, propertyName(name), true, defaultValue));
	}

	public void addRestrictedProperty(String name, String defaultValue) {
		properties.add(new Property(Property.Scope.RESTRICTED, propertyName(name), true, defaultValue));
	}

	public void addPublicProperty(String name, String defaultValue) {
		properties.add(new Property(Property.Scope.PUBLIC, propertyName(name), true, defaultValue));
	}

	public void assertProperties() {
		for (Property p : properties) {
			if (p.getName().startsWith(servletContext + "."))
				getProperty(p.getName().substring(servletContext.length() + 1));
		}
	}

	public ISwaggerResponse execute(String requestMethod, String requestPathInfo, ISwaggerRequest reqOverride,
			HttpServletRequest request, Map<String, String[]> params) throws Exception {
		prepare(requestMethod, requestPathInfo);
		SwaggerContext prepared = current.get();
		if (reqOverride != null)
			prepared.setReq(reqOverride);
		if (request != null)
			prepared.setRequest(request);
//		prepared.setResponse(response);
		ISwaggerRequest req = prepared.getReq();
		ISwaggerResponse resp = prepared.getResp();

		if (request != null)
			req = injectVariables(request, req);

		if (params != null)
			swagger.injectQueryStringParameters(req, params);
		swagger.injectPathVariables(req, prepared.getMatchingPath());

		run(req, resp);

		return resp;
	}

	public ISwaggerResponse execute(String requestMethod, String requestPathInfo, Map<String, String> params)
			throws Exception {
		Map<String, String[]> map = new HashMap<>();

		if (params != null)
			for (String key : params.keySet()) {
				map.put(key, new String[] { params.get(key) });
			}
		return execute(requestMethod, requestPathInfo, null, null, map);
	}

	@Override
	public void destroy() {
		super.destroy();
		servletContext = null;
		redefinedProperties = new HashMap<>();
	}
}
