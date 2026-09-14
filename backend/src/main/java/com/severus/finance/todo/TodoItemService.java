package com.severus.finance.todo;

import com.severus.finance.common.exception.ApiException;
import com.severus.finance.todo.dto.CreateTodoRequest;
import com.severus.finance.todo.dto.TodoResponse;
import com.severus.finance.todo.dto.UpdateTodoRequest;
import com.severus.finance.todo.enums.TodoPriority;
import com.severus.finance.todo.enums.TodoStatus;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TodoItemService {

    private final TodoItemRepository todoRepository;
    private final UserRepository userRepository;

    @Inject
    public TodoItemService(TodoItemRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<TodoResponse> getTodos(UUID userId, TodoStatus statusFilter) {
        List<TodoItem> items = statusFilter != null ?
                todoRepository.findByUserIdAndStatus(userId, statusFilter) :
                todoRepository.findByUserId(userId);
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TodoResponse createTodo(UUID userId, CreateTodoRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        TodoItem item = new TodoItem();
        item.setId(UUID.randomUUID());
        item.setUser(user);
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setDueDate(request.dueDate());
        item.setPriority(request.priority() != null ? request.priority() : TodoPriority.MEDIUM);
        item.setStatus(TodoStatus.PENDING);
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());

        todoRepository.persist(item);
        return toResponse(item);
    }

    @Transactional
    public TodoResponse updateTodo(UUID id, UUID userId, UpdateTodoRequest request) {
        TodoItem item = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "TODO_NOT_FOUND", "Todo item not found"));

        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setDueDate(request.dueDate());
        if (request.priority() != null) {
            item.setPriority(request.priority());
        }

        if (request.status() != null && request.status() != item.getStatus()) {
            item.setStatus(request.status());
            if (request.status() == TodoStatus.COMPLETED) {
                item.setCompletedAt(Instant.now());
            } else {
                item.setCompletedAt(null);
            }
        }
        item.setUpdatedAt(Instant.now());

        return toResponse(item);
    }

    @Transactional
    public TodoResponse toggleStatus(UUID id, UUID userId) {
        TodoItem item = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "TODO_NOT_FOUND", "Todo item not found"));

        if (item.getStatus() == TodoStatus.PENDING) {
            item.setStatus(TodoStatus.COMPLETED);
            item.setCompletedAt(Instant.now());
        } else {
            item.setStatus(TodoStatus.PENDING);
            item.setCompletedAt(null);
        }
        item.setUpdatedAt(Instant.now());

        return toResponse(item);
    }

    @Transactional
    public void deleteTodo(UUID id, UUID userId) {
        TodoItem item = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "TODO_NOT_FOUND", "Todo item not found"));
        todoRepository.delete(item);
    }

    public TodoResponse toResponse(TodoItem t) {
        return new TodoResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getDueDate(),
                t.getPriority(),
                t.getStatus(),
                t.getCompletedAt(),
                t.getVersion(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
