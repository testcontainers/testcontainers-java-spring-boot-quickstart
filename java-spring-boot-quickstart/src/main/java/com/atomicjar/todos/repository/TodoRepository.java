package com.atomicjar.todos.repository;

import com.atomicjar.todos.entity.Todo;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TodoRepository extends PagingAndSortingRepository<Todo, String> {
}
