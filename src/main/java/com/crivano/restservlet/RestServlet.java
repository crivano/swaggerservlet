package com.crivano.restservlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

public abstract class RestServlet extends HttpServlet {
	private static final long serialVersionUID = -3272264240843348162L;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			JSONObject req = new JSONObject();

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

			JSONObject resp = new JSONObject();
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

			run(request, response, req, resp);

			if (resp.has("content-type")) {
				byte[] payload = new Base64().decode(resp.getString("payload"));
				response.setContentLength(payload.length);
				response.setContentType(resp.getString("content-type"));
				response.getOutputStream().write(payload);
				response.getOutputStream().flush();
				return;
			}

			if (isCacheable() && resp.optString("error", null) == null) {
				RestUtils.cacheStoreJson(getContext(), req, resp);
			}

			RestUtils.writeJsonResp(response, resp, getContext(), getService());
		} catch (Exception e) {
			RestUtils.writeJsonError(response, e, getContext(), getService());
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
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

	protected void prepare(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
	};

	abstract protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception;

	abstract protected String getContext();

	abstract protected String getService();
}
