# Testcontainers SpringBoot Quickstart
This quick starter will guide you to configure and use Testcontainers in a SpringBoot project.

In this guide, we'll look at a sample Spring Boot application that uses Testcontainers for running unit tests with real dependencies.
The initial implementation uses a relational database for storing data.
We'll look at the necessary parts of the code that integrates Testcontainers into the app.
Then we'll switch the relation database for MongoDB, and guide you through using Testcontainers for testing the app against a real instance of MongoDB running in a container.

After the quick start, you'll have a working Spring Boot app with Testcontainers-based tests, and will be ready to explore integrations with other databases and other technologies via Testcontainers.

## 1. Setup Environment
Make sure you have Java 8+ and a [compatible Docker environment](https://www.testcontainers.org/supported_docker_environment/) installed.

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
* Clone the repository `git clone https://github.com/testcontainers/quickstarts.git && cd quickstarts/java-spring-boot-quickstart`
* Open the **java-spring-boot-quickstart** project in your favorite IDE.

## 3. Run Tests
Run the Gradle `test` command to run the tests. The sample project uses JUnit tests and Testcontainers to run them against actual databases running in containers.

```shell
$ java-spring-boot-quickstart> ./gradlew test 
```

The tests should pass.

## 4. Let's explore the code
The **java-spring-boot-quickstart** project is a SpringBoot REST API using Java 17, Spring Data JPA, PostgreSQL, and Gradle.
We are using [JUnit 5](https://junit.org/junit5/), [Testcontainers](https://testcontainers.org) and [RestAssured](https://rest-assured.io/) for testing.

### 4.1. Test Dependencies
Following are the Testcontainers and RestAssured dependencies:

**build.gradle**
```groovy 
ext {
    set('testcontainersVersion', "1.17.5")
}

dependencies {
    ...
    ...
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

### 4.2. How to use Testcontainers?
Testcontainers library can be used to spin up desired services as docker containers and run tests against those services.
We can use our testing library lifecycle hooks to start/stop containers using Testcontainers API.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TodoControllerTests {
    @LocalServerPort
    private Integer port;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");

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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");

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
    "spring.datasource.url=jdbc:tc:postgresql:14-alpine:///todos"
})
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

By setting the datasource url to `jdbc:tc:postgresql:14-alpine:///todos` (notice the special `:tc` prefix),
Testcontainers automatically spin up the Postgres database using `postgresql:14-alpine` docker image.

For more information on Testcontainers JDBC Support refer https://www.testcontainers.org/modules/databases/jdbc/

## 5. Switch to MongoDB
Let's explore how Testcontainers allow using other technologies in your unit tests.
In this chapter, we'll switch the application to use MongoDB as its data store, and will adapt the tests accordingly.

The application has several tests in the `TodoControllerTests` class for testing various API endpoints.
These high-level tests enable the developers to enhance or refactor the code without breaking the API contracts.

Let us see how we can switch to MongoDB and use Testcontainers `MongoDBContainer` to ensure API endpoints are not broken and are working as expected.

### 5.1. Switch to MongoDB and Spring Data Mongo
Following are the changes to use MongoDB instead of Postgres.

#### 5.1.1. Update dependencies in `build.gradle`
* Remove `spring-boot-starter-data-jpa`, `flyway-core`, `postgresql`, `org.testcontainers:postgresql` dependencies.
* Add the following dependencies:

    ```groovy
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
        testImplementation 'org.testcontainers:mongodb'
    }
    ```

#### 5.1.2. Delete flyway migrations
Delete flyway migrations under `src/main/resources/db/migration` folder.

#### 5.1.3. Update `Todo.java`
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
#### 5.1.4. Update `TodoControllerTests.java`
Update `TodoControllerTests.java` to use `MongoDBContainer` instead of `PostgreSQLContainer`.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoControllerTest {
  static MongoDBContainer mongodb = new MongoDBContainer("mongo:4.0.10");

  @BeforeAll
  static void beforeAll() {
    mongodb.start();
  }

  @AfterAll
  static void afterAll() {
    mongodb.stop();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
  }
  // tests
}
```

#### 5.1.5. Update `ApplicationTests.java`
Update `ApplicationTests.java` to run MongoDB container using JUnit5 Extension.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationTests {
  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongo:4.0.10");

  @Test
  void contextLoads() {
  }

}
```

We have made all the changes to migrate from Postgres to MongoDB. Let us verify it by running tests.

```shell
$ ./gradlew test
```

All tests should PASS.

## Conclusion
Testcontainers enable using the real dependency services like SQL databases, NoSQL datastores, message brokers
or any containerized services for that matter.  
This approach allows you to create reliable test suites improving confidence in your code.
