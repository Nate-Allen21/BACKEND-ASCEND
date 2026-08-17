package br.com.ascend.the_system.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
