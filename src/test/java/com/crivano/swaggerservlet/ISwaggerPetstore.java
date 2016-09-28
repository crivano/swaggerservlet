package com.crivano.swaggerservlet;

import java.util.List;

interface ISwaggerPetstore {
	class Order {
		Long id;
		Long petId;
		Long quantity;
		String shipDate;
		String status;
		Boolean complete;
	}

	class User {
		Long id;
		String username;
		String firstName;
		String lastName;
		String email;
		String password;
		String phone;
		Long userStatus;
	}

	class Category {
		Long id;
		String name;
	}

	class Tag {
		Long id;
		String name;
	}

	class Pet {
		Long id;
		Category category;
		String name;
		List<String> photoUrls;
		List<Tag> tags;
		String status;
	}

	class ApiResponse {
		Long code;
		String type;
		String message;
	}

	class PetPostRequest implements ISwaggerRequest {
		String body;
	}

	class PetPostResponse implements ISwaggerResponse {
	}

	interface IPetPost extends ISwaggerMethod {
		void run(PetPostRequest req, PetPostResponse resp) throws Exception;
	}

	class PetPutRequest implements ISwaggerRequest {
		String body;
	}

	class PetPutResponse implements ISwaggerResponse {
	}

	interface IPetPut extends ISwaggerMethod {
		void run(PetPutRequest req, PetPutResponse resp) throws Exception;
	}

	class PetFindByStatusGetRequest implements ISwaggerRequest {
		List<String> status;
	}

	class PetFindByStatusGetResponse implements ISwaggerResponse {
	}

	interface IPetFindByStatusGet extends ISwaggerMethod {
		void run(PetFindByStatusGetRequest req, PetFindByStatusGetResponse resp)
				throws Exception;
	}

	class PetFindByTagsGetRequest implements ISwaggerRequest {
		List<String> tags;
	}

	class PetFindByTagsGetResponse implements ISwaggerResponse {
	}

	interface IPetFindByTagsGet extends ISwaggerMethod {
		void run(PetFindByTagsGetRequest req, PetFindByTagsGetResponse resp)
				throws Exception;
	}

	class PetPetIdGetRequest implements ISwaggerRequest {
		Long petId;
	}

	class PetPetIdGetResponse implements ISwaggerResponse {

		public String color;
	}

	interface IPetPetIdGet extends ISwaggerMethod {
		void run(PetPetIdGetRequest req, PetPetIdGetResponse resp)
				throws Exception;
	}

	class PetPetIdPostRequest implements ISwaggerRequest {
		Long petId;
		String name;
		String status;
	}

	class PetPetIdPostResponse implements ISwaggerResponse {
	}

	interface IPetPetIdPost extends ISwaggerMethod {
		void run(PetPetIdPostRequest req, PetPetIdPostResponse resp)
				throws Exception;
	}

	class PetPetIdDeleteRequest implements ISwaggerRequest {
		String api_key;
		Long petId;
	}

	class PetPetIdDeleteResponse implements ISwaggerResponse {
	}

	interface IPetPetIdDelete extends ISwaggerMethod {
		void run(PetPetIdDeleteRequest req, PetPetIdDeleteResponse resp)
				throws Exception;
	}

	class PetPetIdUploadImagePostRequest implements ISwaggerRequest {
		Long petId;
		String additionalMetadata;
		String file;
	}

	class PetPetIdUploadImagePostResponse implements ISwaggerResponse {
	}

	interface IPetPetIdUploadImagePost extends ISwaggerMethod {
		void run(PetPetIdUploadImagePostRequest req,
				PetPetIdUploadImagePostResponse resp) throws Exception;
	}

	class StoreInventoryGetRequest implements ISwaggerRequest {
	}

	class StoreInventoryGetResponse implements ISwaggerResponse {
	}

	interface IStoreInventoryGet extends ISwaggerMethod {
		void run(StoreInventoryGetRequest req, StoreInventoryGetResponse resp)
				throws Exception;
	}

	class StoreOrderPostRequest implements ISwaggerRequest {
		String body;
	}

	class StoreOrderPostResponse implements ISwaggerResponse {
	}

	interface IStoreOrderPost extends ISwaggerMethod {
		void run(StoreOrderPostRequest req, StoreOrderPostResponse resp)
				throws Exception;
	}

	class StoreOrderOrderIdGetRequest implements ISwaggerRequest {
		Long orderId;
	}

	class StoreOrderOrderIdGetResponse implements ISwaggerResponse {
	}

	interface IStoreOrderOrderIdGet extends ISwaggerMethod {
		void run(StoreOrderOrderIdGetRequest req,
				StoreOrderOrderIdGetResponse resp) throws Exception;
	}

	class StoreOrderOrderIdDeleteRequest implements ISwaggerRequest {
		Long orderId;
	}

	class StoreOrderOrderIdDeleteResponse implements ISwaggerResponse {
	}

	interface IStoreOrderOrderIdDelete extends ISwaggerMethod {
		void run(StoreOrderOrderIdDeleteRequest req,
				StoreOrderOrderIdDeleteResponse resp) throws Exception;
	}

	class UserPostRequest implements ISwaggerRequest {
		String body;
	}

	class UserPostResponse implements ISwaggerResponse {
	}

	interface IUserPost extends ISwaggerMethod {
		void run(UserPostRequest req, UserPostResponse resp) throws Exception;
	}

	class UserCreateWithArrayPostRequest implements ISwaggerRequest {
		String body;
	}

	class UserCreateWithArrayPostResponse implements ISwaggerResponse {
	}

	interface IUserCreateWithArrayPost extends ISwaggerMethod {
		void run(UserCreateWithArrayPostRequest req,
				UserCreateWithArrayPostResponse resp) throws Exception;
	}

	class UserCreateWithListPostRequest implements ISwaggerRequest {
		String body;
	}

	class UserCreateWithListPostResponse implements ISwaggerResponse {
	}

	interface IUserCreateWithListPost extends ISwaggerMethod {
		void run(UserCreateWithListPostRequest req,
				UserCreateWithListPostResponse resp) throws Exception;
	}

	class UserLoginGetRequest implements ISwaggerRequest {
		String username;
		String password;
	}

	class UserLoginGetResponse implements ISwaggerResponse {
	}

	interface IUserLoginGet extends ISwaggerMethod {
		void run(UserLoginGetRequest req, UserLoginGetResponse resp)
				throws Exception;
	}

	class UserLogoutGetRequest implements ISwaggerRequest {
	}

	class UserLogoutGetResponse implements ISwaggerResponse {
	}

	interface IUserLogoutGet extends ISwaggerMethod {
		void run(UserLogoutGetRequest req, UserLogoutGetResponse resp)
				throws Exception;
	}

	class UserUsernameGetRequest implements ISwaggerRequest {
		String username;
	}

	class UserUsernameGetResponse implements ISwaggerResponse {
	}

	interface IUserUsernameGet extends ISwaggerMethod {
		void run(UserUsernameGetRequest req, UserUsernameGetResponse resp)
				throws Exception;
	}

	class UserUsernamePutRequest implements ISwaggerRequest {
		String username;
		String body;
	}

	class UserUsernamePutResponse implements ISwaggerResponse {
	}

	interface IUserUsernamePut extends ISwaggerMethod {
		void run(UserUsernamePutRequest req, UserUsernamePutResponse resp)
				throws Exception;
	}

	class UserUsernameDeleteRequest implements ISwaggerRequest {
		String username;
	}

	class UserUsernameDeleteResponse implements ISwaggerResponse {
	}

	interface IUserUsernameDelete extends ISwaggerMethod {
		void run(UserUsernameDeleteRequest req, UserUsernameDeleteResponse resp)
				throws Exception;
	}

}
