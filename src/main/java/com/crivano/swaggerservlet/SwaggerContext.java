package com.crivano.swaggerservlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.crivano.swaggerservlet.Swagger.Path;

public class SwaggerContext {
	private String actionName;
	private ISwaggerMethod action;
	private String context;
	private String service;
	private boolean cacheable;
	private Path matchingPath;
	private Class<? extends ISwaggerRequest> clazzRequest;
	private Class<? extends ISwaggerResponse> clazzResponse;
	private Class<? extends ISwaggerApiContext> clazzContext;
	private Class<? extends ISwaggerApiContext> subclazzContext;
	private ISwaggerRequest req;
	private ISwaggerResponse resp;
	private HttpServletRequest request;
	private HttpServletResponse response;

	public SwaggerContext() {
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public ISwaggerMethod getAction() {
		return action;
	}

	public void setAction(ISwaggerMethod action) {
		this.action = action;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public Path getMatchingPath() {
		return matchingPath;
	}

	public void setMatchingPath(Path matchingPath) {
		this.matchingPath = matchingPath;
	}

	public Class<? extends ISwaggerRequest> getClazzRequest() {
		return clazzRequest;
	}

	public void setClazzRequest(Class<? extends ISwaggerRequest> clazzRequest) {
		this.clazzRequest = clazzRequest;
	}

	public Class<? extends ISwaggerResponse> getClazzResponse() {
		return clazzResponse;
	}

	public void setClazzResponse(Class<? extends ISwaggerResponse> clazzResponse) {
		this.clazzResponse = clazzResponse;
	}

	public ISwaggerRequest getReq() {
		return req;
	}

	public void setReq(ISwaggerRequest req) {
		this.req = req;
	}

	public ISwaggerResponse getResp() {
		return resp;
	}

	public void setResp(ISwaggerResponse resp) {
		this.resp = resp;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public Class<? extends ISwaggerApiContext> getClazzContext() {
		return clazzContext;
	}

	public void setClazzContext(Class<? extends ISwaggerApiContext> clazzContext) {
		this.clazzContext = clazzContext;
	}

	public Class<? extends ISwaggerApiContext> getSubclazzContext() {
		return subclazzContext;
	}

	public void setSubclazzContext(Class<? extends ISwaggerApiContext> subclazzContext) {
		this.subclazzContext = subclazzContext;
	}
}