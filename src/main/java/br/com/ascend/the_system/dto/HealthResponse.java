package br.com.ascend.the_system.dto;

import java.time.OffsetDateTime;

public record HealthResponse(
        String status,
        String application,
        OffsetDateTime timestamp
) {
}
