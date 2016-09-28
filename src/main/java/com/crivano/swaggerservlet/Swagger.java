package com.crivano.swaggerservlet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
	
	public String create(String packageName, boolean singleLine) {
		StringBuilder sb = new StringBuilder();
		if (packageName != null) {
			sb.append("package ");
			sb.append(packageName);
			sb.append(";\n\n");
			sb.append("import java.util.List;");
			sb.append("\n\n");
		}
		String title = toCamelCase((String) ((Map<String, String>) this.swagger
				.get("info")).get("title"));
		sb.append("interface I");
		sb.append(title);
		sb.append(" {\n");

		Map<String, Object> definitions = (Map<String, Object>) this.swagger
				.get("definitions");

		// Object Definitions
		for (String definitionKey : definitions.keySet()) {
			Map<String, Object> definition = (Map<String, Object>) definitions
					.get(definitionKey);
			appendClass(sb, definitionKey, definition);
		}

		Map<String, Object> paths = (Map<String, Object>) this.swagger
				.get("paths");

		for (String pathKey : paths.keySet()) {
			Map<String, Object> path = (Map<String, Object>) paths.get(pathKey);
			for (String funcKey : path.keySet()) {
				Map<String, Object> func = (Map<String, Object>) path
						.get(funcKey);
				String method = toCamelCase(pathKey + " " + funcKey);

				// Request
				List<Map<String, Object>> parameters = (List<Map<String, Object>>) func
						.get("parameters");
				sb.append("\tclass ");
				sb.append(method);
				sb.append("Request implements ISwaggerRequest {\n");
				for (int i = 0; i < parameters.size(); i++) {
					Map<String, Object> param = parameters.get(i);
					sb.append("\t\t");
					sb.append(toJavaType((String) param.get("type"), null));
					sb.append(" ");
					sb.append(param.get("name"));
					sb.append(";\n");
				}
				sb.append("\t}\n\n");

				// Response
				Map<String, Object> r200 = getSuccessfulResponse(func);
				Map<String, Object> schema = null;
				schema = (Map<String, Object>) r200.get("schema");
				if (schema == null) {
					schema = new HashMap<>();
					schema.put("type", "object");
					schema.put("properties", new HashMap<String, Object>());
				}
				appendClass(sb,
						method + "Response implements ISwaggerResponse", schema);

				// Single method interface
				sb.append("\tinterface I");
				sb.append(method);
				sb.append(" extends ISwaggerMethod {\n");
				sb.append("\t\tvoid run(");
				sb.append(method);
				sb.append("Request req, ");
				sb.append(method);
				sb.append("Response resp);\n");
				sb.append("\t}\n\n");
			}
		}
		sb.append("}");
		if (singleLine) {
			return sb.toString().replace("\t", "").replace("\n", "");
		}
		return sb.toString();
	}

	private Map<String, Object> getSuccessfulResponse(Map<String, Object> func) {
		Map<String, Object> r200 = ((Map<String, Map<String, Object>>) func
				.get("responses")).get(200);
		if (r200 == null)
			r200 = ((Map<String, Map<String, Object>>) func.get("responses"))
					.get("200");
		if (r200 == null)
			r200 = ((Map<String, Map<String, Object>>) func.get("responses"))
					.get(204);
		if (r200 == null)
			r200 = ((Map<String, Map<String, Object>>) func.get("responses"))
					.get("204");
		return r200;
	}

	private void appendClass(StringBuilder sb, String className,
			Map<String, Object> definition) {
		if (!"object".equals(definition.get("type")))
			return;
		Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) definition
				.get("properties");
		sb.append("\tclass ");
		sb.append(className);
		sb.append(" {\n");
		if (properties != null) {
			for (String propertyKey : properties.keySet()) {
				Map<String, Object> property = (Map<String, Object>) properties
						.get(propertyKey);
				String typename = null;
				if (property.containsKey("$ref")) {
					String ref = (String) (Object) property.get("$ref");
					property = swaggerGetDefinition(ref);
					typename = swaggerGetDefinitionName(ref);
				}
				sb.append("\t\t");
				String type = (String) property.get("type");
				if ("array".equals(type)) {
					Map<String, Object> items = (Map<String, Object>) property
							.get("items");
					if (items.containsKey("$ref")) {
						String ref = (String) (Object) items.get("$ref");
						typename = swaggerGetDefinitionName(ref);
					}
				} else if ("object".equals(type)) {
					Map<String, Object> objectproperties = (Map<String, Object>) property
							.get("properties");
					if (objectproperties.containsKey("$ref")) {
						String ref = (String) (Object) objectproperties
								.get("$ref");
						typename = swaggerGetDefinitionName(ref);
					}
				}
				sb.append(toJavaType(type, typename));
				sb.append(" ");
				sb.append(propertyKey);
				sb.append(";\n");
			}
		}
		sb.append("\t}\n\n");
		return;
	}

	private String toJavaType(String s, String typename) {
		if (s == null)
			return s;
		switch (s) {
		case "string":
			return "String";
		case "boolean":
			return "boolean";
		case "integer":
			return "long";
		case "number":
			return "double";
		case "null":
			return "String";
		case "object":
			return typename;
		case "array":
			return "List<" + typename + ">";
		default:
			return s;
		}
	}

	private Map<String, Object> swaggerGetDefinition(String ref) {
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

	private String swaggerGetDefinitionName(String ref) {
		if (!ref.startsWith("#/"))
			throw new RuntimeException("invalid reference: " + ref);
		ref = ref.substring(2);
		String[] a = ref.split("/");
		return a[a.length - 1];
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
}
