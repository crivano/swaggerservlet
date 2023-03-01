package com.crivano.swaggerservlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.crivano.swaggerservlet.dependency.IDependency;
import com.crivano.swaggerservlet.dependency.TestableDependency;
import com.crivano.swaggerservlet.test.Test;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class SwaggerTestTest extends TestCase {
	static final TestableDependency depOK = new TestableDependency("category", "depOK", false, 0, 1000) {

		@Override
		public String getUrl() {
			return "test-ok";
		}

		@Override
		public boolean test() throws Exception {
			return true;
		}
	};

	static final TestableDependency depOKP = new TestableDependency("category", "depOKP", false, 0, 1000) {

		@Override
		public String getUrl() {
			return "test-okp";
		}

		@Override
		public boolean test() throws Exception {
			return true;
		}
	};

	static final TestableDependency depNOK = new TestableDependency("category", "depNOK", false, 0, 1000) {

		@Override
		public String getUrl() {
			return "test-nok";
		}

		@Override
		public boolean test() throws Exception {
			return false;
		}
	};

	static final TestableDependency depNOKP = new TestableDependency("category", "depNOKP", false, 0, 1000) {

		@Override
		public String getUrl() {
			return "test-nokp";
		}

		@Override
		public boolean test() throws Exception {
			return false;
		}
	};

	private SwaggerServlet ss = null;
	private Swagger sv = null;
	private Map<String, IDependency> dependencies = null;

	@SuppressWarnings("serial")
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ss = new SwaggerServlet();
		ss.setAPI(ISwaggerPetstore.class);
		ss.setActionPackage("com.crivano.swaggerservlet");
		ss.setExecutor(Executors.newFixedThreadPool(3));
		dependencies = new TreeMap<>();
	}

	public void testSingleDependency_OK_Success() throws JSONException, Exception {
		dependencies.put(depOK.getService(), depOK);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertTrue(resp.getBoolean("available"));
		assertFalse(resp.getBoolean("partial"));
	}

	public void testSingleDependency_NOK_Success() throws JSONException, Exception {
		dependencies.put(depNOK.getService(), depNOK);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertFalse(resp.getBoolean("available"));
		assertFalse(resp.getBoolean("partial"));
	}

	public void testTwoDependencies_OK_NOK_Success() throws JSONException, Exception {
		dependencies.put(depNOK.getService(), depNOK);
		dependencies.put(depOK.getService(), depOK);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertFalse(resp.getBoolean("available"));
		assertFalse(resp.getBoolean("partial"));
	}

	public void testTwoDependencies_OK_OKP_Success() throws JSONException, Exception {
		dependencies.put(depOK.getService(), depOK);
		dependencies.put(depOKP.getService(), depOKP);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertTrue(resp.getBoolean("available"));
		assertFalse(resp.getBoolean("partial"));
	}

	public void testTwoDependencies_OK_NOKP_Success() throws JSONException, Exception {
		dependencies.put(depNOKP.getService(), depNOKP);
		dependencies.put(depOK.getService(), depOK);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertTrue(resp.getBoolean("available"));
		assertTrue(resp.getBoolean("partial"));
	}

	public void testTwoDependencies_OKP_NOKP_Success() throws JSONException, Exception {
		dependencies.put(depNOKP.getService(), depNOKP);
		dependencies.put(depOKP.getService(), depOKP);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertTrue(resp.getBoolean("available"));
		assertTrue(resp.getBoolean("partial"));
	}

	public void testTwoDependencies_OKP_NOK_Success() throws JSONException, Exception {
		dependencies.put(depNOK.getService(), depNOK);
		dependencies.put(depOKP.getService(), depOKP);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertFalse(resp.getBoolean("available"));
		assertFalse(resp.getBoolean("partial"));
	}

	public void testTwoDependencies_NOK_NOKP_Success() throws JSONException, Exception {
		dependencies.put(depNOK.getService(), depNOK);
		dependencies.put(depNOKP.getService(), depNOKP);
		JSONObject resp = testFor(dependencies, new String[] { "false" }, new String[] { "depOKP:true", "depNOKP:true" });
		assertFalse(resp.getBoolean("available"));
		assertFalse(resp.getBoolean("partial"));
	}

	private JSONObject testFor(Map<String, IDependency> dependencies, String[] skip, String[] partial)
			throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn("/api/v1/test");
		when(request.getParameterValues("skip")).thenReturn(skip);
		when(request.getParameterValues("partial")).thenReturn(partial);
		List<Property> properties = new ArrayList<>();

		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.getWriter()).thenReturn(pw);

		Test.run(ss, dependencies, properties, request, response);
		System.out.println(sw.toString());

		return new JSONObject(sw.toString());
	}
}
