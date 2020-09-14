package com.crivano.swaggerservlet.test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.swaggerservlet.LogResponse;
import com.crivano.swaggerservlet.Property;
import com.crivano.swaggerservlet.Property.Scope;
import com.crivano.swaggerservlet.SwaggerAsyncResponse;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;
import com.crivano.swaggerservlet.dependency.IDependency;
import com.crivano.swaggerservlet.dependency.SwaggerServletDependency;
import com.crivano.swaggerservlet.dependency.TestableDependency;

public class Test {
	private static final long DEFAULT_TIMEOUT_MILLISECONDS = 10000L;
	private static final Logger log = LoggerFactory.getLogger(Test.class);

	public static void run(SwaggerServlet ss, Map<String, IDependency> dependencies, List<Property> properties,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		long start_time = System.currentTimeMillis();
		Set<String> skip = new HashSet<>();
		Map<String, Boolean> partialMap = new HashMap<>();
		String[] skipValues = request.getParameterValues("skip");
		String[] partialValues = request.getParameterValues("partial");
		boolean skipAll = false;
		long timeout;
		try {
			timeout = Long.parseLong(request.getParameter("timeout"));
		} catch (NumberFormatException e) {
			timeout = DEFAULT_TIMEOUT_MILLISECONDS;
		}
		if (skipValues != null) {
			for (String s : skipValues)
				skip.add(s);
			if (skipValues.length == 1 && "all".equals(skipValues[0]))
				skipAll = true;
		}
		if (partialValues != null) {
			for (String s : partialValues) {
				String[] split = s.split(":");
				partialMap.put(split[0], Boolean.valueOf(split[1]));
			}
		}

		TestResponse tr = new TestResponse(null, ss.getService(), request.getRequestURI(), false);

		tr.version = ss.getManifestEntries().get("Build-Label");
		tr.timestamp = ss.getManifestEntries().get("Build-Time");

		boolean auth = ss.getAuthorizationToProperties() != null
				&& ss.getAuthorizationToProperties().equals(request.getParameter("authorizationToProperties"));

		for (Property p : properties) {
			if (p.getScope() == Scope.PUBLIC)
				tr.addProperty(p.getName(), ss.getDefinedProperty(p.getName()));
			else if (p.getScope() == Scope.RESTRICTED) {
				if (auth)
					tr.addProperty(p.getName(), ss.getDefinedProperty(p.getName()));
				else
					tr.addPrivateProperty(p.getName());
			} else if (p.getScope() == Scope.PRIVATE)
				tr.addPrivateProperty(p.getName());
		}

		try {
			try {
				ss.assertProperties();

				boolean dependenciesOK = true;
				for (String service : dependencies.keySet()) {
					IDependency dep = dependencies.get(service);
					boolean isPartial = dep.isPartial();
					if (partialMap.containsKey(dep.getService()))
						isPartial = partialMap.get(dep.getService());
					if (partialMap.containsKey(dep.getUrl()))
						isPartial = partialMap.get(dep.getUrl());
					TestResponse tr2 = new TestResponse(dep.getCategory(), dep.getService(), dep.getUrl(), isPartial);
					String ref = dep.getService() + "@" + dep.getUrl();

					long current_time = System.currentTimeMillis();
					long time_left = timeout - current_time + start_time;
					if (time_left < 0L)
						time_left = 0L;
					if (!skipAll && !skip.contains(ref) && (time_left >= dep.getMsMin())) {
						try {
							if (dep instanceof SwaggerServletDependency) {
								StringBuilder sb = new StringBuilder();
								sb.append("?timeout=");
								sb.append(time_left);
								for (String s : skip) {
									if (sb.length() == 0)
										sb.append("?");
									sb.append("&");
									sb.append("skip=");
									sb.append(URLEncoder.encode(s, StandardCharsets.UTF_8.name()));
								}
								SwaggerAsyncResponse<TestResponse> resp = SwaggerCall
										.callAsync("test- " + service, null, "GET",
												dep.getUrl() + "/test" + sb.toString(), null, TestResponse.class)
										.get(time_left / 2, TimeUnit.MILLISECONDS);
								TestResponse r = resp.getRespOrThrowException();
								r.category = dep.getCategory();
								r.service = dep.getService();
								r.url = dep.getUrl();
								r.pass = null;
								tr2 = r;
							} else if (dep instanceof TestableDependency) {
								tr2.available = ((TestableDependency) dep).test();
							}
						} catch (Exception e) {
							tr2.available = false;
							SwaggerUtils.buildSwaggerError(request, e, "testing", dep.getService(), null, tr2, null);
						}
						addToSkipList(skip, tr2, ref);
						if (tr2.available != null && !tr2.available) {
							dependenciesOK = false;
							if (!isPartial) {
								tr.available = false;
								tr.errormsg = tr2.category + ": " + tr2.service + ": " + tr2.errormsg;
							}
						}
					} else {
						tr2.skiped = true;
					}
					tr2.partial = isPartial ? true : null;
					tr2.ms = System.currentTimeMillis() - current_time;
					tr.addDependency(tr2);
				}
				if (tr.available == null)
					tr.available = ss.test();
				if (!tr.available) {
					tr.pass = false;
					// throw new Exception("Test is failing.");
				} else
					tr.pass = true;
			} catch (Exception e) {
				tr.available = false;
				tr.pass = false;
				SwaggerUtils.buildSwaggerError(request, e, "test", ss.getService(), ss.getUser(), tr, null);
				log.error("Error testing swaggerservlet", e);
			}
			try {
				if (tr.pass == null || tr.pass == false)
					response.setStatus(503);
				SwaggerServlet.corsHeaders(request, response);
				SwaggerUtils.writeJsonResp(response, tr, "test", ss.getService());
			} catch (JSONException e) {
				throw new RuntimeException("error reporting test results", e);
			}
		} catch (Exception e) {
			LogResponse lr = new LogResponse();
			lr.method = request.getMethod();
			lr.path = request.getContextPath() + request.getPathInfo();
			String details = SwaggerUtils.toJson(lr);
			log.error("HTTP-ERROR: {}, EXCEPTION", details, e);
		}
	}

	private static void addToSkipList(Set<String> skip, TestResponse tr, String ref) {
		skip.add(ref);
		if (tr.dependencies == null)
			return;
		for (TestResponse tr2 : tr.dependencies)
			addToSkipList(skip, tr2, tr2.service + "@" + tr2.url);
	}

}
