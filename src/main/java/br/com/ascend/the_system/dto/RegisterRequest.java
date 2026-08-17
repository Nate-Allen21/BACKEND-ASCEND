package br.com.ascend.the_system.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {
}
