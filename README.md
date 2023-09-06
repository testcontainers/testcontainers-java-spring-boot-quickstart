# Testcontainers SpringBoot Quickstart
This quick starter will guide you to configure and use Testcontainers in a SpringBoot project.

In this guide, we'll look at a sample Spring Boot application that uses Testcontainers for running unit tests with real dependencies.
The initial implementation uses a relational database for storing data.
We'll look at the necessary parts of the code that integrates Testcontainers into the app.
Then we'll switch the relation database for MongoDB, and guide you through using Testcontainers for testing the app against a real instance of MongoDB running in a container.

After the quick start, you'll have a working Spring Boot app with Testcontainers-based tests, and will be ready to explore integrations with other databases and other technologies via Testcontainers.

## 1. Setup Environment
Make sure you have Java 8+ and a [compatible Docker environment](https://www.testcontainers.org/supported_docker_environment/) installed.
If you are going to use Maven build tool then make sure Java 17+ is installed.

For example:
```shell
$ java -version
openjdk version "17.0.4" 2022-07-19
OpenJDK Runtime Environment Temurin-17.0.4+8 (build 17.0.4+8)
OpenJDK 64-Bit Server VM Temurin-17.0.4+8 (build 17.0.4+8, mixed mode, sharing)

$ docker version
... 
Server: Docker Desktop 4.12.0 (85629)
 Engine:
  Version:          20.10.17
  API version:      1.41 (minimum version 1.12)
  Go version:       go1.17.11
...
```
## 2. Setup Project
* Clone the repository `git clone https://github.com/testcontainers/testcontainers-java-spring-boot-quickstart.git && cd testcontainers-java-spring-boot-quickstart`
* Open the **testcontainers-java-spring-boot-quickstart** project in your favorite IDE.

## 3. Run Tests
The sample project uses JUnit tests and Testcontainers to run them against actual databases running in containers.

Run the command to run the tests.
```shell
$ ./gradlew test //for Gradle
$ ./mvnw verify  //for Maven
```

The tests should pass.

## 4. Let's explore the code
The **testcontainers-java-spring-boot-quickstart** project is a SpringBoot REST API using Java 17, Spring Data JPA, PostgreSQL, and Gradle/Maven.
We are using [JUnit 5](https://junit.org/junit5/), [Testcontainers](https://testcontainers.org) and [RestAssured](https://rest-assured.io/) for testing.

### 4.1. Test Dependencies
Following are the Testcontainers and RestAssured dependencies:

**build.gradle**
```groovy 
ext {
    set('testcontainersVersion', "1.19.0")
}

dependencies {
    ...
    ...
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'io.rest-assured:rest-assured'
}

dependencyManagement {
    imports {
        mavenBom "org.testcontainers:testcontainers-bom:${testcontainersVersion}"
    }
}
```

For Maven build the Testcontainers and RestAssured dependencies are configured in **pom.xml** as follows:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <properties>
    ...
    ...
    <testcontainers.version>1.19.0</testcontainers.version>
  </properties>
  <dependencies>
    ...
    ...
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-testcontainers</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.rest-assured</groupId>
      <artifactId>rest-assured</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <dependencyManagement> 
    <dependencies>
      <!-- If you are using Spring Boot 3.1.0+ then you don't need to configure testcontainers-bom -->
      <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-bom</artifactId>
        <version>${testcontainers.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

### 4.2. How to use Testcontainers?
Testcontainers library can be used to spin up desired services as docker containers and run tests against those services.
We can use our testing library lifecycle hooks to start/stop containers using Testcontainers API.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TodoControllerTests {
    @LocalServerPort
    private Integer port;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TodoRepository todoRepository;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @Test
    void shouldGetAllTodos() {
        List<Todo> todos = List.of(
                new Todo(null, "Todo Item 1", false, 1),
                new Todo(null, "Todo Item 2", false, 2)
        );
        todoRepository.saveAll(todos);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/todos")
                .then()
                .statusCode(200)
                .body(".", hasSize(2));
    }
}
```

Here we have defined a `PostgreSQLContainer` instance, started the container before executing tests and stopped it after executing all the tests using JUnit 5 test lifecycle hook methods.

> **Note**
>
> If you are using any different Testing library like TestNG or Spock then you can use similar lifecycle callback methods provided by that testing library.

The Postgresql container port (5432) will be mapped to a random available port on the host.
This helps to avoid port conflicts and allows running tests in parallel.
Then we are using SpringBoot's dynamic property registration support to add/override the `datasource` properties obtained from the Postgres container.

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

In `shouldGetAllTodos()` test we are saving two Todo entities into the database using `TodoRepository` and testing `GET /todos` API endpoint to fetch todos using RestAssured.

You can run the tests directly from IDE or using the command `./gradlew test` from the terminal.

### 4.3. Using Testcontainers JUnit 5 Extension
Instead of implementing JUnit 5 lifecycle callback methods to start and stop the Postgres container,
we can use [Testcontainers JUnit 5 Extension annotations](https://www.testcontainers.org/quickstart/junit_5_quickstart/) to manage the container lifecycle as follows:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TodoControllerTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

> **Note**
>
> The Testcontainers JUnit 5 Extension will take care of starting the container before tests and stopping it after tests.
If the container is a `static` field then it will be started once before all the tests and stopped after all the tests.
If it is a non-static field then the container will be started before each test and stopped after each test.
>
> Even if you don't stop the containers explicitly, Testcontainers will take care of removing the containers, using `ryuk` container behind the scenes, once all the tests are done.
> But it is recommended to clean up the containers as soon as possible.


### 4.5. Using magical Testcontainers JDBC URL
Testcontainers provides the [**special jdbc url** support](https://www.testcontainers.org/modules/databases/jdbc/) which automatically spins up the configured database as a container.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:tc:postgresql:15-alpine:///todos"
})
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

By setting the datasource url to `jdbc:tc:postgresql:15-alpine:///todos` (notice the special `:tc` prefix),
Testcontainers automatically spin up the Postgres database using `postgresql:15-alpine` docker image.

For more information on Testcontainers JDBC Support refer https://www.testcontainers.org/modules/databases/jdbc/

### 4.6. Using Spring Boot 3.1.0 @ServiceConnection
Spring Boot 3.1.0 introduced better support for Testcontainers that simplifies test configuration greatly.
Instead of registering the postgres database connection properties using `@DynamicPropertySource`,
we can use `@ServiceConnection` to register the Database connection as follows:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TodoControllerTests {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void test() {
      ...
    }
}
```


## 5. Local Development using Testcontainers
Spring Boot 3.1.0 introduced support for using Testcontainers at development time.
You can configure your Spring Boot application to automatically start the required docker containers.

First, create a configuration class to define the required containers as follows:

```java
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    @RestartScope
    PostgreSQLContainer<?> postgreSQLContainer(){
        return new PostgreSQLContainer<>("postgres:15-alpine");
    }
}
```

Next, create a `TestApplication` class under `src/test/java` as follows:

```java
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication
                .from(Application::main)
                .with(ContainersConfig.class)
                .run(args);
    }
}
```

Now you can either run `TestApplication` from your IDE or use your build tool to start the application as follows:

```shell
$ ./gradlew bootTestRun //for Gradle
$ ./mvnw spring-boot:test-run //for Maven
```

You can access the application UI at http://localhost:8080 and enter http://localhost:8080/todos as API URL.

### 5.1 Using DevTools with Testcontainers at Development Time
During development, you can use Spring Boot DevTools to reload the code changes without having to completely restart the application.
You can also configure your containers to reuse the existing containers by adding `@RestartScope`.

First, Add `spring-boot-devtools` dependency.

**Gradle**

```groovy
testImplementation 'org.springframework.boot:spring-boot-devtools'
```

**Maven**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Next, add `@RestartScope` annotation on container bean definition as follows:

```java
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    @RestartScope
    PostgreSQLContainer<?> postgreSQLContainer(){
        return new PostgreSQLContainer<>("postgres:15-alpine");
    }

}
```

Now when devtools reloads your application, the same containers will be reused instead of re-creating them.

## 6. Switch to MongoDB
Let's explore how Testcontainers allow using other technologies in your unit tests.
In this chapter, we'll switch the application to use MongoDB as its data store, and will adapt the tests accordingly.

The application has several tests in the `TodoControllerTests` class for testing various API endpoints.
These high-level tests enable the developers to enhance or refactor the code without breaking the API contracts.

Let us see how we can switch to MongoDB and use Testcontainers `MongoDBContainer` to ensure API endpoints are not broken and are working as expected.

### 6.1. Switch to MongoDB and Spring Data Mongo
Following are the changes to use MongoDB instead of Postgres.

#### 6.1.1. Update dependencies in `build.gradle`
* Remove `spring-boot-starter-data-jpa`, `flyway-core`, `postgresql`, `org.testcontainers:postgresql` dependencies.
* Add the following dependencies:
    
    * If you are using Gradle
    ```groovy
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
        testImplementation 'org.testcontainers:mongodb'
    }
    ```

  * If you are using Maven
    ```xml
    <dependencies>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-data-mongodb</artifactId>
      </dependency>
      <dependency>
          <groupId>org.testcontainers</groupId>
          <artifactId>mongodb</artifactId>
          <scope>test</scope>
      </dependency>
    </dependencies>
    ```
#### 6.1.2. Delete flyway migrations
Delete flyway migrations under `src/main/resources/db/migration` folder.

#### 6.1.3. Update `Todo.java`
Update `Todo.java` which is currently a JPA entity to represent a Mongo Document using Spring Data Mongo as follows:

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "todos")
public class Todo {
  @Id
  private String id;
  private String title;
  private Boolean completed;
  private Integer order;
  //setter & getters
   ...
}
```
#### 6.1.4. Update `TodoControllerTests.java`
Update `TodoControllerTests.java` to use `MongoDBContainer` instead of `PostgreSQLContainer`.

```java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TodoControllerTest {

  @Container
  @ServiceConnection
  static MongoDBContainer mongodb = new MongoDBContainer("mongo:6.0.5");

  // tests
}
```

#### 6.1.5. Update `ApplicationTests.java`
Update `ApplicationTests.java` to run MongoDB container using JUnit5 Extension.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationTests {
  @Container
  @ServiceConnection
  static MongoDBContainer mongodb = new MongoDBContainer("mongo:6.0.5");

  @Test
  void contextLoads() {
  }

}
```

We have made all the changes to migrate from Postgres to MongoDB. Let us verify it by running tests.

```shell
$ ./gradlew test
$ ./mvnw verify
```

All tests should PASS.

## Conclusion
Testcontainers enable using the real dependency services like SQL databases, NoSQL datastores, message brokers
or any containerized services for that matter. This approach allows you to create reliable test suites improving confidence in your code.
