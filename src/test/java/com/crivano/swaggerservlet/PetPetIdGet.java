package com.crivano.swaggerservlet;

public class PetPetIdGet implements ISwaggerPetstore.IPetPetIdGet {

	@Override
	public String getContext() {
		return "get color";
	}

	@Override
	public void run(Request req, Response resp, SwaggerPetstoreContext ctx) throws Exception {
		if (req.petId != null && req.petId.equals(123L)) {
			resp.color = "white";
		} else {
			throw new Exception("unknown petId");
		}
	}

}
