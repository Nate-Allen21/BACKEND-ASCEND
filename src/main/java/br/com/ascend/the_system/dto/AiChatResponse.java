package br.com.ascend.the_system.dto;

import java.util.List;

public record AiChatResponse(
        String assistantName,
        String reply,
        List<String> suggestions
) {
}
