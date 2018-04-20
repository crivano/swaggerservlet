package com.crivano.swaggerservlet;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public class Swagger {
	private Map swagger = null;
	private List<Pattern> regexs = null;
	private List<String> swaggerPaths = null;
	private String interfaceName = null;
	private String interfacePackage = null;

	public void loadFromInputStream(InputStream is) {
		Yaml yaml = new Yaml();
		this.swagger = (Map) yaml.load(is);
		deference();
		buildRegexs();
	}

	public void deference() {
		Map<String, Object> paths = (Map<String, Object>) this.swagger.get("paths");

		for (String key : paths.keySet()) {
			Map<String, Object> path = (Map<String, Object>) paths.get(key);
			for (String funcKey : path.keySet()) {
				Map<String, Object> func = (Map<String, Object>) path.get(funcKey);
				List<Map<String, Object>> parameters = (List<Map<String, Object>>) func.get("parameters");
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

		Map<String, Object> paths = (Map<String, Object>) this.swagger.get("paths");

		for (String pathKey : paths.keySet()) {
			String s = "^" + pathKey.replaceAll("\\{([^\\}]+)\\}", "(?<$1>[^/]+)") + "$";
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

	public class Path {
		String swaggerPath;
		String path;
		String method;
		Matcher matcher;
	}

	public Path checkRequestPath(String path, String method) {
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
				Path pth = new Path();
				pth.path = path;
				pth.method = method;
				pth.swaggerPath = swaggerPath;
				pth.matcher = m;
				return pth;
			}
			i++;
		}
		throw new RuntimeException("unknown path: " + path);
	}

	public void checkRequestParameters(Swagger.Path path, ISwaggerRequest req) throws Exception {
		Map<String, Object> paths = (Map<String, Object>) this.swagger.get("paths");
		for (String pathKey : paths.keySet()) {
			if (!pathKey.equals(path.swaggerPath))
				continue;
			Map<String, Object> func = (Map<String, Object>) ((Map<String, Object>) paths.get(pathKey))
					.get(path.method);
			if (func == null)
				continue;

			checkParams(func, req);
			return;
		}
		throw new RuntimeException("path/method undefined: " + path.swaggerPath + "/" + path.method);
	}

	/**
	 * Put path variables at the req, if not there yet.
	 * 
	 * @param req
	 * @param swaggerPath
	 * @param m
	 * @throws Exception
	 */
	public void injectPathVariables(ISwaggerRequest req, Path path) throws Exception {
		Pattern pv = Pattern.compile("\\{(?<var>[^\\}]+)\\}");
		Matcher mv = pv.matcher(path.swaggerPath);
		while (mv.find()) {
			String var = mv.group("var");
			String value = path.matcher.group(var);
			if (!has(req, var)) {
				try {
					set(req, var, value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void checkParams(Map<String, Object> func, ISwaggerRequest req) throws Exception {
		List<Map<String, Object>> parameters = (List<Map<String, Object>>) func.get("parameters");
		if (parameters == null)
			return;
		for (Map<String, Object> param : parameters) {
			if (param.containsKey("required") && (Boolean) param.get("required")
					&& (req == null || !has(req, (String) param.get("name")))) {
				throw new RuntimeException("required parameter is missing: " + param.get("name"));
			}
		}
	}

	public String getInfoTitle() {
		return ((Map<String, Map<String, String>>) swagger).get("info").get("title");
	}

	public String create(boolean singleLine) {
		StringBuilder sb = new StringBuilder();
		String title = toCamelCase((String) ((Map<String, String>) this.swagger.get("info")).get("title"));
		sb.append("public interface I");
		sb.append(title);
		sb.append(" {\n");

		Map<String, Object> definitions = (Map<String, Object>) this.swagger.get("definitions");

		// Object Definitions
		for (String definitionKey : definitions.keySet()) {
			Map<String, Object> definition = (Map<String, Object>) definitions.get(definitionKey);
			appendClass(sb, definitionKey + " implements ISwaggerModel", definition, null);
		}

		Map<String, Object> paths = (Map<String, Object>) this.swagger.get("paths");

		for (String pathKey : paths.keySet()) {
			Map<String, Object> path = (Map<String, Object>) paths.get(pathKey);
			for (String funcKey : path.keySet()) {
				Map<String, Object> func = (Map<String, Object>) path.get(funcKey);
				String method = toCamelCase(pathKey + " " + funcKey);

				// Request
				List<Map<String, Object>> parameters = (List<Map<String, Object>>) func.get("parameters");
				sb.append("\tpublic class ");
				sb.append(method);
				sb.append("Request implements ISwaggerRequest {\n");
				for (int i = 0; i < parameters.size(); i++) {
					Map<String, Object> param = parameters.get(i);
					sb.append("\t\tpublic ");
					sb.append(toJavaType((String) param.get("type"), (String) param.get("format"), null));
					sb.append(" ");
					sb.append(param.get("name"));
					sb.append(";\n");
				}
				sb.append("\t}\n\n");

				// Response
				Map<String, Object> r200 = getSuccessfulResponse(func);
				Map<String, Object> schema = null;
				if (r200 != null)
					schema = (Map<String, Object>) r200.get("schema");
				if (schema == null) {
					schema = new HashMap<>();
					schema.put("type", "object");
					schema.put("properties", new HashMap<String, Object>());
				}
				Map<String, Map<String, Object>> headers = (Map<String, Map<String, Object>>) r200.get("headers");

				appendClass(sb, method + "Response implements ISwaggerResponse", schema, headers);

				// Single method interface
				sb.append("\tpublic interface I");
				sb.append(method);
				sb.append(" extends ISwaggerMethod {\n");
				sb.append("\t\tpublic void run(");
				sb.append(method);
				sb.append("Request req, ");
				sb.append(method);
				sb.append("Response resp) throws Exception;\n");
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
		Map<String, Object> r200 = ((Map<String, Map<String, Object>>) func.get("responses")).get(200);
		if (r200 == null)
			r200 = ((Map<String, Map<String, Object>>) func.get("responses")).get("200");
		if (r200 == null)
			r200 = ((Map<String, Map<String, Object>>) func.get("responses")).get(204);
		if (r200 == null)
			r200 = ((Map<String, Map<String, Object>>) func.get("responses")).get("204");
		return r200;
	}

	private void appendClass(StringBuilder sb, String className, Map<String, Object> definition,
			Map<String, Map<String, Object>> headers) {
		Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) definition.get("properties");
		String typeOnly = (String) definition.get("type");
		boolean fileResponse = typeOnly != null && "file".equals(typeOnly);
		boolean payloadResponse = properties != null && properties.containsKey("payload")
				&& properties.containsKey("contenttype");

		sb.append("\tpublic class ");
		sb.append(className + (fileResponse ? ", ISwaggerResponseFile" : "")
				+ (payloadResponse ? ", ISwaggerResponsePayload" : ""));
		sb.append(" {\n");

		if (fileResponse) {
			sb.append("\t\tpublic String contenttype");
			if (headers != null) {
				String contenttype = headers.get("Content-Type") == null ? null
						: (String) headers.get("Content-Type").get("description");
				if (contenttype != null)
					sb.append(" = \"" + contenttype + "\"");
			}
			sb.append(";\n");
			sb.append("\t\tpublic String contentdisposition");
			if (headers != null) {
				String contentdisposition = headers.get("Content-Disposition") == null ? null
						: (String) headers.get("Content-Disposition").get("description");
				if (contentdisposition != null)
					sb.append(" = \"" + contentdisposition + "\"");
			}
			sb.append(";\n\n");
			sb.append("\t\tpublic Long contentlength;\n");
			sb.append("\t\tpublic InputStream inputstream;\n");

			sb.append("\t\tpublic String getContenttype() {\n");
			sb.append("\t\t\treturn contenttype;\n");
			sb.append("\t\t}\n");
			sb.append("\t\tpublic void setContenttype(String contenttype) {\n");
			sb.append("\t\t\tthis.contenttype = contenttype;\n");
			sb.append("\t\t}\n");
			sb.append("\t\tpublic String getContentdisposition() {\n");
			sb.append("\t\t\treturn contentdisposition;\n");
			sb.append("\t\t}\n");
			sb.append("\t\tpublic void setContentdisposition(String contentdisposition) {\n");
			sb.append("\t\t\tthis.contentdisposition = contentdisposition;\n");
			sb.append("\t\t}\n");
			sb.append("\t\tpublic Long getContentlength() {\n");
			sb.append("\t\t\treturn contentlength;\n");
			sb.append("\t\t}\n");
			sb.append("\t\tpublic void setContentlength(Long contentlength) {\n");
			sb.append("\t\t\tthis.contentlength = contentlength;\n");
			sb.append("\t\t}\n");
			sb.append("\t\tpublic InputStream getInputstream() {\n");
			sb.append("\t\t\treturn inputstream;\n");
			sb.append("\t\t}\n");
			sb.append("\t\tpublic void setInputstream(InputStream inputstream) {\n");
			sb.append("\t\t\tthis.inputstream = inputstream;\n");
			sb.append("\t\t}\n");
		} else if (properties != null) {
			for (String propertyKey : properties.keySet()) {
				Map<String, Object> property = (Map<String, Object>) properties.get(propertyKey);
				String typename = null;
				if (property.containsKey("$ref")) {
					String ref = (String) (Object) property.get("$ref");
					property = swaggerGetDefinition(ref);
					typename = swaggerGetDefinitionName(ref);
				}
				sb.append("\t\tpublic ");
				String type = (String) property.get("type");
				String format = (String) property.get("format");
				if ("array".equals(type)) {
					Map<String, Object> items = (Map<String, Object>) property.get("items");
					if (items.containsKey("$ref")) {
						String ref = (String) (Object) items.get("$ref");
						typename = swaggerGetDefinitionName(ref);
					}
				} else if ("object".equals(type)) {
					Map<String, Object> objectproperties = (Map<String, Object>) property.get("properties");
					if (objectproperties.containsKey("$ref")) {
						String ref = (String) (Object) objectproperties.get("$ref");
						typename = swaggerGetDefinitionName(ref);
					}
				}
				sb.append(toJavaType(type, format, typename));
				sb.append(" ");
				sb.append(propertyKey);
				sb.append(";\n");
			}
		}
		sb.append("\t}\n\n");
		return;
	}

	private String toJavaType(String type, String format, String typename) {
		if (type == null)
			return type;
		switch (type) {
		case "string":
			if ("byte".equals(format))
				return "byte[]";
			if ("date".equals(format))
				return "Date";
			if ("date-time".equals(format))
				return "Date";
			return "String";
		case "boolean":
			return "Boolean";
		case "integer":
			if ("int32".equals(format))
				return "Integer";
			return "Long";
		case "number":
			if ("float".equals(format))
				return "Float";
			return "Double";
		case "null":
			return "String";
		case "object":
			return typename;
		case "array":
			return "List<" + typename + ">";
		default:
			return type;
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

	public static boolean has(ISwaggerModel model, String param) throws Exception {
		return get(model, param) != null;
	}

	public static Object get(ISwaggerModel model, String param) throws Exception {
		Class<? extends ISwaggerModel> clazz = model.getClass();

		Field field;
		try {
			field = clazz.getDeclaredField(param);
		} catch (NoSuchFieldException ex) {
			throw new Exception("unknown parameter: " + param, ex);
		}
		field.setAccessible(true);
		return field.get(model);
	}

	public static void set(ISwaggerModel model, String param, String value) throws Exception {
		Class<? extends ISwaggerModel> clazz = model.getClass();

		Field field;
		try {
			field = clazz.getDeclaredField(param);
		} catch (NoSuchFieldException ex) {
			throw new Exception("unknown parameter: " + param, ex);
		}
		Object v = value;
		if (field.getType() == Long.class)
			v = new Long(value);
		if (field.getType() == Boolean.class)
			v = new Boolean(value);
		field.setAccessible(true);
		field.set(model, v);
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getInterfacePackage() {
		return interfacePackage;
	}

	public void setInterfacePackage(String interfacePackage) {
		this.interfacePackage = interfacePackage;
	}
}
