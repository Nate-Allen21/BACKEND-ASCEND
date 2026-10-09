package br.com.ascend.the_system.dto;

import java.util.List;

public record AiChatRequest(
        String message,
        List<AiChatMessage> conversation,
        RoutineProfileRequest profile,
        Boolean externalConsent
) {
}
