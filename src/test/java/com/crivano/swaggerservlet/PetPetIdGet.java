package com.crivano.swaggerservlet;

import com.crivano.swaggerservlet.ISwaggerPetstore.PetPetIdGetRequest;
import com.crivano.swaggerservlet.ISwaggerPetstore.PetPetIdGetResponse;

public class PetPetIdGet implements ISwaggerPetstore.IPetPetIdGet {

	@Override
	public String getContext() {
		return "get color";
	}

	@Override
	public void run(PetPetIdGetRequest req, PetPetIdGetResponse resp)
			throws Exception {
		if (Long.getLong("123").equals(req.petId)) {
			resp.color = "white";
		} else {
			throw new Exception("unknown petId");
		}
	}

}
