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

import com.crivano.swaggerservlet.ISwaggerPetstore.PetPetIdGetRequest;

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
		ss.setAPI(ISwaggerPetstore.class);
		ss.setActionPackage("com.crivano.swaggerservlet");
	}

	public void testCamelCase_Simple_Success() throws JSONException {
		assertEquals("PetPetId", ss.toCamelCase("/pet/{petId}"));
	}

	public void testAction_Simple_Success() throws Exception {
		ISwaggerPetstore.PetPetIdGetRequest req = new ISwaggerPetstore.PetPetIdGetRequest();
		ISwaggerPetstore.PetPetIdGetResponse resp = new ISwaggerPetstore.PetPetIdGetResponse();
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/v2/pet/123");

		ss.prepare(request, null);
		req = (PetPetIdGetRequest) ss.injectVariables(request, req);
		ss.run(req, resp);

		assertEquals("white", resp.color);
	}

	public void testAction_SimpleException_FailWithUnknownId() throws Exception {
		ISwaggerPetstore.PetPetIdGetRequest req = new ISwaggerPetstore.PetPetIdGetRequest();
		ISwaggerPetstore.PetPetIdGetResponse resp = new ISwaggerPetstore.PetPetIdGetResponse();
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/v2/pet/456");

		try {
			ss.prepare(request, null);
			ss.run(req, resp);
			assertTrue(false);
		} catch (Exception ex) {
			assertEquals("unknown petId", ex.getMessage());
		}
	}

	public void testGet_Simple_Success() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getProtocol()).thenReturn("HTTP 1.1");
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/v2/pet/123");

		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.getWriter()).thenReturn(pw);

		ss.doGet(request, response);

		String body = sw.toString();

		JSONObject resp = new JSONObject(body);

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

		assertEquals("unknown petId", resp.get("errormsg"));
		assertEquals("test", errordetails.get("context"));
		assertEquals("Swagger Petstore", errordetails.get("service"));
		assertNotNull(errordetails.get("stacktrace"));
	}

}
