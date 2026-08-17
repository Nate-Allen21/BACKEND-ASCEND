package br.com.ascend.the_system.dto;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        String message,
        String path,
        OffsetDateTime timestamp
) {
}
