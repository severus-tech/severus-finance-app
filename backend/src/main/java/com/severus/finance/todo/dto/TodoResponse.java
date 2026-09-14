package com.severus.finance.todo.dto;

import com.severus.finance.todo.enums.TodoPriority;
import com.severus.finance.todo.enums.TodoStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TodoResponse(
    UUID id,
    String title,
    String description,
    LocalDate dueDate,
    TodoPriority priority,
    TodoStatus status,
    Instant completedAt,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
