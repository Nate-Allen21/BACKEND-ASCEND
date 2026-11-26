package br.com.ascend.the_system.dto;

public record AccountDeletionRequest(
        String password,
        String googleCredential,
        String reason,
        String confirmation,
        boolean acknowledged
) {}
