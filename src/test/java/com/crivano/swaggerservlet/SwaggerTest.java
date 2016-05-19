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

	public void testCheckRequest_NoParameterRequired_Success()
			throws JSONException {
		sv.checkRequest("/v2/store/inventory", "get", null);
	}

	public void testCheckRequest_ParameterInformed_Success()
			throws JSONException {
		JSONObject req = new JSONObject();
		req.put("username", "1");
		req.put("password", "2");
		sv.checkRequest("/v2/user/login", "get", req);
	}

	public void testCheckRequest_MissingParameter_FailWithRequeredParameterIsMissing()
			throws JSONException {
		JSONObject req = new JSONObject();
		req.put("username", "1");
		try {
			sv.checkRequest("/v2/user/login", "get", req);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("required parameter is missing: password",
					ex.getMessage());
		}
	}

	public void testCheckRequest_NullRequest_FailWithRequeredParameterIsMissing()
			throws JSONException {
		try {
			sv.checkRequest("/v2/user/login", "get", null);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("required parameter is missing: username",
					ex.getMessage());
		}
	}

	public void testCheckRequest_ParhParameterInformed_Success()
			throws JSONException {
		JSONObject req = new JSONObject();
		sv.checkRequest("/v2/pet/123", "get", req);
	}

	public void testCheckRequest_ParhParameterAndFormParameterInformed_Success()
			throws JSONException {
		JSONObject req = new JSONObject();
		req.put("name", "name");
		sv.checkRequest("/v2/pet/123", "post", req);
	}

	public void testCheckRequest_UnknownPath_FailWithUnknownPath()
			throws JSONException {
		try {
			sv.checkRequest("/v2/pet/123/play", "get", null);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("unknown path: /pet/123/play", ex.getMessage());
		}
	}

}
