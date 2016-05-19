package com.crivano.restservlet;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.crivano.swaggerservlet.Swagger;

public abstract class RestServlet extends HttpServlet {
	private static final long serialVersionUID = -3272264240843348162L;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			JSONObject req = new JSONObject();

			try {
				req = RestUtils.getJsonReq(request, getContext());
			} catch (Exception e) {
			}

			for (Object key : request.getParameterMap().keySet())
				if (key instanceof String
						&& request.getParameter((String) key) instanceof String
						&& !req.has((String) key))
					req.put((String) key, request.getParameter((String) key));

			corsHeaders(response);

			JSONObject resp = new JSONObject();
			run(request, response, req, resp);

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

	abstract protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception;

	abstract protected String getContext();

	abstract protected String getService();
}
