package com.crivano.restservlet;

import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class RestLoggingCallback implements Callback<JsonNode> {
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

	public void completed(HttpResponse<JsonNode> response) {
		JSONObject o = null;
		o = response.getBody().getObject();
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

	public void failed(UnirestException ex) {
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
