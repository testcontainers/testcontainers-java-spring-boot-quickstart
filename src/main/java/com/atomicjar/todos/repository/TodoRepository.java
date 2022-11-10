package com.atomicjar.todos.repository;

import com.atomicjar.todos.entity.Todo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TodoRepository extends PagingAndSortingRepository<Todo, String> {
    @Query("select t from Todo t where t.completed is false")
    Iterable<Todo> getPendingTodos();
}
