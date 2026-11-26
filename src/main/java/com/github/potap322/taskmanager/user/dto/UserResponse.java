package com.github.potap322.taskmanager.user.dto;

public record UserResponse(
        Long id,
        String username,
        String email
) {
}
