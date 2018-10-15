# SwaggerServlet
A very simple framework tha validates REST requests with Swagger and routes them to Pojo classes

SwaggerServlet also provides two useful features:
- A monitoring URL like ```/api/v1/test``` that tests the webservice and it's dependecies;
- A documentation URL like ```/api/v1/swagger-ui``` that opens swagger-ui showing your API;

### Downloading directly or using it through Maven

For a quick start, you can use this snippet in your maven POM:

```xml
<dependency>
    <groupId>com.crivano</groupId>
    <artifactId>swaggerservlet</artifactId>
    <version>1.15.0</version>
</dependency>
```

