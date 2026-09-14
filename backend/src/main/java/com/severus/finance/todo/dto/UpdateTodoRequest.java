package com.severus.finance.todo.dto;

import com.severus.finance.todo.enums.TodoPriority;
import com.severus.finance.todo.enums.TodoStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateTodoRequest(
    @NotBlank(message = "Title is required")
    String title,

    String description,
    LocalDate dueDate,
    TodoPriority priority,
    TodoStatus status
) {}
