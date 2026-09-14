package com.severus.finance.todo;

import com.severus.finance.todo.enums.TodoStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TodoItemRepository implements PanacheRepositoryBase<TodoItem, UUID> {

    public List<TodoItem> findByUserId(UUID userId) {
        return list("user.id = ?1 order by status asc, dueDate asc nulls last, createdAt desc", userId);
    }

    public List<TodoItem> findByUserIdAndStatus(UUID userId, TodoStatus status) {
        return list("user.id = ?1 and status = ?2 order by dueDate asc nulls last, createdAt desc", userId, status);
    }

    public Optional<TodoItem> findByIdAndUserId(UUID id, UUID userId) {
        return find("id = ?1 and user.id = ?2", id, userId).firstResultOptional();
    }

    public long countPendingByUserId(UUID userId) {
        return count("user.id = ?1 and status = 'PENDING'", userId);
    }
}
