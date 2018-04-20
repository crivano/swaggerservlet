package com.crivano.swaggerservlet;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
			body = SwaggerUtils.toJson(req);

			con.setRequestProperty("Content-Type", "application/json");
			con.setConnectTimeout(5000); // set timeout to 5 seconds
			con.setReadTimeout(20000); // set read timeout to 20 seconds
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.write(body.getBytes(StandardCharsets.UTF_8));
			wr.flush();
			wr.close();
		}

		int responseCode = con.getResponseCode();

		if (responseCode >= 400 && responseCode < 600) {
			SwaggerError err = (SwaggerError) SwaggerUtils.fromJson(convertStreamToString(con.getErrorStream()),
					SwaggerError.class);
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
			return (T) resp;
		}

		String respString = convertStreamToString(con.getInputStream());
		T resp = (T) SwaggerUtils.fromJson(respString, clazzResp);
		return resp;
	}

}
