package com.crivano.swaggerservlet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public class Swagger {
	private Map swagger = null;
	private List<Pattern> regexs = null;
	private List<String> swaggerPaths = null;

	public void loadFromInputStream(InputStream is) {
		Yaml yaml = new Yaml();
		this.swagger = (Map) yaml.load(is);
		deference();
		buildRegexs();
	}

	public void deference() {
		Map<String, Object> paths = (Map<String, Object>) this.swagger
				.get("paths");

		for (String key : paths.keySet()) {
			Map<String, Object> path = (Map<String, Object>) paths.get(key);
			for (String funcKey : path.keySet()) {
				Map<String, Object> func = (Map<String, Object>) path
						.get(funcKey);
				List<Map<String, Object>> parameters = (List<Map<String, Object>>) func
						.get("parameters");
				for (int i = 0; i < parameters.size(); i++) {
					Map<String, Object> param = parameters.get(i);
					if (param.containsKey("$ref")) {
						String ref = (String) (Object) param.get("$ref");
						parameters.set(i, swaggerGetReference(ref));
					}
				}
			}
		}
	}

	public void buildRegexs() {
		regexs = new ArrayList<>();
		swaggerPaths = new ArrayList<>();

		Map<String, Object> paths = (Map<String, Object>) this.swagger
				.get("paths");

		for (String pathKey : paths.keySet()) {
			String s = "^"
					+ pathKey.replaceAll("\\{([^\\}]+)\\}", "(?<$1>[^/]+)")
					+ "$";
			regexs.add(Pattern.compile(s));
			swaggerPaths.add(pathKey);
		}
	}

	private Map<String, Object> swaggerGetReference(String ref) {
		if (!ref.startsWith("#/"))
			throw new RuntimeException("invalid reference: " + ref);
		ref = ref.substring(2);
		String[] a = ref.split("/");

		Map<String, Object> m = this.swagger;
		for (String s : a) {
			m = (Map<String, Object>) m.get(s);
		}
		return m;
	}

	public String checkRequest(String path, String method, JSONObject req) {
		String basePath = (String) this.swagger.get("basePath");
		
		if (path == null)
			throw new RuntimeException("path can't be null");

		if (basePath != null && path.startsWith(basePath))
			path = path.substring(basePath.length());

		// Replace path with a Swagger like path
		String swaggerPath = null;
		int i = 0;
		for (Pattern p : regexs) {
			Matcher m = p.matcher(path);
			if (m.matches()) {
				swaggerPath = swaggerPaths.get(i);
				injectPathVariables(req, swaggerPath, m);
				break;
			}
			i++;
		}
		if (swaggerPath == null)
			throw new RuntimeException("unknown path: " + path);

		Map<String, Object> paths = (Map<String, Object>) this.swagger
				.get("paths");
		for (String pathKey : paths.keySet()) {
			if (!pathKey.equals(swaggerPath))
				continue;
			Map<String, Object> func = (Map<String, Object>) ((Map<String, Object>) paths
					.get(pathKey)).get(method);
			if (func == null)
				continue;

			checkParams(func, req);
			return pathKey;
		}
		throw new RuntimeException("path/method undefined: " + swaggerPath
				+ "/" + method);
	}

	/**
	 * Put path variables at the req, if not there yet.
	 * 
	 * @param req
	 * @param swaggerPath
	 * @param m
	 */
	public void injectPathVariables(JSONObject req, String swaggerPath,
			Matcher m) {
		Pattern pv = Pattern.compile("\\{(?<var>[^\\}]+)\\}");
		Matcher mv = pv.matcher(swaggerPath);
		while (mv.find()) {
			String var = mv.group("var");
			String value = m.group(var);
			if (!req.has(var)) {
				try {
					req.put(var, value);
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void checkParams(Map<String, Object> func, JSONObject req) {
		List<Map<String, Object>> parameters = (List<Map<String, Object>>) func
				.get("parameters");
		if (parameters == null)
			return;
		for (Map<String, Object> param : parameters) {
			if (param.containsKey("required")
					&& (Boolean) param.get("required")
					&& (req == null || !req.has((String) param.get("name")))) {
				throw new RuntimeException("required parameter is missing: "
						+ param.get("name"));
			}
		}
	}

	public String getInfoTitle() {
		return ((Map<String, Map<String, String>>) swagger).get("info").get(
				"title");
	}
}
