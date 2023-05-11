package com.atomicjar.todos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:tc:postgresql:15-alpine:///todos"
})
class ApplicationTests {

    @Test
    void contextLoads() {
    }

}
