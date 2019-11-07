package com.crivano.swaggerservlet;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DefaultHTTP implements IHTTP {
	public static String convertStreamToString(java.io.InputStream is) {
		@SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	@Override
	public <T extends ISwaggerResponse> T fetch(String authorization, String url, String method, ISwaggerRequest req,
			Class<T> clazzResp) throws Exception {
		HttpURLConnection con = null;
		URL obj = new URL(url);
		con = (HttpURLConnection) obj.openConnection();

		con.setRequestProperty("User-Agent", "SwaggerServlets");
		if (authorization != null)
			con.setRequestProperty("Authorization", authorization);
		con.setRequestMethod(method);

		String body = null;

		if (req != null && ("POST".equals(method) || "PUT".equals(method))) {
			if (false) {
				body = SwaggerUtils.toJson(req);
				con.setRequestProperty("Content-Type", "application/json");
			} else {
				StringBuilder sb = new StringBuilder();
				boolean first = true;
				for (Field f : req.getClass().getDeclaredFields()) {
					f.setAccessible(true);
					Object v = f.get(req);
					if (v == null)
						continue;
					if (v instanceof Date)
						v = SwaggerUtils.format((Date) v);
					sb.append(first ? "" : "&");
					first = false;
					sb.append(URLEncoder.encode(f.getName()));
					sb.append("=");
					sb.append(URLEncoder.encode(v.toString(), "UTF-8"));
				}
				body = sb.toString();
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			}
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			con.setRequestProperty("Content-Length", Integer.toString(bytes.length));
			con.setRequestProperty("charset", "utf-8");

			con.setConnectTimeout(5000); // set timeout to 5 seconds
			con.setReadTimeout(20000); // set read timeout to 20 seconds
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.write(bytes);
			wr.flush();
			wr.close();
		}

		int responseCode = con.getResponseCode();

		if (responseCode >= 400 && responseCode < 600) {
			SwaggerError err = null;
			InputStream errorStream = null;
			String string = null;
			errorStream = con.getErrorStream();
			if (errorStream != null)
				string = convertStreamToString(errorStream);
			err = (SwaggerError) SwaggerUtils.fromJson(string, SwaggerError.class);
			String errormsg = "HTTP ERROR: " + Integer.toString(responseCode);
			if (con.getResponseMessage() != null)
				errormsg = errormsg + " - " + con.getResponseMessage();
			if (err != null && err.errormsg != null)
				errormsg = err.errormsg;
			errormsg = errormsg.replaceAll("\\s+", " ");
			throw new SwaggerException(errormsg, responseCode,
					new Exception("calling webservice "
							+ clazzResp.getName().replaceAll("Response$", "").replaceAll("^.+\\.", "")),
					req, err, null);
		}

		if (ISwaggerResponseFile.class.isAssignableFrom(clazzResp)) {
			ISwaggerResponseFile resp = (ISwaggerResponseFile) SwaggerUtils.fromJson("{}", clazzResp);
			resp.setContentlength((long) con.getContentLength());
			resp.setContenttype(con.getContentType());
			resp.setInputstream(con.getInputStream());
			resp.setHeaderFields(con.getHeaderFields());
			return (T) resp;
		}

		String respString = convertStreamToString(con.getInputStream());
		T resp = (T) SwaggerUtils.fromJson(respString, clazzResp);
		return resp;
	}
}
