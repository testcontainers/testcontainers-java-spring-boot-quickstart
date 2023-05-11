package com.atomicjar.todos.repository;

import com.atomicjar.todos.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, String> {
    @Query("select t from Todo t where t.completed = false")
    List<Todo> getPendingTodos();
}
