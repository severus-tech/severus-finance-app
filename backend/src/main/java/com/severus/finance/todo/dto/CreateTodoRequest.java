package com.severus.finance.todo.dto;

import com.severus.finance.todo.enums.TodoPriority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateTodoRequest(
    @NotBlank(message = "Title is required")
    String title,

    String description,
    LocalDate dueDate,
    TodoPriority priority
) {}
