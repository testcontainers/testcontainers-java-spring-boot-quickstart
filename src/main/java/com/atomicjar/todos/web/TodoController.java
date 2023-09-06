package com.atomicjar.todos.web;

import com.atomicjar.todos.entity.Todo;
import com.atomicjar.todos.repository.TodoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/todos")
public class TodoController {
    private final TodoRepository repository;

    public TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Todo> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @PostMapping
    public ResponseEntity<Todo> save(@Valid @RequestBody Todo todo) {
        todo.setId(null);
        Todo savedTodo = repository.save(todo);
        String uriLocation = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + "/todos/" + savedTodo.getId();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", uriLocation)
                .body(savedTodo);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Todo> update(@PathVariable String id, @Valid @RequestBody Todo todo) {
        Todo existingTodo = repository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        if(todo.getCompleted() != null) {
            existingTodo.setCompleted(todo.getCompleted());
        }
        if(todo.getOrder() != null) {
            existingTodo.setOrder(todo.getOrder());
        }
        if(todo.getTitle() != null) {
            existingTodo.setTitle(todo.getTitle());
        }
        Todo updatedTodo = repository.save(existingTodo);
        return ResponseEntity.ok(updatedTodo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        Todo todo = repository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        repository.delete(todo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        repository.deleteAll();
        return ResponseEntity.ok().build();
    }
}
