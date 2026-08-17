package br.com.ascend.the_system.dto;

public record AuthRequest(
        String email,
        String password
) {
}
