package com.crivano.swaggerservlet;

import java.lang.reflect.Constructor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.RestServlet;

public class SwaggerServlet extends RestServlet {
	private static final long serialVersionUID = 4436503480265700847L;

	private Swagger swagger = null;
	private String actionpackage = null;
	private ThreadLocal<String> currentContext = new ThreadLocal<String>();

	@Override
	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
		String method = request.getMethod().toLowerCase();
		String path = swagger.checkRequest(request.getPathInfo(), method, req);

		path = toCamelCase(path + " " + method);

		Class<?> clazz = Class.forName(actionpackage + "." + path);
		Constructor<?> ctor = clazz.getConstructor();
		IRestAction action = (IRestAction) ctor.newInstance();

		currentContext.set(action.getContext());

		action.run(request, response, req, resp);

		action = null;
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

	@Override
	protected String getContext() {
		String context = currentContext.get();
		return context;
	}

	@Override
	protected String getService() {
		return swagger.getInfoTitle();
	}

	public void setSwagger(Swagger sw) {
		this.swagger = sw;
	}

	public void setActionPackage(String ap) {
		this.actionpackage = ap;
	}
}
