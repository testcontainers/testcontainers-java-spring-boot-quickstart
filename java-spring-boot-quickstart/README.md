# Testcontainers SpringBoot Quickstart
This quick starter will guide you to configure and use Testcontainers in a SpringBoot project.

## 1. SetUp Environment
Make sure you have Java 17 and Docker installed.

```shell
$ java -version
openjdk version "17.0.4" 2022-07-19
OpenJDK Runtime Environment Temurin-17.0.4+8 (build 17.0.4+8)
OpenJDK 64-Bit Server VM Temurin-17.0.4+8 (build 17.0.4+8, mixed mode, sharing)

$ docker version
Client:
 Cloud integration: v1.0.29
 Version:           20.10.17
 API version:       1.41
 Go version:        go1.17.11
 Git commit:        100c701
 Built:             Mon Jun  6 23:04:45 2022
 OS/Arch:           darwin/amd64
 Context:           default
 Experimental:      true

Server: Docker Desktop 4.12.0 (85629)
 Engine:
  Version:          20.10.17
  API version:      1.41 (minimum version 1.12)
  Go version:       go1.17.11
  Git commit:       a89b842
  Built:            Mon Jun  6 23:01:23 2022
  OS/Arch:          linux/amd64
  Experimental:     false
  ...
```
## 2. SetUp Project
* Clone the repository `git clone https://github.com/testcontainers/quickstarts.git`
* Open **java-spring-boot-quickstart** project in your favorite IDE

## 3. Run Tests

```shell
$ java-spring-boot-quickstart> ./gradlew test 
```

## 4. Let's explore the code
The **java-spring-boot-quickstart** project is a SpringBoot REST API using Java 17, Spring Data JPA, PostgreSQL and Gradle.
We are using JUnit 5, Testcontainers, RestAssured along with Spring Testing support for testing.

### 4.1. Test Dependencies
Following are the Testcontainers and RestAssured dependencies:

```shell
ext {
    set('testcontainersVersion', "1.17.4")
}

dependencies {
    ...
    ...
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'io.rest-assured:spring-mock-mvc'
}

dependencyManagement {
    imports {
        mavenBom "org.testcontainers:testcontainers-bom:${testcontainersVersion}"
    }
}
```

### 4.2. How to use Testcontainers?
We can use Testcontainers library to spin up desired services as docker containers and run tests against those services.
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

Here we have defined a `PostgreSQLContainer` instance, started container before executing tests and stopped after executing all the tests using JUnit 5 test lifecycle hook methods.

> **Note:** If you are using any different Testing library like TestNG or Spock, they also provide similar lifecycle callback methods.

The Postgresql container port (5432) will be mapped to a random available port on host.
Then we are using SpringBoot's dynamic property registration support to add/override the datasource properties obtained from Postgres container .

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

In `shouldGetAllTodos()` test we are saving two Todo entities into database using `TodoRepository` and testing `GET /todos` API endpoint to fetch todos using RestAssured.

You can run the tests directly from IDE or using the command `./gradlew test` from Terminal.

### 4.3. Using Testcontainers JUnit 5 Extension
Instead of implementing JUnit 5 lifecycle callback methods to start and stop Postgres container,
we can use Testcontainers JUnit 5 Extension annotations to manage the container lifecycle as follows:

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

The Testcontainers JUnit 5 Extension will take care of starting the container before tests and stopping it after tests.
If the container is a `static` field then it will be started once before all the tests and stopped after all the tests.
If it is non-static field then container will be started before each test and stopped after each test.

### 4.5. Using magical Testcontainers JDBC URL
Testcontainers provides the **special jdbc url** support which automatically spin up the configured database as a container.

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
Testcontainers automatically spin up Postgres database using `postgresql:14-alpine` docker image.

For more information on Testcontainers JDBC Support refer https://www.testcontainers.org/modules/databases/jdbc/

## 5. Switch to MongoDB
There are several tests in `TodoControllerTests` class for testing various API endpoints.
These tests enable the developers to enhance or refactor the code without breaking the API contracts.

Imagine for some reason we wanted to switch from Postgres to MongoDB.
Let us see how we can switch to MongoDB and use Testcontainers `MongoDBContainer` to ensure API endpoints are not broken and are working as expected.

### 5.1. Switch to MongoDB and Spring Data Mongo
Following are the changes to use MongoDB instead of Postgres.

#### 5.1.1. Update dependencies in `build.gradle`
* Remove `spring-boot-starter-data-jpa`, `flyway-core`, `postgresql`, `org.testcontainers:postgresql` dependencies.
* Add following dependencies
    
    ```shell
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
        testImplementation 'org.testcontainers:mongodb'
    }
    ```

#### 5.1.2. Delete flyway migrations
Delete flyway migrations under `src/main/resources/db/migration` folder.

#### 5.1.3. Update `Todo.java`
Update `Todo.java` which is currently a JPA entity to represent a Mongo Document using Spring Data Mongo as follows:
    
```shell
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

We have made all the changes to migrate from Postgres to MongoDB. Let us verify by running tests.

```shell
$ ./gradlew test
```

All tests should PASS.

## Summary
Testcontainers enable using the real dependent services like SQL databases, NoSQL datastores, message brokers
(any dockerizable services for that matter) instead of using mocks. 
This will allow the developer to create high quality TestSuite which gives more confidence in our tests.
