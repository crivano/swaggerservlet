# SwaggerServlet
A very simple framework that validates REST requests with Swagger and routes them to Pojo classes

SwaggerServlet also provides useful features:
- A monitoring URL like ```/api/v1/test``` that tests the web service and its dependencies;
- A documentation URL like ```/api/v1/swagger-ui``` that opens swagger-ui showing your API;
- Declared (and validated) environment parameters.

Main objectives are:
- To be very small and simple;
- To enforce consistency by using typed data;
- To have minimum dependencies.

A very simple usage example can be seen [here](https://github.com/crivano/swaggerservlet-ui/tree/master/src/main/java/br/jus/trf2/trek/ui), and the respective swagger.yaml is [here](https://github.com/crivano/swaggerservlet-ui/tree/master/src/main/resources/br/jus/trf2/trek/ui).

A more complex, real life usage example, can be seen [here](https://github.com/assijus/assijus/blob/master/src/main/java/br/jus/trf2/assijus/AssijusServlet.java).

### Using it through Maven

For a quick start, you can use this snippet in your maven POM:

```xml
<dependency>
    <groupId>com.crivano</groupId>
    <artifactId>swaggerservlet</artifactId>
    <version>1.34.0</version>
</dependency>
```

### Architecture

SwaggerServlet requires a swagger.yaml file and a Java interface with declarations for all methods inputs and outputs. It's also necessary to declare a HttpServlet that extends SwaggerServlet class, and to have a Pojo for each web service method.

In order to reuse the web service's contract, one can create a project containing only the swagger.yaml and the Java interface, as can be seen in [this example](https://github.com/assijus/assijus-system-api).

### SwaggerServlet-UI

SwaggerServlet has an accompanying [website](http://swaggerservlet.appspot.com/) that is capable of generating a Java interface from a swagger.yaml file. For instance:

![swaggerservlet appspot com_(Desktop PC) (1)](https://user-images.githubusercontent.com/4137623/93111448-056be480-f68d-11ea-8250-a163f17f365d.png)

It is also possible to create a diagram of the dependencies using the [SwaggerServlet-UI](http://swaggerservlet.appspot.com/) tool. For example:

![swaggerservlet appspot com_(Desktop PC)](https://user-images.githubusercontent.com/4137623/93108500-3ea25580-f689-11ea-9c26-47b4422f922e.png)


### Dependencies

When a web service depends on another, it can be declared like this:
```Java
addDependency(new SwaggerServletDependency("webservice", system, false, 0, 10000) {
    @Override
    public String getUrl() {
        return "https:example.com/;
    }
});
```

SwaggerServletDependecy constructor parameters are: service's category, service's name, a boolean to indicate a partial dependency, minimum time necessary to test de dependency in milliseconds and maximum wait time to test de dependency.

A generic dependency can be declared like this:

```Java
addDependency(new TestableDependency("cache", "redis", false, 0, 10000) {
    @Override
    public String getUrl() {
        return "redis://redis.example.com";
    }
    @Override
    public boolean test() throws Exception {
        String uuid = UUID.randomUUID().toString();
        MemCacheRedis mc = new MemCacheRedis();
        mc.store("test", uuid.getBytes());
        String uuid2 = new String(mc.retrieve("test"));
        return uuid.equals(uuid2);
    }
});
```

### Properties

Every service can be customized with many parameters. Usually these parameters are defined in JBoss' standalone.xml or other App Server configuration file. The problem is that sometimes it gets difficult to track all these parameters. SwaggerServlet offers the possibility of declaring all the necessary parameters and giving default values to some. The complete list of parameters is returned by the ```/api/v1/test``` url. It is also possible to set parameters as public, restricted or private. For example:

```Java
addPublicProperty("redis.url");
addRestrictedProperty("redis.database", "10");
addPrivateProperty("redis.password", null);
```

In the above example, the first parameter tells the name of the property and the last says if it has a default value. Properties that miss the default value will cause an error if they are not present in the configuration files.

All properties are prefixed with the name of the servlet's context. For instance, if the webapp is installed at "https://assijus.trf2.jus.br/assijus", the "redis.url" property above should be declared at standalone.xml as "assijus.redis.url".

Properties not found in the configuration files are searched among environment variables. If SwaggerServlet can't find "assijus.redis.url" at the standalone.xml properties, it will try to find the environment variable PROP_ASSIJUS_REDIS_URL instead. This is very useful when instantiating a web server with docker-compose.
