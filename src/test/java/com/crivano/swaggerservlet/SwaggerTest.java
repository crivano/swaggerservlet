package com.crivano.swaggerservlet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.json.JSONException;
import org.json.JSONObject;

import com.crivano.swaggerservlet.Swagger;

/**
 * Unit test for simple App.
 */
public class SwaggerTest extends TestCase {
	private Swagger sv = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sv = new Swagger();
		sv.loadFromInputStream(this.getClass().getResourceAsStream(
				"swagger.yaml"));
	}

	public SwaggerTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(SwaggerTest.class);
	}

	public void testCheckRequest_NoParameterRequired_Success() throws Exception {
		Swagger.Path path = sv.checkRequestPath("/store/inventory", "get");
		sv.checkRequestParameters(path, null);
	}

	public void testCheckRequest_ParameterInformed_Success() throws Exception {
		ISwaggerPetstore.UserLoginGetRequest req = new ISwaggerPetstore.UserLoginGetRequest();
		req.username = "1";
		req.password = "2";
		Swagger.Path path = sv.checkRequestPath("/user/login", "get");
		sv.checkRequestParameters(path, req);
	}

	public void testCheckRequest_MissingParameter_FailWithRequeredParameterIsMissing()
			throws JSONException {
		ISwaggerPetstore.UserLoginGetRequest req = new ISwaggerPetstore.UserLoginGetRequest();
		req.username = "1";
		try {
			Swagger.Path path = sv.checkRequestPath("/user/login", "get");
			sv.checkRequestParameters(path, req);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("required parameter is missing: password",
					ex.getMessage());
		}
	}

	public void testCheckRequest_NullRequest_FailWithRequeredParameterIsMissing()
			throws JSONException {
		try {
			Swagger.Path path = sv.checkRequestPath("/user/login", "get");
			sv.checkRequestParameters(path, null);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("required parameter is missing: username",
					ex.getMessage());
		}
	}

	public void testCheckRequest_ParhParameterInformed_Success()
			throws Exception {
		ISwaggerPetstore.PetPetIdGetRequest req = new ISwaggerPetstore.PetPetIdGetRequest();
		Swagger.Path path = sv.checkRequestPath("/pet/123", "get");
		sv.checkRequestParameters(path, req);
	}

	public void testCheckRequest_ParhParameterAndFormParameterInformed_Success()
			throws Exception {
		ISwaggerPetstore.PetPetIdPostRequest req = new ISwaggerPetstore.PetPetIdPostRequest();
		req.name = "name";
		Swagger.Path path = sv.checkRequestPath("/pet/123", "post");
		sv.checkRequestParameters(path, req);
	}

	public void testCheckRequest_UnknownPath_FailWithUnknownPath()
			throws JSONException {
		try {
			Swagger.Path path = sv.checkRequestPath("/pet/123/play", "get");
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("unknown path: /pet/123/play", ex.getMessage());
		}
	}

}
