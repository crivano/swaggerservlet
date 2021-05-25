package com.crivano.swaggerservlet;

import org.json.JSONException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class SwaggerTest extends TestCase {
	private Swagger sv = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sv = new Swagger();
		sv.loadFromInputStream(this.getClass().getResourceAsStream("swagger.yaml"));
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
		ISwaggerPetstore.IUserLoginGet.Request req = new ISwaggerPetstore.IUserLoginGet.Request();
		req.username = "1";
		req.password = "2";
		Swagger.Path path = sv.checkRequestPath("/user/login", "get");
		sv.checkRequestParameters(path, req);
	}

	public void testCheckRequest_MissingParameter_FailWithRequeredParameterIsMissing() throws JSONException {
		ISwaggerPetstore.IUserLoginGet.Request req = new ISwaggerPetstore.IUserLoginGet.Request();
		req.username = "1";
		try {
			Swagger.Path path = sv.checkRequestPath("/user/login", "get");
			sv.checkRequestParameters(path, req);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("required parameter is missing: password", ex.getMessage());
		}
	}

	public void testCheckRequest_NullRequest_FailWithRequeredParameterIsMissing() throws JSONException {
		try {
			Swagger.Path path = sv.checkRequestPath("/user/login", "get");
			sv.checkRequestParameters(path, null);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("required parameter is missing: username", ex.getMessage());
		}
	}

	public void testCheckRequest_ParhParameterInformed_Success() throws Exception {
		ISwaggerPetstore.IPetPetIdGet.Request req = new ISwaggerPetstore.IPetPetIdGet.Request();
		Swagger.Path path = sv.checkRequestPath("/pet/123", "get");
		sv.injectPathVariables(req, path);
		sv.checkRequestParameters(path, req);
	}

	public void testCheckRequest_ParhParameterAndFormParameterInformed_Success() throws Exception {
		ISwaggerPetstore.IPetPetIdPost.Request req = new ISwaggerPetstore.IPetPetIdPost.Request();
		req.name = "name";
		Swagger.Path path = sv.checkRequestPath("/pet/123", "post");
		sv.injectPathVariables(req, path);
		sv.checkRequestParameters(path, req);
	}

	public void testCheckRequest_UnknownPath_FailWithUnknownPath() throws JSONException {
		try {
			Swagger.Path path = sv.checkRequestPath("/pet/123/play", "get");
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("unknown path: /pet/123/play", ex.getMessage());
		}
	}

}
