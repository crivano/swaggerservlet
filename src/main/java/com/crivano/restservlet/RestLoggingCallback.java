package com.crivano.restservlet;

import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class RestLoggingCallback {
	private RestAsyncCallback callback;
	private JSONObject req;
	private String context;
	private Logger log;

	public RestLoggingCallback(RestAsyncCallback callback, JSONObject req,
			String context, Logger log) {
		this.callback = callback;
		this.req = req;
		this.context = context;
		this.log = log;
	}

	public void completed(RestAsyncResponse response) {
		JSONObject o = null;
		o = response.getJSONObject();
		if (context != null)
			try {
				log.fine(context + " resp: " + o.toString(3));
			} catch (JSONException e) {
			}
		String error = o.optString("error", null);
		if (error != null) {
			try {
				callback.failed(new RestException(error, req, o, context));
			} catch (Exception e) {
			}
		} else
			try {
				callback.completed(o);
			} catch (Exception e) {
			}
	}

	public void failed(Exception ex) {
		String errmsg = RestUtils.messageAsString(ex);
		String errstack = RestUtils.stackAsString(ex);
		RestException wse = new RestException(errmsg, req, null, context);
		try {
			callback.failed(wse);
		} catch (Exception e) {
		}
	}

	public void cancelled() {
		try {
			callback.cancelled();
		} catch (Exception e) {
		}
	}

}
