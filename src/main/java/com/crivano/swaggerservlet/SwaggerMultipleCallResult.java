package com.crivano.swaggerservlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwaggerMultipleCallResult {

	public Map<String, ISwaggerResponse> responses = new HashMap<>();
	public List<SwaggerCallStatus> status = new ArrayList<>();
}