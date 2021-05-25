package com.crivano.swaggerservlet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.crivano.swaggerservlet.ISwaggerPetstore.IPetPetIdGet.Response;

public interface ISwaggerPetstore {
	public static class Order implements ISwaggerModel {
		public Long id;
		public Long petId;
		public Integer quantity;
		public Date shipDate;
		public String status;
		public Boolean complete;
	}

	public static class User implements ISwaggerModel {
		public Long id;
		public String username;
		public String firstName;
		public String lastName;
		public String email;
		public String password;
		public String phone;
		public Integer userStatus;
	}

	public static class Category implements ISwaggerModel {
		public Long id;
		public String name;
	}

	public static class Tag implements ISwaggerModel {
		public Long id;
		public String name;
	}

	public static class Pet implements ISwaggerModel {
		public Long id;
		public Category category;
		public String name;
		public List<Tag> tags = new ArrayList<>();
		public String status;
	}

	public static class ApiResponse implements ISwaggerModel {
		public Integer code;
		public String type;
		public String message;
	}

	public interface IPetPetIdGet extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public Long petId;
		}

		public static class Response implements ISwaggerResponse {
			public String color;
		}

		public void run(Request req, Response resp, SwaggerPetstoreContext ctx) throws Exception;
	}

	public interface IPetPetIdPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public Long petId;
			public String name;
			public String status;
		}

		public void run(Request req, Response resp, SwaggerPetstoreContext ctx) throws Exception;
	}

	public interface IUserLoginGet extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String username;
			public String password;
		}

		public static class Response implements ISwaggerResponse {
		}

		public void run(Request req, Response resp, SwaggerPetstoreContext ctx) throws Exception;
	}

}
