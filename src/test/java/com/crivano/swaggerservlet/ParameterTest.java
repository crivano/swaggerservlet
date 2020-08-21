package com.crivano.swaggerservlet;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class ParameterTest extends TestCase {
	private SwaggerServlet ss = null;
	private Swagger sv = null;

	@SuppressWarnings("serial")
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ss = new SwaggerServlet();
		ss.setAPI(ISwaggerPetstore.class);
		ss.setActionPackage("com.crivano.swaggerservlet");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ss.destroy();
	}

	public ParameterTest(String testName) {
		super(testName);
	}

	public static junit.framework.Test suite() {
		return new TestSuite(ParameterTest.class);
	}

	public void testGetParameter_Default_Success() throws Exception {
		ss.addPublicProperty("foo", "bar");
		assertEquals("bar", ss.getProperty("foo"));
	}

	public void testGetParameter_Default_WithContext_Success() throws Exception {
		ss.servletContext = "context";
		ss.addPublicProperty("foo", "bar");
		assertEquals("bar", ss.getProperty("foo"));
	}

	public void testGetParameter_Default_ChangeContext_Fail() throws Exception {
		ss.servletContext = "context";
		ss.addPublicProperty("foo", "bar");
		ss.servletContext = "context2";
		try {
			ss.getProperty("foo");
			throw new Exception();
		} catch (RuntimeException ex) {
			assertEquals("Can't access property 'context2.foo' because it was not declared in servlet's initialization",
					ex.getMessage());
		}
	}

	public void testGetGlobalParameter_Default_ChangeContext_Fail() throws Exception {
		ss.servletContext = "context";
		ss.addPublicProperty("/foo", "bar");
		ss.servletContext = "context2";
		assertEquals("bar", ss.getProperty("/foo"));
	}

}
