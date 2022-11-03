package com.atomicjar.todos.entity;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.persistence.*;

@Entity
@Table(name = "todos")
public class Todo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "order_number")
    private Integer order;

    public Todo() {
    }

    public Todo(Long id, String title, Boolean completed, Integer order) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + "/todos/" + this.getId();
    }
}
