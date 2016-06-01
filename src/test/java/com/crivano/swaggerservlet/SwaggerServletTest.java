package com.crivano.swaggerservlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;

import com.crivano.swaggerservlet.Swagger;
import com.crivano.swaggerservlet.SwaggerServlet;

/**
 * Unit test for simple App.
 */
public class SwaggerServletTest extends TestCase {
	private SwaggerServlet ss = null;
	private Swagger sv = null;

	@SuppressWarnings("serial")
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ss = new SwaggerServlet() {
			@Override
			protected String getContext() {
				return "test";
			}
		};
		sv = new Swagger();
		sv.loadFromInputStream(this.getClass().getResourceAsStream(
				"swagger.yaml"));

		ss.setSwagger(sv);
		ss.setActionPackage("com.crivano.swaggerservlet");
	}

	public void testCamelCase_Simple_Success() throws JSONException {
		assertEquals("PetPetId", ss.toCamelCase("/pet/{petId}"));
	}

	public void testAction_Simple_Success() throws Exception {
		JSONObject req = new JSONObject();
		JSONObject resp = new JSONObject();
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/v2/pet/123");

		ss.prepare(request, null, req, resp);
		ss.run(request, null, req, resp);

		assertEquals("{\"color\":\"white\"}", resp.toString());
	}

	public void testAction_SimpleException_FailWithUnknownId() throws Exception {
		JSONObject req = new JSONObject();
		JSONObject resp = new JSONObject();
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/v2/pet/456");

		try {
			ss.prepare(request, null, req, resp);
			ss.run(request, null, req, resp);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("unknown petId", ex.getMessage());
		}
	}

	public void testGet_Simple_Success() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/v2/pet/123");

		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.getWriter()).thenReturn(pw);

		ss.doGet(request, response);

		JSONObject resp = new JSONObject(sw.toString());

		assertEquals("white", resp.get("color"));
	}

	public void testGet_SimpleException_FailWithUnknownId() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/v2/pet/456");

		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.getWriter()).thenReturn(pw);

		ss.doGet(request, response);

		JSONObject resp = new JSONObject(sw.toString());

		JSONObject errordetails = resp.getJSONArray("errordetails")
				.getJSONObject(0);

		assertEquals("unknown petId", resp.get("error"));
		assertEquals("test", errordetails.get("context"));
		assertEquals("Swagger Petstore", errordetails.get("service"));
		assertNotNull(errordetails.get("stacktrace"));
	}
}
