package com.crivano.restservlet;

import org.json.JSONObject;

public interface IRestAction {
	public void run(JSONObject req, JSONObject resp) throws Exception;

	public String getContext();

}
