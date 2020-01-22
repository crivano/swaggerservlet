package com.crivano.swaggerservlet.dependency;

import java.util.concurrent.Callable;

import com.crivano.swaggerservlet.test.TestResult;

public abstract class TestableDependency extends DependencySupport implements Callable<TestResult> {
	public TestableDependency() {
		super();
	}

	public TestableDependency(String category, String service, boolean partial, long msMin, long msMax) {
		super(category, service, partial, msMin, msMax);
	}

	public abstract boolean test() throws Exception;

	@Override
	public TestResult call() throws Exception {
		long time = System.currentTimeMillis();
		TestResult tr = new TestResult();
		try {
			tr.ok = test();
		} catch (Exception ex) {
			tr.exception = ex;
		}
		tr.miliseconds = System.currentTimeMillis() - time;
		return tr;
	}

	@Override
	public boolean isTestable() {
		return true;
	}

}
