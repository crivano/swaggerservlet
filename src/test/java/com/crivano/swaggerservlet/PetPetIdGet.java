package com.crivano.swaggerservlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;

public class PetPetIdGet implements IRestAction {

	@Override
	public void run(HttpServletRequest request, HttpServletResponse response,
			JSONObject req, JSONObject resp) throws Exception {
		if ("123".equals(req.getString("petId"))) {
			resp.put("color", "white");
		} else {
			throw new Exception("unknown petId");
		}
	}

	@Override
	public String getContext() {
		return "get color";
	}

}
