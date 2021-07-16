package com.crivano.swaggerservlet.test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.swaggerservlet.Property;
import com.crivano.swaggerservlet.Property.Scope;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerCallParameters;
import com.crivano.swaggerservlet.SwaggerCallStatus;
import com.crivano.swaggerservlet.SwaggerError.Detail;
import com.crivano.swaggerservlet.SwaggerMultipleCallResult;
import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;
import com.crivano.swaggerservlet.dependency.IDependency;
import com.crivano.swaggerservlet.dependency.SwaggerServletDependency;
import com.crivano.swaggerservlet.dependency.TestableDependency;

public class Test {
	private static final long DEFAULT_TIMEOUT_MILLISECONDS = 10000L;
	private static final long DEFAULT_TIMEOUT_MINUS_MILLISECONDS = 500L;
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

		// Begin counting the time
		long dt1 = System.currentTimeMillis();

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
			ss.assertProperties();

			boolean dependenciesOK = true;
			Map<String, SwaggerCallParameters> mapp = new HashMap<>();
			Map<String, IDependency> mapt = new HashMap<>();
			for (String service : dependencies.keySet()) {
				IDependency dep = dependencies.get(service);
				String ref = buildRef(dep);

				if (!skipAll && !skip.contains(ref)) {
					if (timeout >= dep.getMsMin()) {
						// Add to a multiple call
						if (dep instanceof SwaggerServletDependency) {
							StringBuilder sb = new StringBuilder();
							sb.append("?timeout=");
							sb.append(timeout);
							for (String s : skip) {
								if (sb.length() == 0)
									sb.append("?");
								sb.append("&");
								sb.append("skip=");
								sb.append(URLEncoder.encode(s, StandardCharsets.UTF_8.name()));
							}
							mapp.put(service, new SwaggerCallParameters("test- " + service, null, "GET",
									dep.getUrl() + "/test" + sb.toString(), null, TestResponse.class));

						} else if (dep instanceof TestableDependency) {
							// Add to a executor for testing
							mapt.put(service, dep);
//								 {
//								tr2.available = ((TestableDependency) dep).test();
						}
					} else {
						// If we are not going to test, than add an skipped response
						TestResponse tr2 = new TestResponse(dep.getCategory(), dep.getService(), dep.getUrl(),
								isPartial(dep, partialMap));
						tr2.skiped = true;
						tr.addDependency(tr2);
						addToSkipList(skip, null, ref);
					}
				}
			}

			// Add testable dependencies to executor
			Map<String, Future<TestResponse>> map = new HashMap<>();
			for (String service : mapt.keySet()) {
				TestableDependency dep = (TestableDependency) dependencies.get(service);
				TestResponse tr2 = new TestResponse(dep.getCategory(), dep.getService(), dep.getUrl(),
						isPartial(dep, partialMap));
				map.put(service, SwaggerServlet.executor.submit(new TestAsync(dep, tr2)));
			}

			// Add SwaggerServlet Dependencies
			SwaggerMultipleCallResult mcr = SwaggerCall.callMultiple(mapp, timeout);
			for (SwaggerCallStatus sts : mcr.status) {
				IDependency dep = dependencies.get(sts.system);
				TestResponse r;
				r = (TestResponse) mcr.responses.get(sts.system);
				if (r == null) {
					r = new TestResponse(dep.getCategory(), dep.getService(), dep.getUrl(), isPartial(dep, partialMap));
					r.pass = null;
					r.available = false;
					r.errormsg = sts.errormsg;
					r.errordetails = new ArrayList<>();
					Detail det = new Detail();
					det.stacktrace = sts.stacktrace;
					r.errordetails.add(det);
				}
				r.category = dep.getCategory();
				r.service = dep.getService();
				r.url = dep.getUrl();
				r.partial = isPartial(dep, partialMap);
				r.ms = sts.miliseconds;
				tr.addDependency(r);
				addToSkipList(skip, r, buildRef(dep));
			}

			// Take the remaining time to wait for testable dependencies
			for (String service : mapt.keySet()) {
				long time = System.currentTimeMillis();
				try {
					long timeRemaining = timeout - (time - dt1);
					if (timeRemaining < 0L)
						timeRemaining = 0; // minimum timeout
					TestResponse r = map.get(service).get(timeRemaining, TimeUnit.MILLISECONDS);
					tr.addDependency(r);
				} catch (Exception ex) {
					IDependency dep = mapt.get(service);
					TestResponse r = new TestResponse(dep.getCategory(), dep.getService(), dep.getUrl(),
							isPartial(dep, partialMap));
					SwaggerUtils.buildSwaggerError(request, ex, "test", ss.getService(), ss.getUser(), r, null);
					r.available = false;
					r.ms = time - dt1;
					tr.addDependency(r);
					if (ex instanceof TimeoutException)
						map.get(service).cancel(true);
				}
			}

			// Compute availability
			if (tr.dependencies != null)
				for (TestResponse r : tr.dependencies) {
					if (r.available != null && !r.available) {
						dependenciesOK = false;
						if (!r.partial) {
							tr.available = false;
							tr.errormsg = r.category + ": " + r.service + ": " + r.errormsg;
							break;
						} else {
							tr.partial = true;
						}
					}
				}

			if (tr.available == null)
				tr.available = ss.test();
			tr.pass = tr.available;
		} catch (Exception e) {
			tr.available = false;
			tr.pass = false;
			SwaggerUtils.buildSwaggerError(request, e, "test", ss.getService(), ss.getUser(), tr, null);
			log.error("Error testing swaggerservlet", e);
		}
		tr.ms = System.currentTimeMillis() - dt1;
		try {
			if (tr.pass == null || tr.pass == false)
				response.setStatus(242);
			SwaggerServlet.corsHeaders(request, response);
			SwaggerUtils.writeJsonResp(response, tr, "test", ss.getService());
		} catch (Exception e) {
			throw new RuntimeException("error reporting test results", e);
		}
	}

	private static String buildRef(IDependency dep) {
		return dep.getService() + "@" + dep.getUrl();
	}

	private static boolean isPartial(IDependency dep, Map<String, Boolean> partialMap) {
		boolean isPartial = dep.isPartial();

		if (partialMap.containsKey(dep.getService()))
			isPartial = partialMap.get(dep.getService());
		if (partialMap.containsKey(dep.getUrl()))
			isPartial = partialMap.get(dep.getUrl());
		return isPartial;
	}

	private static void addToSkipList(Set<String> skip, TestResponse tr, String ref) {
		skip.add(ref);
		if (tr == null || tr.dependencies == null)
			return;
		for (TestResponse tr2 : tr.dependencies)
			addToSkipList(skip, tr2, tr2.service + "@" + tr2.url);
	}

}
