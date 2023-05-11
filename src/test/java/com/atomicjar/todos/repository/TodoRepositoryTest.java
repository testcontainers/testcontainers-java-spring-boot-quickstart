package com.atomicjar.todos.repository;

import com.atomicjar.todos.entity.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:15-alpine:///todos"
})
class TodoRepositoryTest {

    @Autowired
    TodoRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new Todo(null, "Todo Item 1", true, 1));
        repository.save(new Todo(null, "Todo Item 2", false, 2));
        repository.save(new Todo(null, "Todo Item 3", false, 3));
    }

    @Test
    void shouldGetPendingTodos() {
        assertThat(repository.getPendingTodos()).hasSize(2);
    }
}